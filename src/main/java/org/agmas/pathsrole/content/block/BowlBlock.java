package org.agmas.pathsrole.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.game.GameUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionPlayerComponent;
import org.agmas.pathsrole.init.ModRoles;

public class BowlBlock extends Block {

    public BowlBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(sp.serverLevel());
        if (gameWorld == null || !gameWorld.isRunning() || (!gameWorld.isRole(sp, ModRoles.SHION) && !sp.isCreative())) {
            return InteractionResult.SUCCESS;
        }

        ShionPlayerComponent comp = PathsroleComponents.getShionComponent(sp);
        if (comp == null) return InteractionResult.PASS;

        if (comp.isSitting()) {
            return InteractionResult.SUCCESS;
        }

        if (comp.getBowlCooldown() > 0) {
            int remaining = comp.getBowlCooldown() / 20;
            sp.displayClientMessage(
                    Component.translatable("message.pathsrole.shion.bowl_cooldown", remaining)
                            .withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.SUCCESS;
        }

        // Abnormal state, remove bowl
        level.removeBlock(pos, false);
        comp.setPlacedBowlPos(null);
        comp.setBowlCooldown(200);
        sp.displayClientMessage(
                Component.translatable("message.pathsrole.shion.bowl_not_yours")
                        .withStyle(ChatFormatting.RED),
                true);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean moved) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            for (Player p : level.players()) {
                ShionPlayerComponent comp = PathsroleComponents.getShionComponent(p);
                if (comp != null && comp.isSitting()
                        && comp.getPlacedBowlPos() != null
                        && comp.getPlacedBowlPos().equals(pos)) {
                    comp.stopBowlSitting();
                    p.removeEffect(MobEffects.GLOWING);
                    if (p instanceof ServerPlayer sp) {
                        sp.displayClientMessage(
                                Component.translatable("message.pathsrole.shion.bowl_broken")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}