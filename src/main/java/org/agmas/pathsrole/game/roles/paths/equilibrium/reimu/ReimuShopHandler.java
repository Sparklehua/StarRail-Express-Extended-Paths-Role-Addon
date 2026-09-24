package org.agmas.pathsrole.game.roles.paths.equilibrium.reimu;

import io.wifi.starrailexpress.game.ShopContent;
import io.wifi.starrailexpress.util.ShopEntry;
import java.util.ArrayList;
import net.minecraft.world.entity.player.Player;
import org.agmas.noellesroles.game.roles.innocence.fool.ShrineSequence;
import org.agmas.pathsrole.content.block.DonationBoxDataManager;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;
import org.jetbrains.annotations.NotNull;

public class ReimuShopHandler {
    public static ArrayList<ShopEntry> getEntries() {
        ArrayList<ShopEntry> shop = new ArrayList<ShopEntry>();
        shop.add(new UniqueItemShopEntry(ModItems.DONATION_BOX.getDefaultInstance(), 100, ShopEntry.Type.TOOL, ModItems.DONATION_BOX) {
            @Override
            public boolean canBuy(@NotNull Player player) {
                if (DonationBoxDataManager.countOwnedBoxes(player.getUUID()) >= 3) return false;
                return super.canBuy(player);
            }
        });
        shop.add(new SpellCardShopEntry(ModItems.REIMU_SPELL_CARD.getDefaultInstance(), 200, ShopEntry.Type.TOOL));
        shop.add(new UniqueItemShopEntry(ModItems.SHRINE.getDefaultInstance(), 125, ShopEntry.Type.TOOL, ModItems.SHRINE) {
            @Override
            public boolean canBuy(@NotNull Player player) {
                if (!ShrineSequence.canStart()) return false;
                return super.canBuy(player);
            }
        });
        return shop;
    }

    public static void init() {
        ShopContent.customEntries.put(ModRoles.REIMU_ID, ReimuShopHandler.getEntries());
    }
}