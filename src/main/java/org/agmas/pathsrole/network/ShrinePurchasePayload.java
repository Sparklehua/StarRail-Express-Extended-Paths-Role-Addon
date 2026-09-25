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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.agmas.pathsrole.ShrineShopType;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.server.ShrinePurchaseTracker;
import org.jetbrains.annotations.NotNull;

public record ShrinePurchasePayload(ItemStack item, int price, ShrineShopType shopType) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShrinePurchasePayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pathsrole", "shrine_purchase"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShrinePurchasePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                ItemStack.STREAM_CODEC.encode(buf, payload.item());
                buf.writeInt(payload.price());
                buf.writeEnum(payload.shopType());
            },
            (buf) -> new ShrinePurchasePayload(
                    ItemStack.STREAM_CODEC.decode(buf),
                    buf.readInt(),
                    buf.readEnum(ShrineShopType.class)
            )
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(ShrinePurchasePayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer buyer = context.player();
        if (buyer == null) return;

        ItemStack item = payload.item();
        int price = payload.price();
        ShrineShopType shopType = payload.shopType();

        if (item.isEmpty() || price <= 0) {
            return;
        }

        context.server().execute(() -> {
            // === 阵营购买次数检查 ===
            if (!ShrinePurchaseTracker.hasRemaining(shopType)) {
                buyer.displayClientMessage(
                        Component.translatable("message.pathsrole.shrine_shop.no_remaining_purchases")
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            SREPlayerShopComponent shop = SREPlayerShopComponent.KEY.get(buyer);
            if (shop == null) {
                return;
            }

            if (shop.balance < price) {
                buyer.displayClientMessage(
                        Component.translatable("message.pathsrole.shrine_shop.not_enough_money")
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            // 扣除一次阵营购买次数
            ShrinePurchaseTracker.consume(shopType);
            int remaining = ShrinePurchaseTracker.getRemaining(shopType);

            shop.setBalance(shop.balance - price);
            shop.sync();

            ItemStack toGive = item.copy();
            if (buyer.getInventory() != null && !buyer.getInventory().add(toGive)) {
                buyer.drop(toGive, false);
            }

            buyer.level().playSound(null, buyer.getX(), buyer.getY(), buyer.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);

            buyer.displayClientMessage(
                    Component.translatable("message.pathsrole.shrine_shop.purchase_success",
                            item.getDisplayName().getString(), price)
                            .withStyle(ChatFormatting.GREEN),
                    false);

            // 提示该阵营剩余次数
            buyer.displayClientMessage(
                    Component.translatable("message.pathsrole.shrine_shop.remaining_count",
                            remaining, ShrinePurchaseTracker.getMaxPurchases())
                            .withStyle(ChatFormatting.AQUA),
                    true);

            int tax = price / 5;
            if (tax <= 0) {
                return;
            }

            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(buyer.serverLevel());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }

            ServerLevel level = buyer.serverLevel();
            if (level == null) return;

            for (ServerPlayer sp : level.players()) {
                if (!gameWorld.isRole(sp, ModRoles.REIMU)) {
                    continue;
                }
                SREPlayerShopComponent reimuShop = SREPlayerShopComponent.KEY.get(sp);
                if (reimuShop != null) {
                    reimuShop.addToBalance(tax);
                }
                sp.displayClientMessage(
                        Component.translatable("message.pathsrole.shrine_tax.received",
                                buyer.getName().getString(), tax)
                                .withStyle(ChatFormatting.GOLD),
                        false);
                return;
            }
        });
    }
}