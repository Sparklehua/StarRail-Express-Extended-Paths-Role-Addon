package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record BanTogglePayload(UUID playerUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BanTogglePayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "ban_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BanTogglePayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUUID(payload.playerUuid()),
            (buf) -> new BanTogglePayload(buf.readUUID())
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}