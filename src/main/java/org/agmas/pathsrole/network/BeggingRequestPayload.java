package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record BeggingRequestPayload(UUID beggarUuid, String beggarName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BeggingRequestPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "begging_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeggingRequestPayload> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.beggarUuid);
                buf.writeUtf(packet.beggarName);
            },
            (buf) -> new BeggingRequestPayload(buf.readUUID(), buf.readUtf())
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}