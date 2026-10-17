package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.init.ModModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SREGameWorldComponent.class, remap = false)
public class ShipperMarkGunRestrictionMixin {

    @Inject(method = "canPickUpRevolver", at = @At("HEAD"), cancellable = true, require = 0)
    private void pathsrole$blockInnocentShipperMarkPickupGun(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        WorldModifierComponent wmc = WorldModifierComponent.KEY.get(serverLevel);
        if (wmc == null || !wmc.isModifier(player.getUUID(), ModModifiers.SHIPPER_MARK)) {
            return;
        }

        SREGameWorldComponent gameWorld = (SREGameWorldComponent)(Object)this;
        SRERole role = gameWorld.getRole(player);
        if (role == null || !role.isInnocent()) {
            return;
        }

        for (ServerPlayer sp : serverLevel.players()) {
            if (!gameWorld.isRole(sp, ModRoles.SHIPPER)) continue;
            ShipperPlayerComponent shipperComp = ShipperPlayerComponent.KEY.getNullable(sp);
            if (shipperComp != null && shipperComp.isShipperMomentActive()) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
        }
    }
}