package org.agmas.pathsrole.network;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.pathsrole.init.ModRoles;
import org.jetbrains.annotations.NotNull;

public record ShrineTaxPayload(int price) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShrineTaxPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "shrine_tax"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShrineTaxPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeInt(payload.price),
            (buf) -> new ShrineTaxPayload(buf.readInt())
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ShrineTaxPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer buyer = context.player();
        int price = payload.price();
        int tax = price / 5;
        if (tax <= 0) {
            return;
        }

        context.server().execute(() -> {
            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(buyer.serverLevel());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }

            ServerLevel level = buyer.serverLevel();
            for (ServerPlayer sp : level.players()) {
                if (!gameWorld.isRole(sp, ModRoles.REIMU)) {
                    continue;
                }
                SREPlayerShopComponent shop = SREPlayerShopComponent.KEY.get(sp);
                if (shop != null) {
                    shop.addToBalance(tax);
                }
                sp.displayClientMessage(
                        Component.translatable("message.pathsrole.shrine_tax.received",
                                buyer.getName().getString(), tax).withStyle(ChatFormatting.GOLD),
                        false);
                return;
            }
        });
    }
}