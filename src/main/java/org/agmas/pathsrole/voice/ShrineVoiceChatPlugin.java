package org.agmas.pathsrole.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;
import org.agmas.noellesroles.init.ModEffects;

public class ShrineVoiceChatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return "pathsrole_shrine_voice";
    }

    @Override
    public void initialize(VoicechatApi api) {
        VoicechatPlugin.super.initialize(api);
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        if (api == null) {
            return;
        }

        var connection = event.getSenderConnection();
        if (connection == null || !connection.isInstalled() || !connection.isConnected()) {
            return;
        }

        var vcPlayer = connection.getPlayer();
        if (vcPlayer == null) {
            return;
        }

        var player = vcPlayer.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        if (!ShrineSequence.isShrineProtected(serverPlayer.getUUID())) {
            return;
        }
        if (serverPlayer.hasEffect(ModEffects.VOICE_SILENCE)) {
            return;
        }

        event.cancel();

        PlayerList playerList = server.getPlayerList();
        if (playerList == null) {
            return;
        }

        for (ServerPlayer receiver : playerList.getPlayers()) {
            if (receiver == null) {
                continue;
            }
            if (receiver.getUUID().equals(serverPlayer.getUUID())) {
                continue;
            }
            if (!ShrineSequence.isShrineProtected(receiver.getUUID())) {
                continue;
            }

            VoicechatConnection con = api.getConnectionOf(receiver.getUUID());
            if (con == null || !con.isInstalled() || !con.isConnected()) {
                continue;
            }
            api.sendLocationalSoundPacketTo(con, event.getPacket()
                    .locationalSoundPacketBuilder()
                    .position(api.createPosition(
                            receiver.getX(), receiver.getY(), receiver.getZ()))
                    .distance((float) api.getVoiceChatDistance())
                    .build());
        }
    }
}