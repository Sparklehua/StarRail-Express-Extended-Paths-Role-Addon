package org.agmas.pathsrole.client.screen;

import io.wifi.starrailexpress.util.ShopEntry;
import java.util.ArrayList;
import java.util.List;
import org.agmas.pathsrole.init.ModItems;

public class ShrineFactionShopHandler {

    public static List<ShopEntry> getEntries(ShrineShopType type) {
        switch (type) {
            case KILLER:
                return getKillerEntries();
            case NEUTRAL_KILLER:
                return getNeutralKillerEntries();
            case SPECIAL_NEUTRAL:
                return getSpecialNeutralEntries();
            case INNOCENT:
                return getInnocentEntries();
            default:
                return new ArrayList<>();
        }
    }

    private static List<ShopEntry> getKillerEntries() {
        List<ShopEntry> entries = new ArrayList<>();
        entries.add(new ShopEntry(ModItems.BROOM.getDefaultInstance(), 50, ShopEntry.Type.TOOL));
        // 手铐 200
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.HANDCUFFS.getDefaultInstance(), 200, ShopEntry.Type.TOOL));
        // 警长手枪 185
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.SHERIFF_REVOLVER.getDefaultInstance(), 185, ShopEntry.Type.TOOL));
        // 列车长钥匙 150（原巧匠钥匙位置）
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.MASTER_KEY.getDefaultInstance(), 150, ShopEntry.Type.TOOL));
        // 绳索 100
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.ROPE.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        // 刀 80
        entries.add(new ShopEntry(io.wifi.starrailexpress.index.TMMItems.KNIFE.getDefaultInstance(), 80, ShopEntry.Type.TOOL));
        // 撬梶 10
        entries.add(new ShopEntry(io.wifi.starrailexpress.index.TMMItems.CROWBAR.getDefaultInstance(), 10, ShopEntry.Type.TOOL));
        // 手榴弹 300
        entries.add(new ShopEntry(io.wifi.starrailexpress.index.TMMItems.GRENADE.getDefaultInstance(), 300, ShopEntry.Type.TOOL));
        // 毒药瓶 100
        entries.add(new ShopEntry(io.wifi.starrailexpress.index.TMMItems.POISON_VIAL.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        // 窥视之眼 100
        entries.add(new ShopEntry(ModItems.PEEPING_EYE.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        return entries;
    }

    private static List<ShopEntry> getNeutralKillerEntries() {
        List<ShopEntry> entries = new ArrayList<>();
        entries.add(new ShopEntry(ModItems.BROOM.getDefaultInstance(), 50, ShopEntry.Type.TOOL));
        // 巧匠钥匙 300（原列车长钥匙位置）
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.NOELL_ARTISAN_KEY.getDefaultInstance(), 300, ShopEntry.Type.TOOL));
        // 绳索 100
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.ROPE.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        // 毒药瓶 250
        entries.add(new ShopEntry(io.wifi.starrailexpress.index.TMMItems.POISON_VIAL.getDefaultInstance(), 250, ShopEntry.Type.TOOL));
        return entries;
    }

    private static List<ShopEntry> getSpecialNeutralEntries() {
        List<ShopEntry> entries = new ArrayList<>();
        entries.add(new ShopEntry(ModItems.BROOM.getDefaultInstance(), 50, ShopEntry.Type.TOOL));
        // 隐身药水30s（无气泡效果）125
        net.minecraft.world.item.ItemStack invisPotion = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION);
        invisPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
            new net.minecraft.world.item.alchemy.PotionContents(
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.List.of(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.INVISIBILITY, 600, 0, false, false, false))
            ));
        entries.add(new ShopEntry(invisPotion, 125, ShopEntry.Type.TOOL));
        // 速度药水（速度II，10s）250
        net.minecraft.world.item.ItemStack speedPotion = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION);
        speedPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
            new net.minecraft.world.item.alchemy.PotionContents(
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.List.of(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 200, 1, false, true, true))
            ));
        entries.add(new ShopEntry(speedPotion, 250, ShopEntry.Type.TOOL));
        // 烟雾弹 90
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.SMOKE_GRENADE.getDefaultInstance(), 90, ShopEntry.Type.TOOL));
        // 空包弹 90
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.BLANK_CARTRIDGE.getDefaultInstance(), 90, ShopEntry.Type.TOOL));
        // 轮椅 300
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.WHEELCHAIR.getDefaultInstance(), 300, ShopEntry.Type.TOOL));
        // 锁 450
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.LOCK_ITEM.getDefaultInstance(), 450, ShopEntry.Type.TOOL));
        // 铁门钥匙 380
        entries.add(new ShopEntry(io.wifi.starrailexpress.index.TMMItems.IRON_DOOR_KEY.getDefaultInstance(), 380, ShopEntry.Type.TOOL));
        return entries;
    }

    private static List<ShopEntry> getInnocentEntries() {
        List<ShopEntry> entries = new ArrayList<>();
        entries.add(new ShopEntry(ModItems.BROOM.getDefaultInstance(), 50, ShopEntry.Type.TOOL));
        // 解药 200
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.ANTIDOTE.getDefaultInstance(), 200, ShopEntry.Type.TOOL));
        // 照明弹 300
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.FLARE.getDefaultInstance(), 300, ShopEntry.Type.TOOL));
        // 零食 100
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.LINGSHI.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        // 一杯水 100
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.A_BOTTLE_OF_WATER.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        // 夜视仪 200
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.NIGHT_VISION_GLASSES.getDefaultInstance(), 200, ShopEntry.Type.TOOL));
        // 假手雷 600
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.FAKE_GRENADE.getDefaultInstance(), 600, ShopEntry.Type.TOOL));
        // 乘务员钥匙 100
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.MASTER_KEY_P.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        // 拳套 350
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.BOXING_GLOVE.getDefaultInstance(), 350, ShopEntry.Type.TOOL));
        // 巧克力 100
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.CHOCOLATE.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        // 对讲机 300
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.RADIO.getDefaultInstance(), 300, ShopEntry.Type.TOOL));
        // 存折 400
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.PASSBOOK.getDefaultInstance(), 400, ShopEntry.Type.TOOL));
        // 磁铁 350
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.MAGNET.getDefaultInstance(), 350, ShopEntry.Type.TOOL));
        // 彩虹马蹄铁 1000
        entries.add(new ShopEntry(org.agmas.noellesroles.init.FunnyItems.RAINBOW_HORSESHOE.getDefaultInstance(), 1000, ShopEntry.Type.TOOL));
        // 铁门钥匙 100
        entries.add(new ShopEntry(io.wifi.starrailexpress.index.TMMItems.IRON_DOOR_KEY.getDefaultInstance(), 100, ShopEntry.Type.TOOL));
        // 一次性手枪 600
        entries.add(new ShopEntry(org.agmas.noellesroles.init.ModItems.ONCE_REVOLVER.getDefaultInstance(), 600, ShopEntry.Type.TOOL));
        return entries;
    }
}