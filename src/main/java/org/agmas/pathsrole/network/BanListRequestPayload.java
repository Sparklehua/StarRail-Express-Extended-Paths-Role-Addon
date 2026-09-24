package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record BanListRequestPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BanListRequestPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "ban_list_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BanListRequestPayload> CODEC = StreamCodec.of(
            (buf, packet) -> {},
            (buf) -> new BanListRequestPayload()
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}