package org.agmas.pathsrole.game.roles.paths.elation.kira;

import io.wifi.starrailexpress.util.ShopEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.agmas.pathsrole.content.entity.SheerHeartEntity;
import org.agmas.pathsrole.init.ModItems;
import org.jetbrains.annotations.NotNull;

public class SheerHeartShopEntry extends ShopEntry {
    public SheerHeartShopEntry(ItemStack stack, int price, Type type) {
        super(stack, price, type);
    }

    @Override
    public boolean canBuy(@NotNull Player player) {
        if (!super.canBuy(player)) {
            return false;
        }

        if (hasActiveSheerHeart(player)) {
            return false;
        }

        if (hasSheerHeartInInventory(player)) {
            return false;
        }

        return true;
    }

    private boolean hasActiveSheerHeart(Player player) {
        return player.level().getEntitiesOfClass(SheerHeartEntity.class,
                player.getBoundingBox().inflate(200),
                entity -> entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID())).size() > 0;
    }

    private boolean hasSheerHeartInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.KIRA_SHEER_HEART_ATTACK)) {
                return true;
            }
        }
        return false;
    }
}