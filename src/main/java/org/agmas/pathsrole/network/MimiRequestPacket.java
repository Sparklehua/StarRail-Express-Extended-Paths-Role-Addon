package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record MimiRequestPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MimiRequestPacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "mimi_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MimiRequestPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {},
            (buf) -> new MimiRequestPacket()
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}