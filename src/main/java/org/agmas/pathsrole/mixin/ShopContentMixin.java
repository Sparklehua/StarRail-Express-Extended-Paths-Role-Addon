package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.game.ShopContent;
import io.wifi.starrailexpress.index.TMMItems;
import io.wifi.starrailexpress.util.ShopEntry;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.game.roles.paths.elation.kira.SheerHeartShopEntry;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ShopContent.class, priority = 500)
public class ShopContentMixin {
    @Inject(method = "getShopEntries", at = @At("HEAD"), cancellable = true)
    private static void kiraaddon$useKiraEntries(ResourceLocation role,
                                                   CallbackInfoReturnable<List<ShopEntry>> cir) {
        if (ModRoles.KIRA_ID.equals(role)) {
            List<ShopEntry> kiraShop = new java.util.ArrayList<>();
            kiraShop.add(new ShopEntry(
                    org.agmas.noellesroles.init.ModItems.FAKE_REVOLVER.getDefaultInstance(),
                    10, ShopEntry.Type.WEAPON));
            kiraShop.add(new ShopEntry(
                    TMMItems.BLACKOUT.getDefaultInstance(),
                    100, ShopEntry.Type.TOOL));
            kiraShop.add(new ShopEntry(
                    TMMItems.LOCKPICK.getDefaultInstance(),
                    80, ShopEntry.Type.TOOL));
            kiraShop.add(new ShopEntry(
                    TMMItems.KNIFE.getDefaultInstance(),
                    105, ShopEntry.Type.WEAPON));
            kiraShop.add(new SheerHeartShopEntry(
                    ModItems.KIRA_SHEER_HEART_ATTACK.getDefaultInstance(),
                    125, ShopEntry.Type.WEAPON));
            cir.setReturnValue(kiraShop);
            PathsRoleMod.LOGGER.info("Kira shop fully overridden with custom entries!");
        }
    }
}