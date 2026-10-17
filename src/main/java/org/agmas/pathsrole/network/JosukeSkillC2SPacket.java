package org.agmas.pathsrole.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.PathsRoleMod;

import java.util.UUID;

public record JosukeSkillC2SPacket(UUID targetUuid) implements CustomPacketPayload {
    public static final ResourceLocation JOSUKE_SKILL_PAYLOAD_ID = ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "josuke_skill");
    public static final CustomPacketPayload.Type<JosukeSkillC2SPacket> ID = new CustomPacketPayload.Type<>(JOSUKE_SKILL_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, JosukeSkillC2SPacket> CODEC;

    public JosukeSkillC2SPacket(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.targetUuid);
    }

    public static JosukeSkillC2SPacket read(FriendlyByteBuf buf) {
        return new JosukeSkillC2SPacket(buf.readUUID());
    }

    static {
        CODEC = StreamCodec.ofMember(JosukeSkillC2SPacket::write, JosukeSkillC2SPacket::read);
    }
}