package org.agmas.pathsrole.network;

import net.minecraft.world.phys.Vec3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;

public record ReimuBarrierPacket(Vec3 center, boolean active, int duration) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ReimuBarrierPacket> ID = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"pathsrole", (String)"reimu_barrier"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReimuBarrierPacket> CODEC = StreamCodec.of(ReimuBarrierPacket::encode, ReimuBarrierPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, ReimuBarrierPacket packet) {
        buf.writeDouble(packet.center.x);
        buf.writeDouble(packet.center.y);
        buf.writeDouble(packet.center.z);
        buf.writeBoolean(packet.active);
        buf.writeInt(packet.duration);
    }

    private static ReimuBarrierPacket decode(RegistryFriendlyByteBuf buf) {
        return new ReimuBarrierPacket(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readBoolean(), buf.readInt());
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}