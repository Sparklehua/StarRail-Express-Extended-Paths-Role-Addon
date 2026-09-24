package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record MimiResponsePacket(boolean allowed, boolean locked) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MimiResponsePacket> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "mimi_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MimiResponsePacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.allowed);
                buf.writeBoolean(packet.locked);
            },
            (buf) -> new MimiResponsePacket(buf.readBoolean(), buf.readBoolean())
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}