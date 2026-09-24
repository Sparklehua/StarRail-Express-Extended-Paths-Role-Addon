package org.agmas.pathsrole.game.roles.paths.equilibrium.reimu;

import io.wifi.starrailexpress.util.ShopEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.agmas.pathsrole.init.ModItems;
import org.jetbrains.annotations.NotNull;

public class SpellCardShopEntry extends ShopEntry {
    public SpellCardShopEntry(ItemStack stack, int price, Type type) {
        super(stack, price, type);
    }

    @Override
    public boolean canBuy(@NotNull Player player) {
        if (!super.canBuy(player)) {
            return false;
        }

        ReimuPlayerComponent rpc = ReimuPlayerComponent.KEY.get(player);
        if (rpc != null && rpc.getSpellCardCooldown() > 0) {
            return false;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.REIMU_SPELL_CARD)) {
                return false;
            }
        }
        return true;
    }
}