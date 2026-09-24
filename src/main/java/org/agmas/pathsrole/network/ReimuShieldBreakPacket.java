package org.agmas.pathsrole.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;

public record ReimuShieldBreakPacket() implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ReimuShieldBreakPacket> ID = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath("pathsrole", "reimu_shield_break"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReimuShieldBreakPacket> CODEC = StreamCodec.unit(new ReimuShieldBreakPacket());

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}