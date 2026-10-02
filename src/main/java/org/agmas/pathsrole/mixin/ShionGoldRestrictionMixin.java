package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionPlayerComponent;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SREPlayerShopComponent.class)
public class ShionGoldRestrictionMixin {

    @Inject(method = "addToBalance(I)V", at = @At("HEAD"), cancellable = true)
    private void restrictShionBalance(int amount, CallbackInfo ci) {
        if (amount <= 0) return;

        SREPlayerShopComponent self = (SREPlayerShopComponent) (Object) this;
        Player player = self.getPlayer();
        if (player == null || player.level().isClientSide()) return;

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
        if (gameWorld == null || !gameWorld.isRunning()) return;
        if (!gameWorld.isRole(player, ModRoles.SHION)) return;

        ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
        if (comp == null) return;

        if (!comp.isBegGoldPending()) {
            ci.cancel();
        }
    }
}