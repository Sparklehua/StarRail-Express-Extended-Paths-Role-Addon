package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.game.GameUtils;
import io.wifi.starrailexpress.game.modes.SREMurderGameMode;
import net.minecraft.server.level.ServerLevel;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SREMurderGameMode.class)
public class SREMurderGameModeMixin {

    @Inject(method = "allowGameEnd", at = @At("HEAD"), cancellable = true)
    private void checkShipperWinFirst(ServerLevel serverWorld, GameUtils.WinStatus winStatus,
                                       boolean isLooseEndsMode, SREGameWorldComponent gameWorldComponent,
                                       CallbackInfoReturnable<GameUtils.WinStatus> cir) {
        GameUtils.WinStatus shipperResult = ShipperEvents.checkShipperGameEnd(serverWorld, winStatus, isLooseEndsMode);
        if (shipperResult != GameUtils.WinStatus.NOT_MODIFY) {
            cir.setReturnValue(shipperResult);
        }
    }
}