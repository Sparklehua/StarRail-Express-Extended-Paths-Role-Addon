package org.agmas.pathsrole.game.roles.paths.equilibrium.reimu;

import io.wifi.starrailexpress.util.ShopEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class UniqueItemShopEntry extends ShopEntry {
    private final Item uniqueItem;

    public UniqueItemShopEntry(ItemStack stack, int price, Type type, Item uniqueItem) {
        super(stack, price, type);
        this.uniqueItem = uniqueItem;
    }

    @Override
    public boolean canBuy(@NotNull Player player) {
        if (!super.canBuy(player)) {
            return false;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(uniqueItem)) {
                return false;
            }
        }
        return true;
    }
}