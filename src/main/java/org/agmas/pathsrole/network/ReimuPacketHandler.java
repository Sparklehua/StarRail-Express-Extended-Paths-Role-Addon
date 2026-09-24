package org.agmas.pathsrole.network;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.ReimuBarrierPacket;
import org.agmas.pathsrole.network.MimiRequestPacket;
import org.agmas.pathsrole.network.MimiResponsePacket;
import org.agmas.pathsrole.network.BanListRequestPayload;
import org.agmas.pathsrole.network.BanListResponsePayload;
import org.agmas.pathsrole.network.BanTogglePayload;
import org.agmas.pathsrole.messaging.MimiAccessManager;

public class ReimuPacketHandler {

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ReimuBarrierPacket.ID, ReimuBarrierPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(MimiRequestPacket.ID, MimiRequestPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(MimiResponsePacket.ID, MimiResponsePacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BanListRequestPayload.ID, BanListRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BanListResponsePayload.ID, BanListResponsePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BanTogglePayload.ID, BanTogglePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReimuBarrierPacket.ID, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player == null || player.level().isClientSide()) {
                return;
            }
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)player.level());
            if (gameWorld == null || !gameWorld.isRunning() || !gameWorld.isRole((Player)player, ModRoles.REIMU)) {
                return;
            }
            ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent((Player)player);
            if (comp == null) {
                return;
            }
            comp.activateBarrier(player, true);
        }));
        ServerPlayNetworking.registerGlobalReceiver(MimiRequestPacket.ID, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player == null || player.level().isClientSide()) {
                return;
            }
            MimiAccessManager.load();
            boolean isOp = player.hasPermissions(2);
            if (!isOp) {
                ServerPlayNetworking.send(player, new MimiResponsePacket(false, MimiAccessManager.isLocked()));
                return;
            }
            boolean canOpen = MimiAccessManager.canPlayerOpen(player.getUUID());
            ServerPlayNetworking.send(player, new MimiResponsePacket(canOpen, MimiAccessManager.isLocked()));
        }));

        ServerPlayNetworking.registerGlobalReceiver(BanListRequestPayload.ID, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player == null || player.level().isClientSide() || player.getServer() == null) {
                return;
            }
            HashMap<UUID, String> playerNames = new HashMap<>();
            for (ServerPlayer sp : player.getServer().getPlayerList().getPlayers()) {
                if (sp.isAlive()) {
                    playerNames.put(sp.getUUID(), sp.getName().getString());
                }
            }
            HashSet<UUID> banned = new HashSet<>(ShrineSequence.getBannedPlayers());
            ServerPlayNetworking.send(player, new BanListResponsePayload(playerNames, banned));
        }));

        ServerPlayNetworking.registerGlobalReceiver(BanTogglePayload.ID, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player == null || player.level().isClientSide()) {
                return;
            }
            SREGameWorldComponent gameWorld = (SREGameWorldComponent) SREGameWorldComponent.KEY.get((Object) player.level());
            if (gameWorld == null || !gameWorld.isRunning() || !gameWorld.isRole((Player) player, ModRoles.REIMU)) {
                return;
            }
            ShrineSequence.togglePlayerBan(payload.playerUuid());
        }));
    }
}