package org.agmas.pathsrole.content.item;

import java.util.List;
import java.util.UUID;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.game.GameUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterPlayerComponent;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;

public class WantedPosterItem extends Item {

    public static final String POSTER_STATE = "posterState";
    public static final String SUSPENDER_UUID = "suspenderUUID";
    public static final String SUSPENDER_NAME = "suspenderName";
    public static final String TARGET_UUID = "targetUUID";
    public static final String TARGET_NAME = "targetName";

    public static Runnable openGuiRunner = null;

    public enum PosterState {
        UNASSIGNED,
        ASSIGNED,
        ACTIVE
    }

    public WantedPosterItem(Properties settings) {
        super(settings);
    }

    public static PosterState getState(ItemStack stack) {
        return PosterState.valueOf(getTag(stack).getString(POSTER_STATE));
    }

    public static void setState(ItemStack stack, PosterState state) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putString(POSTER_STATE, state.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        if (!tag.contains(POSTER_STATE)) {
            tag.putString(POSTER_STATE, PosterState.UNASSIGNED.name());
        }
        return tag;
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        return getTag(stack);
    }

    public static void setSuspender(ItemStack stack, String uuid, String name) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putString(SUSPENDER_UUID, uuid);
        tag.putString(SUSPENDER_NAME, name);
        tag.putString(POSTER_STATE, PosterState.ASSIGNED.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void setTarget(ItemStack stack, String uuid, String name) {
        CompoundTag tag = getOrCreateTag(stack);
        tag.putString(TARGET_UUID, uuid);
        tag.putString(TARGET_NAME, name);
        tag.putString(POSTER_STATE, PosterState.ACTIVE.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String getSuspenderUUID(ItemStack stack) {
        return getTag(stack).getString(SUSPENDER_UUID);
    }

    public static String getSuspenderName(ItemStack stack) {
        return getTag(stack).getString(SUSPENDER_NAME);
    }

    public static String getTargetUUID(ItemStack stack) {
        return getTag(stack).getString(TARGET_UUID);
    }

    public static String getTargetName(ItemStack stack) {
        return getTag(stack).getString(TARGET_NAME);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(world);
        boolean isHunter = gameWorld != null && gameWorld.isRole(user, ModRoles.BOUNTY_HUNTER);
        PosterState state = getState(stack);

        if (state == PosterState.UNASSIGNED) {
            if (isHunter) {
                return InteractionResultHolder.pass(stack);
            }
            if (gameWorld != null && !gameWorld.isKillerTeam(user)) {
                user.displayClientMessage(
                    Component.translatable("message.pathsrole.bounty_hunter.not_killer_cannot_use").withStyle(ChatFormatting.RED),
                    true);
                return InteractionResultHolder.fail(stack);
            }
            if (world.isClientSide()) {
                if (openGuiRunner != null) {
                    openGuiRunner.run();
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        }

        if (state == PosterState.ACTIVE) {
            if (!isHunter) {
                return InteractionResultHolder.pass(stack);
            }
            if (world.isClientSide()) {
                return InteractionResultHolder.success(stack);
            }

            BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(user);
            if (comp == null) {
                return InteractionResultHolder.fail(stack);
            }
            if (comp.getTargetUUID() != null) {
                user.displayClientMessage(
                    Component.translatable("message.pathsrole.bounty_hunter.already_hunting").withStyle(ChatFormatting.RED),
                    true);
                return InteractionResultHolder.fail(stack);
            }
            if (comp.getCooldownTicks() > 0) {
                user.displayClientMessage(
                    Component.translatable("message.pathsrole.bounty_hunter.cooldown", comp.getCooldownTicks() / 20)
                        .withStyle(ChatFormatting.RED),
                    true);
                return InteractionResultHolder.fail(stack);
            }

            String targetUUIDStr = getTargetUUID(stack);
            String targetName = getTargetName(stack);
            if (targetUUIDStr.isEmpty()) {
                return InteractionResultHolder.fail(stack);
            }

            UUID targetUUID;
            try {
                targetUUID = UUID.fromString(targetUUIDStr);
            } catch (IllegalArgumentException e) {
                return InteractionResultHolder.fail(stack);
            }
            Player target = world.getPlayerByUUID(targetUUID);
            if (target == null || !GameUtils.isPlayerAliveAndSurvival(target)) {
                user.displayClientMessage(
                    Component.translatable("message.pathsrole.bounty_hunter.target_dead").withStyle(ChatFormatting.RED),
                    true);
                return InteractionResultHolder.fail(stack);
            }

            comp.setTarget(targetUUID, targetName);
            user.displayClientMessage(
                Component.translatable("message.pathsrole.bounty_hunter.hunt_started", targetName).withStyle(ChatFormatting.GOLD),
                true);
            target.displayClientMessage(
                Component.translatable("message.pathsrole.bounty_hunter.being_hunted").withStyle(ChatFormatting.RED),
                true);
            ItemStack targetRevolver = ModItems.TARGET_REVOLVER.getDefaultInstance();
            TargetRevolverItem.setHunterUUID(targetRevolver, user.getUUID());
            target.addItem(targetRevolver);
            target.displayClientMessage(
                Component.translatable("message.pathsrole.bounty_hunter.received_revolver").withStyle(ChatFormatting.GOLD),
                true);
            stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        PosterState state = getState(stack);
        switch (state) {
            case UNASSIGNED:
                tooltip.add(Component.translatable("item.pathsrole.wanted_poster.unassigned").withStyle(ChatFormatting.GRAY));
                break;
            case ASSIGNED:
                tooltip.add(Component.translatable("item.pathsrole.wanted_poster.assigned", getSuspenderName(stack)).withStyle(ChatFormatting.YELLOW));
                break;
            case ACTIVE:
                tooltip.add(Component.translatable("item.pathsrole.wanted_poster.active", getTargetName(stack)).withStyle(ChatFormatting.RED));
                break;
        }
    }
}