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
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.pathsrole.init.ModRoles;

import java.util.List;
import java.util.Optional;

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

        // 手动射线检测，绕过 ProjectileUtilMixin 对最后参数的误用
        ServerPlayer closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (ServerPlayer candidate : serverPlayer.getServer().getPlayerList().getPlayers()) {
            if (candidate == serverPlayer) continue;
            if (!candidate.isAlive()) continue;
            if (candidate.level() != level) continue;
            if (gameWorld != null && gameWorld.isRole(candidate, ModRoles.REIMU)) continue;
            SREArmorPlayerComponent candidateArmor = SREArmorPlayerComponent.KEY.get(candidate);
            if (candidateArmor == null || candidateArmor.getArmor() <= 0) continue;

            AABB hitbox = candidate.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = hitbox.clip(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = candidate;
                }
            }
        }

        if (closestTarget != null) {
            ServerPlayer target = closestTarget;
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