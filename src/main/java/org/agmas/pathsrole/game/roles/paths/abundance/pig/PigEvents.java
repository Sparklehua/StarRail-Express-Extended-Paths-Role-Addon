package org.agmas.pathsrole.game.roles.paths.abundance.pig;

import io.wifi.starrailexpress.SRE;
import io.wifi.starrailexpress.rules.ArmorRules;
import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.cca.SREPlayerTaskComponent;
import io.wifi.starrailexpress.content.item.CocktailItem;
import io.wifi.starrailexpress.event.AllowPlayerDeath;
import io.wifi.starrailexpress.event.OnPlayerDeath;
import io.wifi.starrailexpress.event.OnShieldBroken;
import io.wifi.starrailexpress.game.GameConstants;
import io.wifi.starrailexpress.game.GameUtils;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoneyBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Item;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.init.ModRoles;

public final class PigEvents {

    private PigEvents() {
    }

    public static void register() {
        // 护甲可被穿透的死亡原因：猪猪掉出列车和AFK死亡不保护
        ArmorRules.canStickArmor.add((deathInfo -> {
            String path = deathInfo.deathReason().getPath();
            return path.equals(GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.getPath())
                    || path.equals(GameConstants.DeathReasons.DEATH_AFK.getPath());
        }));

        // 护盾被击破时掉落5个猪肉
        OnShieldBroken.EVENT.register((victim, killer) -> {
            if (!isPig(victim))
                return;
            if (!(victim instanceof ServerPlayer))
                return;
            for (int i = 0; i < 5; i++) {
                ItemEntity porkEntity = new ItemEntity(victim.level(),
                        victim.getX(), victim.getY() + 0.5, victim.getZ(),
                        new ItemStack(Items.PORKCHOP));
                porkEntity.setPickUpDelay(10);
                victim.level().addFreshEntity(porkEntity);
            }
            PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(victim);
            if (comp != null) {
                comp.sync();
            }
        });

        // 猪猪真正死亡时全服播放杀猪嚎叫
        OnPlayerDeath.EVENT.register((victim, deathReason) -> {
            if (!isPig(victim))
                return;
            if (victim.level().isClientSide())
                return;
            victim.level().players().forEach(p -> {
                p.playNotifySound(SoundEvents.PIG_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
            });
        });

        // 护盾拦截所有死因（包括无 killer 的巫毒/毒药等），排除掉落列车和AFK
        AllowPlayerDeath.EVENT.register((player, deathReason) -> {
            if (!isPig(player) || player.level().isClientSide() || deathReason == null)
                return true;

            SREArmorPlayerComponent armor = SREArmorPlayerComponent.KEY.getNullable(player);
            if (armor == null || armor.getArmor() <= 0)
                return true;

            String path = deathReason.getPath();
            if (path == null)
                return true;

            if (path.equals(GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.getPath())
                    || path.equals(GameConstants.DeathReasons.DEATH_AFK.getPath())) {
                return true;
            }

            armor.removeArmor();

            for (int i = 0; i < 5; i++) {
                ItemEntity porkEntity = new ItemEntity(player.level(),
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        new ItemStack(Items.PORKCHOP));
                porkEntity.setPickUpDelay(10);
                player.level().addFreshEntity(porkEntity);
            }
            PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(player);
            if (comp != null)
                comp.sync();

            return false;
        });

        // 阻止猪猪使用任何物品
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!isPig(player))
                return InteractionResultHolder.pass(ItemStack.EMPTY);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        });

        // 其他玩家右键喂食猪猪
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer feeder))
                return InteractionResult.PASS;
            if (!(entity instanceof Player pigPlayer))
                return InteractionResult.PASS;
            if (!isPig(pigPlayer))
                return InteractionResult.PASS;
            if (feeder == pigPlayer)
                return InteractionResult.PASS;
            if (!GameUtils.isPlayerAliveAndSurvival(pigPlayer))
                return InteractionResult.PASS;
            if (!GameUtils.isPlayerAliveAndSurvival(feeder))
                return InteractionResult.PASS;
            if (!GameUtils.isGameRunning(feeder))
                return InteractionResult.PASS;

            PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(pigPlayer);
            if (comp == null)
                return InteractionResult.PASS;

            if (!comp.canFeed(feeder.getUUID()))
                return InteractionResult.PASS;

            ItemStack stack = feeder.getItemInHand(hand);

            boolean isFood = stack.get(DataComponents.FOOD) != null;
            boolean isDrink = stack.getItem() instanceof PotionItem
                    || stack.getItem() instanceof HoneyBottleItem
                    || stack.getItem() instanceof CocktailItem;

            if (!isFood && !isDrink)
                return InteractionResult.PASS;

            SREPlayerTaskComponent taskComp = SREPlayerTaskComponent.KEY.getNullable(pigPlayer);
            if (taskComp == null)
                return InteractionResult.PASS;

            if (taskComp.tasks.isEmpty())
                return InteractionResult.PASS;

            boolean taskCompleted = false;

            if (isFood) {
                SREPlayerTaskComponent.TrainTask eatTask = taskComp.tasks.get(SREPlayerTaskComponent.Task.EAT);
                if (eatTask instanceof SREPlayerTaskComponent.EatTask eat && !eat.fulfilled) {
                    eat.fulfilled = true;
                    taskCompleted = true;
                }
            }
            if (isDrink) {
                SREPlayerTaskComponent.TrainTask drinkTask = taskComp.tasks.get(SREPlayerTaskComponent.Task.DRINK);
                if (drinkTask instanceof SREPlayerTaskComponent.DrinkTask drink && !drink.fulfilled) {
                    drink.fulfilled = true;
                    taskCompleted = true;
                }
            }

            if (!taskCompleted)
                return InteractionResult.PASS;

            comp.setFeedCooldown(feeder.getUUID());

            if (!feeder.isCreative()) {
                stack.shrink(1);
            }

            SREPlayerShopComponent feederShop = SREPlayerShopComponent.KEY.getNullable(feeder);
            if (feederShop != null) {
                feederShop.addToBalance(5);
            }

            comp.onFeedComplete();

            comp.sync();
            taskComp.sync();

            feeder.playNotifySound(SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, 1.0F);

            return InteractionResult.CONSUME;
        });
    }

    private static boolean isPig(Player player) {
        if (player == null)
            return false;
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.getNullable(player.level());
        return gameWorld != null && gameWorld.isRole(player, ModRoles.PIG);
    }
}