package org.agmas.pathsrole.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class PigPlayerEyeHeightMixin {

    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    private void getPigEyeHeight(CallbackInfoReturnable<Float> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player))
            return;
        PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(entity);
        if (comp != null && comp.isInPigForm()) {
            cir.setReturnValue(0.7F);
        }
    }
}