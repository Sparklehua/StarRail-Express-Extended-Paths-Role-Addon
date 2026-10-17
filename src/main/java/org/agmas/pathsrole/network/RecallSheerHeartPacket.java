package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.agmas.pathsrole.PathsRoleMod;

public record RecallSheerHeartPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RecallSheerHeartPacket> ID = new CustomPacketPayload.Type<>(PathsRoleMod.id("recall_sheer_heart"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecallSheerHeartPacket> CODEC = StreamCodec.of(
        (packet, buf) -> {},
        buf -> new RecallSheerHeartPacket()
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}