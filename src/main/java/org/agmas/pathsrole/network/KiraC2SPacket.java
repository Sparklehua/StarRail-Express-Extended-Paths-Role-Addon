package org.agmas.pathsrole.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.PathsRoleMod;

import java.util.UUID;

public record KiraC2SPacket(UUID player, int action) implements CustomPacketPayload {
    public static final ResourceLocation KIRA_PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "kira");
    public static final CustomPacketPayload.Type<KiraC2SPacket> ID = new CustomPacketPayload.Type<>(KIRA_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, KiraC2SPacket> CODEC;

    public static final int ACTION_MARK = 0;
    public static final int ACTION_DETONATE = 1;
    public static final int ACTION_TOGGLE_JEB = 2;
    public static final int ACTION_ANCHOR_MARK = 3;

    public KiraC2SPacket(UUID player, int action) {
        this.player = player;
        this.action = action;
    }

    public boolean isValidAction() {
        return action >= ACTION_MARK && action <= ACTION_ANCHOR_MARK;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.action);
    }

    public static KiraC2SPacket read(FriendlyByteBuf buf) {
        return new KiraC2SPacket(buf.readUUID(), buf.readInt());
    }

    static {
        CODEC = StreamCodec.ofMember(KiraC2SPacket::write, KiraC2SPacket::read);
    }
}