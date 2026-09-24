package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "com.cowboymod.network.CowboyDuelPacket", remap = false)
public class CowboyDuelPacketMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, require = 0)
    private static void checkReimuImmunity(ServerPlayer player, UUID targetUuid, CallbackInfo ci) {
        ServerPlayer tsp = player.getServer().getPlayerList().getPlayer(targetUuid);
        if (tsp == null) return;

        SREGameWorldComponent game = SREGameWorldComponent.KEY.get(tsp.level());
        if (game == null || !game.isRunning()) return;

        if (ModRoles.REIMU != null && game.isRole(tsp, ModRoles.REIMU)) {
            player.sendSystemMessage(
                Component.literal("§c对方是博丽灵梦，无法发起决斗。"), true);
            ci.cancel();
        }
    }
}