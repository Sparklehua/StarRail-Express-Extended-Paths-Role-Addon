package org.agmas.pathsrole.game.roles.paths.elation.kira;

import io.wifi.starrailexpress.game.ShopContent;
import io.wifi.starrailexpress.index.TMMItems;
import io.wifi.starrailexpress.util.ShopEntry;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;

import java.util.ArrayList;

public class KiraShopHandler {
    public static ArrayList<ShopEntry> getEntries() {
        ArrayList<ShopEntry> kiraShop = new ArrayList<>();

        kiraShop.add(new ShopEntry(
            org.agmas.noellesroles.init.ModItems.FAKE_REVOLVER.getDefaultInstance(),
            10,
            ShopEntry.Type.WEAPON));

        kiraShop.add(new ShopEntry(TMMItems.BLACKOUT.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        kiraShop.add(new ShopEntry(TMMItems.LOCKPICK.getDefaultInstance(), 80, ShopEntry.Type.TOOL));
        kiraShop.add(new ShopEntry(TMMItems.KNIFE.getDefaultInstance(), 105, ShopEntry.Type.WEAPON));
        kiraShop.add(new SheerHeartShopEntry(
            ModItems.KIRA_SHEER_HEART_ATTACK.getDefaultInstance(), 125, ShopEntry.Type.WEAPON));
        return kiraShop;
    }

    public static void init() {
        PathsRoleMod.LOGGER.info("Initializing Kira shop entries for {}", ModRoles.KIRA_ID);
        var kiraShop = getEntries();

        ShopContent.customEntries.put(ModRoles.KIRA_ID, kiraShop);

        PathsRoleMod.LOGGER.info("Kira shop overridden with custom entries!");
        PathsRoleMod.LOGGER.info("Shop entries count: {}", kiraShop.size());
        PathsRoleMod.LOGGER.info("Custom entries contains KIRA_ID: {}", ShopContent.customEntries.containsKey(ModRoles.KIRA_ID));
    }
}