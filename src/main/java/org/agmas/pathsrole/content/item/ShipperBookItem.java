package org.agmas.pathsrole.content.item;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import org.agmas.pathsrole.client.screen.ShipperBookScreen;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;

public class ShipperBookItem
extends Item {
    public ShipperBookItem(Item.Properties settings) {
        super(settings);
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (world.isClientSide()) {
            handleClientUse(stack, user);
        }
        return InteractionResultHolder.pass(stack);
    }

    private void storePlayerName(ItemStack stack, String playerName) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        String playerA = tag.getString("PlayerA");
        String playerB = tag.getString("PlayerB");
        if (playerA.isEmpty()) {
            tag.putString("PlayerA", playerName);
        } else if (playerB.isEmpty()) {
            if (playerA.equals(playerName)) {
                return;
            }
            tag.putString("PlayerB", playerName);
        } else {
            tag.putString("PlayerA", playerName);
            tag.remove("PlayerB");
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private void clearPlayerData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.remove("PlayerA");
        tag.remove("PlayerB");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.pathsrole.shipper_book");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        if (!Screen.hasShiftDown()) {
            tooltipComponents.add(Component.literal("按住 [Shift] 查看详情")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        
        tooltipComponents.add(Component.literal(""));
        tooltipComponents.add(Component.translatable("text.pathsrole.shipper_book.intro")
            .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal(""));
        tooltipComponents.add(Component.translatable("message.pathsrole.shipper.book_hint")
            .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }

    @Environment(value=EnvType.CLIENT)
    private void handleClientUse(ItemStack stack, Player user) {
            EntityHitResult entityHit;
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult instanceof EntityHitResult && (entityHit = (EntityHitResult)hitResult).getEntity() instanceof Player targetPlayer) {
                if (user.isShiftKeyDown()) {
                    clearPlayerData(stack);
                    user.displayClientMessage(Component.translatable("message.pathsrole.shipper_book.cleared"), true);
                    return;
                }
                if (targetPlayer == user) {
                    return;
                }
                storePlayerName(stack, targetPlayer.getName().getString());
                return;
            }
            this.openBookScreen(stack);
    }

    @Environment(value=EnvType.CLIENT)
    private void openBookScreen(ItemStack stack) {
        ShipperPlayerComponent shipperComp = ShipperPlayerComponent.KEY.get(Minecraft.getInstance().player);
        if (shipperComp != null && shipperComp.isRageActive()) {
            return;
        }
        Minecraft.getInstance().setScreen((Screen)new ShipperBookScreen());
    }
}