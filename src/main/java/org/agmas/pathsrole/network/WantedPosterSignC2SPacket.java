package org.agmas.pathsrole.network;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.game.GameUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.content.item.WantedPosterItem;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;

import java.util.UUID;

public record WantedPosterSignC2SPacket(UUID targetUuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WantedPosterSignC2SPacket> TYPE =
            new CustomPacketPayload.Type<>(PathsRoleMod.id("wanted_poster_sign"));

    public static final StreamCodec<FriendlyByteBuf, WantedPosterSignC2SPacket> CODEC =
            CustomPacketPayload.codec(WantedPosterSignC2SPacket::write, WantedPosterSignC2SPacket::new);

    public WantedPosterSignC2SPacket(FriendlyByteBuf buf) {
        this(buf.readUUID());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(targetUuid);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WantedPosterSignC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayer signer = context.player();
        context.server().execute(() -> {
            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(signer.level());
            if (!gameWorld.isRunning() || !GameUtils.isPlayerAliveAndSurvivalIgnoreShitSplit(signer)) {
                return;
            }
            if (gameWorld.isRole(signer, ModRoles.BOUNTY_HUNTER)) {
                signer.displayClientMessage(
                        Component.translatable("message.pathsrole.bounty_hunter.cannot_select_hunter")
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            Player target = signer.level().getPlayerByUUID(payload.targetUuid());
            if (target == null || target == signer || !GameUtils.isPlayerAliveAndSurvival(target)) {
                return;
            }
            if (gameWorld.isRole(target, ModRoles.BOUNTY_HUNTER)) {
                signer.displayClientMessage(
                        Component.translatable("message.pathsrole.bounty_hunter.cannot_select_hunter")
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }
            if (gameWorld.isKillerTeam(target)) {
                signer.displayClientMessage(
                        Component.translatable("message.pathsrole.bounty_hunter.cannot_select_killer")
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            ItemStack posterStack = ItemStack.EMPTY;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack handItem = signer.getItemInHand(hand);
                if (handItem.is(ModItems.WANTED_POSTER)
                        && WantedPosterItem.getState(handItem) == WantedPosterItem.PosterState.UNASSIGNED) {
                    posterStack = handItem;
                    break;
                }
            }

            if (posterStack.isEmpty()) {
                return;
            }

            SREPlayerShopComponent shop = SREPlayerShopComponent.KEY.get(signer);
            if (shop.balance < 50) {
                signer.displayClientMessage(
                        Component.translatable("message.pathsrole.bounty_hunter.not_enough_coins", 50)
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            shop.addToBalance(-50);
            WantedPosterItem.setTarget(posterStack,
                    target.getUUID().toString(),
                    target.getName().getString());

            signer.displayClientMessage(
                    Component.translatable("message.pathsrole.bounty_hunter.target_selected",
                            target.getName().getString()).withStyle(ChatFormatting.GOLD),
                    true);
        });
    }
}