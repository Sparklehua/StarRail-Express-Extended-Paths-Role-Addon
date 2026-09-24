package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.game.GameUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.noellesroles.game.roles.neutral.pelican.PelicanPlayerComponent;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PelicanPlayerComponent.class)
public abstract class PelicanPlayerComponentMixin {

    @Unique
    private boolean reimuAdjusted = false;

    @Shadow
    public int requiredEaten;

    @Shadow
    public abstract Player getPlayer();

    @Inject(method = "init", at = @At("TAIL"))
    private void resetReimuAdjusted(CallbackInfo ci) {
        reimuAdjusted = false;
    }

    @Inject(method = "tryEat", at = @At("HEAD"), cancellable = true)
    private void checkReimuTarget(ServerPlayer target, CallbackInfoReturnable<Boolean> cir) {
        if (ModRoles.REIMU == null) return;
        if (target == null) return;
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(target.level());
        if (gameWorld == null) return;
        if (gameWorld.isRole(target, ModRoles.REIMU)) {
            if (getPlayer() instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.literal("§c对方是博丽灵梦，无法吞噬。"), true);
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "checkWinCondition", at = @At("HEAD"))
    private void adjustWinCondition(CallbackInfo ci) {
        if (ModRoles.REIMU == null) return;
        if (reimuAdjusted) return;
        Player player = getPlayer();
        if (player == null) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        SREGameWorldComponent game = SREGameWorldComponent.KEY.get(level);
        if (game == null) return;
        int reimuCount = 0;
        for (ServerPlayer p : level.players()) {
            if (GameUtils.isPlayerAliveAndSurvival(p) && game.isRole(p, ModRoles.REIMU)) {
                reimuCount++;
            }
        }
        if (reimuCount > 0) {
            this.requiredEaten = Math.max(1, this.requiredEaten - reimuCount);
        }
        reimuAdjusted = true;
    }
}