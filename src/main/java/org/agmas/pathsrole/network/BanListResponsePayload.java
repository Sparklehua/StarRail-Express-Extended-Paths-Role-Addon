package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public record BanListResponsePayload(HashMap<UUID, String> playerNames, HashSet<UUID> bannedPlayers) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BanListResponsePayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "ban_list_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BanListResponsePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.playerNames().size());
                for (var entry : payload.playerNames().entrySet()) {
                    buf.writeUUID(entry.getKey());
                    buf.writeUtf(entry.getValue());
                }
                buf.writeInt(payload.bannedPlayers().size());
                for (UUID uuid : payload.bannedPlayers()) {
                    buf.writeUUID(uuid);
                }
            },
            (buf) -> {
                int nameCount = buf.readInt();
                HashMap<UUID, String> names = new HashMap<>();
                for (int i = 0; i < nameCount; i++) {
                    names.put(buf.readUUID(), buf.readUtf());
                }
                int banCount = buf.readInt();
                HashSet<UUID> banned = new HashSet<>();
                for (int i = 0; i < banCount; i++) {
                    banned.add(buf.readUUID());
                }
                return new BanListResponsePayload(names, banned);
            }
    );

    private static HashMap<UUID, String> cachedPlayerNames = new HashMap<>();
    private static HashSet<UUID> cachedBannedPlayers = new HashSet<>();
    private static boolean hasData = false;

    public static HashMap<UUID, String> getCachedPlayerNames() {
        return cachedPlayerNames;
    }

    public static HashSet<UUID> getCachedBannedPlayers() {
        return cachedBannedPlayers;
    }

    public static boolean hasData() {
        return hasData;
    }

    public static void updateCache(HashMap<UUID, String> names, HashSet<UUID> banned) {
        cachedPlayerNames = names;
        cachedBannedPlayers = banned;
        hasData = true;
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}