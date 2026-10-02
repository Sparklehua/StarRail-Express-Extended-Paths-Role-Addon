package org.agmas.pathsrole.network;

import io.wifi.starrailexpress.game.GameUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionEvents;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record BeggingResponsePayload(UUID beggarUuid, boolean accepted, int amount) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BeggingResponsePayload> TYPE =
            new CustomPacketPayload.Type<>(PathsRoleMod.id("begging_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeggingResponsePayload> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.beggarUuid);
                buf.writeBoolean(packet.accepted);
                buf.writeInt(packet.amount);
            },
            (buf) -> new BeggingResponsePayload(buf.readUUID(), buf.readBoolean(), buf.readInt())
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BeggingResponsePayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer donor = context.player();
        context.server().execute(() -> {
            if (!GameUtils.isPlayerAliveAndSurvival(donor)) return;

            Player target = donor.serverLevel().getPlayerByUUID(payload.beggarUuid());
            if (!(target instanceof ServerPlayer beggar)) return;
            if (!GameUtils.isPlayerAliveAndSurvival(beggar)) return;

            ShionEvents.handleBegResponse(beggar, donor, payload.accepted(), payload.amount());
        });
    }
}