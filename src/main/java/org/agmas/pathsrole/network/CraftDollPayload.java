package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CraftDollPayload(String playerName, float[] boneData,
                                String hatItemId, float[] hatTransform,
                                String handItemId, float[] handTransform) implements CustomPacketPayload {

    public static final Type<CraftDollPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "craft_doll"));

    private static void writeFloatArray(RegistryFriendlyByteBuf buf, float[] arr) {
        if (arr == null) {
            buf.writeVarInt(0);
        } else {
            buf.writeVarInt(arr.length);
            for (float f : arr) buf.writeFloat(f);
        }
    }

    private static float[] readFloatArray(RegistryFriendlyByteBuf buf) {
        int len = buf.readVarInt();
        if (len == 0) return new float[0];
        float[] arr = new float[len];
        for (int i = 0; i < len; i++) arr[i] = buf.readFloat();
        return arr;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftDollPayload> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUtf(packet.playerName);
                writeFloatArray(buf, packet.boneData);
                buf.writeUtf(packet.hatItemId != null ? packet.hatItemId : "");
                writeFloatArray(buf, packet.hatTransform);
                buf.writeUtf(packet.handItemId != null ? packet.handItemId : "");
                writeFloatArray(buf, packet.handTransform);
            },
            (buf) -> {
                String name = buf.readUtf();
                float[] boneData = readFloatArray(buf);
                String hatItemId = buf.readUtf();
                float[] hatTransform = readFloatArray(buf);
                String handItemId = buf.readUtf();
                float[] handTransform = readFloatArray(buf);
                return new CraftDollPayload(name, boneData,
                        hatItemId.isEmpty() ? null : hatItemId,
                        hatTransform,
                        handItemId.isEmpty() ? null : handItemId,
                        handTransform);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}