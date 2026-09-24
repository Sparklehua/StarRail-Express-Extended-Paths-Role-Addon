package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.api.RoleComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.game.GameUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.habi.roleport.role.component.taotie.TaotiePlayerComponent")
public class TaotiePlayerComponentMixin {

    @Inject(method = "swallowPlayer", at = @At("HEAD"), cancellable = true)
    private void checkReimuTarget(ServerPlayer target, CallbackInfoReturnable<Boolean> cir) {
        if (ModRoles.REIMU == null) return;
        if (target == null) return;
        SREGameWorldComponent game = SREGameWorldComponent.KEY.get(target.level());
        if (game == null) return;
        if (game.isRole(target, ModRoles.REIMU)) {
            var player = ((RoleComponent) (Object) this).getPlayer();
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.literal("§c对方是博丽灵梦，无法吞噬。"), true);
            }
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "checkAndTriggerMoment", at = @At("HEAD"), argsOnly = true)
    private int adjustAliveCount(int aliveCount) {
        if (ModRoles.REIMU == null) return aliveCount;
        var player = ((RoleComponent) (Object) this).getPlayer();
        if (player == null) return aliveCount;
        if (!(player.level() instanceof ServerLevel level)) return aliveCount;
        SREGameWorldComponent game = SREGameWorldComponent.KEY.get(level);
        if (game == null) return aliveCount;
        int reimuCount = 0;
        for (ServerPlayer p : level.players()) {
            if (GameUtils.isPlayerAliveAndSurvival(p) && game.isRole(p, ModRoles.REIMU)) {
                reimuCount++;
            }
        }
        return Math.max(0, aliveCount - reimuCount);
    }
}