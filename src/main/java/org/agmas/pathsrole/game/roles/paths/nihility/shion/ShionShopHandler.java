package org.agmas.pathsrole.game.roles.paths.nihility.shion;

import io.wifi.starrailexpress.game.ShopContent;
import io.wifi.starrailexpress.util.ShopEntry;
import java.util.ArrayList;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;

public class ShionShopHandler {

    public static ArrayList<ShopEntry> getEntries() {
        ArrayList<ShopEntry> shop = new ArrayList<>();

        shop.add(new ShopEntry(ModItems.BOWL.getDefaultInstance(), 50, ShopEntry.Type.TOOL));

        shop.add(new ShopEntry(ModItems.CANE.getDefaultInstance(), 100, ShopEntry.Type.TOOL));

        return shop;
    }

    public static void init() {
        ShopContent.customEntries.put(ModRoles.SHION_ID, ShionShopHandler.getEntries());
    }
}