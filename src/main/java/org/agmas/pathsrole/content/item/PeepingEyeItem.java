package org.agmas.pathsrole.content.item;

import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.agmas.pathsrole.init.ModRoles;

import java.util.List;

public class PeepingEyeItem extends Item {
    public PeepingEyeItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (stack == null) return;
        tooltip.add(Component.translatable("item.pathsrole.peeping_eye.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.pathsrole.peeping_eye.desc2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // 空值防护：防止服务器崩溃
        if (level == null || player == null) {
            return InteractionResultHolder.fail(ItemStack.EMPTY);
        }
        
        ItemStack stack = player.getItemInHand(hand);
        if (stack == null || stack.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        // 确保服务端玩家有效
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        // Server-side: raycast to find a shielded player the user is looking at
        Vec3 eyePos = serverPlayer.getEyePosition();
        Vec3 lookVec = serverPlayer.getLookAngle();
        if (eyePos == null || lookVec == null) {
            return InteractionResultHolder.fail(stack);
        }
        Vec3 endPos = eyePos.add(lookVec.scale(256));

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(level);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level,
                serverPlayer,
                eyePos,
                endPos,
                AABB.ofSize(eyePos, 256, 256, 256).inflate(256),
                entity -> entity instanceof ServerPlayer target &&
                        target != serverPlayer &&
                        target.isAlive() &&
                        target.level() == level &&
                        (gameWorld == null || !gameWorld.isRole(target, ModRoles.REIMU)) &&
                        SREArmorPlayerComponent.KEY.get(target).getArmor() > 0,
                256 * 256
        );

        if (entityHit != null && entityHit.getEntity() instanceof ServerPlayer target) {
            // 额外空值防护：确保目标存活
            if (!target.isAlive()) {
                return InteractionResultHolder.fail(stack);
            }
            
            // Check line of sight — no obstructing blocks allowed
            var blockHit = level.clip(
                    new net.minecraft.world.level.ClipContext(
                            eyePos,
                            target.getEyePosition(),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                            serverPlayer
                    )
            );

            if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.pathsrole.peeping_eye.obstructed")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }

            // Remove the target's shield — 空值防护
            SREArmorPlayerComponent armorComp = SREArmorPlayerComponent.KEY.get(target);
            if (armorComp != null) {
                armorComp.removeArmor(armorComp.getArmor());
            }

            // Consume one item — 空值防护
            if (!stack.isEmpty()) {
                stack.consume(1, serverPlayer);
            }

            // Glass break sound
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);

            // Feedback
            serverPlayer.displayClientMessage(
                    Component.translatable("message.pathsrole.peeping_eye.success", target.getDisplayName())
                            .withStyle(ChatFormatting.GREEN),
                    true
            );

            return InteractionResultHolder.success(stack);
        }

        // No shielded target found
        serverPlayer.displayClientMessage(
                Component.translatable("message.pathsrole.peeping_eye.no_target")
                        .withStyle(ChatFormatting.RED),
                true
        );

        return InteractionResultHolder.fail(stack);
    }
}