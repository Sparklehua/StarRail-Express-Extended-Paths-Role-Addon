package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record PlayerVisibilityStatePayload(Set<UUID> hiddenPlayerUuids) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerVisibilityStatePayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "player_visibility_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVisibilityStatePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.hiddenPlayerUuids().size());
                for (UUID uuid : payload.hiddenPlayerUuids()) {
                    buf.writeUUID(uuid);
                }
            },
            (buf) -> {
                int count = buf.readVarInt();
                Set<UUID> uuids = new HashSet<>();
                for (int i = 0; i < count; i++) {
                    uuids.add(buf.readUUID());
                }
                return new PlayerVisibilityStatePayload(uuids);
            }
    );

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}