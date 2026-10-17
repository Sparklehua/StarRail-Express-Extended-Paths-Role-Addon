package org.agmas.pathsrole.mixin;

import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigFormHelper;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PigEntityMixin {

    private static final EntityDimensions PIG_DIMENSIONS = EntityDimensions.scalable(0.9F, 0.9F)
            .withEyeHeight(0.4F);

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    public void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        if (self.getPose() == Pose.SWIMMING) return;
        if (!(self instanceof Player)) return;
        if (self.level().isClientSide()) {
            PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(self);
            if (comp != null && comp.isInPigForm()) {
                cir.setReturnValue(PIG_DIMENSIONS);
            }
            return;
        }
        PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(self);
        if (comp != null && comp.isInPigForm()) {
            cir.setReturnValue(PIG_DIMENSIONS);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void tickPigFormExpiry(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        long currentTick = player.level().getGameTime();
        PigFormHelper.cleanExpired(currentTick);

        PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(player);
        if (comp == null || !comp.isInPigForm()) return;

        Long expiryTick = PigFormHelper.getTransformExpiry(player.getUUID());
        if (expiryTick == null || expiryTick < 0) return;

        if (currentTick >= expiryTick) {
            comp.setPigForm(false, Util.NIL_UUID);
            PigFormHelper.clearTransformExpiry(player.getUUID());
            player.refreshDimensions();
        }
    }
}