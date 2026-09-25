package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 服务器→客户端：同步各阵营剩余购买次数
 */
public record ShrinePurchaseCountSyncPayload(
        int killerRemaining,
        int neutralKillerRemaining,
        int specialNeutralRemaining,
        int innocentRemaining,
        boolean specialNeutralPotionPurchased
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShrinePurchaseCountSyncPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "shrine_purchase_count_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShrinePurchaseCountSyncPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.killerRemaining);
                buf.writeInt(payload.neutralKillerRemaining);
                buf.writeInt(payload.specialNeutralRemaining);
                buf.writeInt(payload.innocentRemaining);
                buf.writeBoolean(payload.specialNeutralPotionPurchased);
            },
            (buf) -> new ShrinePurchaseCountSyncPayload(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean()
            )
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}