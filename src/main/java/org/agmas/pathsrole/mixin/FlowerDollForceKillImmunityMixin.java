package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.game.GameConstants;
import io.wifi.starrailexpress.game.GameUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.content.entity.FlowerDollExplosionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameUtils.class)
public abstract class FlowerDollForceKillImmunityMixin {

    @Inject(method = "killPlayer(Lnet/minecraft/world/entity/player/Player;Z"
            + "Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/resources/ResourceLocation;Z)V",
            at = @At("HEAD"), cancellable = true)
    private static void pathsrole$blockTrainKillDuringDollInvuln(
            Player victim, boolean spawnBody, Player killer,
            ResourceLocation deathReason, boolean forceDeath, CallbackInfo ci) {
        if (victim == null || deathReason == null) {
            return;
        }
        if (deathReason.equals(GameConstants.DeathReasons.FELL_OUT_OF_TRAIN)
                && FlowerDollExplosionManager.isDollInvulnerable(victim.getUUID())) {
            ci.cancel();
        }
    }
}