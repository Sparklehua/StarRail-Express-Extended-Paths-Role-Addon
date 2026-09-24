package org.agmas.pathsrole.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class ShipperInvisibleMixin {

    @Inject(method = "isInvisible", at = @At("RETURN"), cancellable = true)
    private void pathsrole$fadeIsInvisible(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        Entity entity = (Entity)(Object)this;
        if (!(entity instanceof Player player)) return;
        ShipperPlayerComponent comp = ShipperPlayerComponent.KEY.get(player);
        if (comp != null && comp.getInvisibilityAlpha() <= 0.01f) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isInvisibleTo", at = @At("RETURN"), cancellable = true)
    private void pathsrole$fadeIsInvisibleTo(Player observer, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        Entity entity = (Entity)(Object)this;
        if (!(entity instanceof Player player)) return;
        ShipperPlayerComponent comp = ShipperPlayerComponent.KEY.get(player);
        if (comp != null && comp.getInvisibilityAlpha() <= 0.01f) {
            cir.setReturnValue(true);
        }
    }
}