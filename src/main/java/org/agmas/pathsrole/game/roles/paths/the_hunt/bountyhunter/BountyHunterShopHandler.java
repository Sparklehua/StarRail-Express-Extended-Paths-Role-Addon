package org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter;

import io.wifi.starrailexpress.game.ShopContent;
import io.wifi.starrailexpress.index.TMMItems;
import io.wifi.starrailexpress.util.ShopEntry;
import java.util.ArrayList;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;

public class BountyHunterShopHandler {
    public static ArrayList<ShopEntry> getEntries() {
        ArrayList<ShopEntry> shop = new ArrayList<ShopEntry>();
        shop.add(new ShopEntry(ModItems.WANTED_POSTER.getDefaultInstance(), 75, ShopEntry.Type.TOOL));
        shop.add(new ShopEntry(ModItems.HUNT_TEMP_SHIELD.getDefaultInstance(), 120, ShopEntry.Type.TOOL));
        shop.add(new ShopEntry(TMMItems.LOCKPICK.getDefaultInstance(), 50, ShopEntry.Type.TOOL));
        shop.add(new ShopEntry(TMMItems.REVOLVER.getDefaultInstance(), 150, ShopEntry.Type.TOOL));
        shop.add(new ShopEntry(TMMItems.DERRINGER.getDefaultInstance(), 220, ShopEntry.Type.TOOL));
        shop.add(new ShopEntry(TMMItems.KNIFE.getDefaultInstance(), 80, ShopEntry.Type.TOOL));
        return shop;
    }

    public static void init() {
        ShopContent.customEntries.put(ModRoles.BOUNTY_HUNTER_ID, BountyHunterShopHandler.getEntries());
    }
}