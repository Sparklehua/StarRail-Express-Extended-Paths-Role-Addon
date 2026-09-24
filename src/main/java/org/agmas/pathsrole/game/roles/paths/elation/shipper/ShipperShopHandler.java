package org.agmas.pathsrole.game.roles.paths.elation.shipper;

import io.wifi.starrailexpress.util.ShopEntry;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.agmas.noellesroles.init.ModItems;

public class ShipperShopHandler {
    private static List<ShopEntry> entries;

    public static void init() {
        entries = List.of(new ShopEntry(new ItemStack((net.minecraft.world.level.ItemLike)ModItems.MASTER_KEY_P), 50, ShopEntry.Type.TOOL));
    }

    public static List<ShopEntry> getEntries() {
        return entries;
    }
}