package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.PathsRoleMod;

public record ShrineGhostStartPayload(
        long startTimeMs,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        double playerStartY
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShrineGhostStartPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "shrine_ghost_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShrineGhostStartPayload> CODEC =
            StreamCodec.of(ShrineGhostStartPayload::encode, ShrineGhostStartPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, ShrineGhostStartPayload payload) {
        buf.writeLong(payload.startTimeMs);
        buf.writeDouble(payload.minX);
        buf.writeDouble(payload.minY);
        buf.writeDouble(payload.minZ);
        buf.writeDouble(payload.maxX);
        buf.writeDouble(payload.maxY);
        buf.writeDouble(payload.maxZ);
        buf.writeDouble(payload.playerStartY);
    }

    private static ShrineGhostStartPayload decode(RegistryFriendlyByteBuf buf) {
        return new ShrineGhostStartPayload(
                buf.readLong(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}