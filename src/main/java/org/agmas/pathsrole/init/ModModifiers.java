package org.agmas.pathsrole.init;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.event.OnPlayerDeathWithKiller;
import io.wifi.starrailexpress.game.GameUtils;
import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.components.Button;
import net.minecraft.ChatFormatting;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.modifiers.HMLModifiers;
import org.agmas.harpymodloader.modifiers.SREModifier;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.modifier.aha_blessing.AhaBlessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModModifiers {
    private static final Logger LOGGER = LoggerFactory.getLogger((String)"Pathsrole-Modifiers");
    public static SREModifier REIMU_CURSE;
    public static SREModifier SHIPPER_MARK;
    public static SREModifier AHA_BLESSING;
    public static final Map<UUID, Long> LAST_COIN_DEDUCT_TIME;
    public static final Set<UUID> BLINDNESS_APPLIED_PLAYERS = new HashSet<>();

    public static void init() {
        REIMU_CURSE = HMLModifiers.registerModifier((SREModifier)new SREModifier(PathsRoleMod.id("reimu_curse"), new Color(139, 0, 0).getRGB(), (Collection)null, (Collection)null, false, true)).setDefaultEnableChance(0);
        SHIPPER_MARK = HMLModifiers.registerModifier((SREModifier)new SREModifier(PathsRoleMod.id("shipper_mark"), new Color(255, 105, 180).getRGB(), (Collection)null, (Collection)null, false, false)).setDefaultEnableChance(0);
        AHA_BLESSING = HMLModifiers.registerModifier((SREModifier)new SREModifier(PathsRoleMod.id("aha_blessing"), new Color(255, 215, 0).getRGB(), (Collection)null, (Collection)null, false, false)).setDefaultEnableChance(0).setHidden(false);
        LOGGER.info("Registered modifier: {}", (Object)REIMU_CURSE.identifier());
        AhaBlessingHandler.init();
        OnPlayerDeathWithKiller.EVENT.register((victim, killer, deathReason) -> {
            if (killer == null) return;
            WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)victim.level());
            if (wmc == null) return;
            if (!wmc.isModifier(victim.getUUID(), AHA_BLESSING)) return;
            wmc.removeModifier(victim.getUUID(), AHA_BLESSING);
            wmc.addModifier(killer.getUUID(), AHA_BLESSING);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                WorldModifierComponent modifiers;
                SREGameWorldComponent gameWorld;
                if (!GameUtils.isPlayerAliveAndSurvival((Player)player) || (gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)player.serverLevel())) == null || !gameWorld.isRunning() || (modifiers = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)player.serverLevel())) == null || !modifiers.isModifier(player.getUUID(), REIMU_CURSE)) continue;
                player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 999999, 2, false, false, true));
                if (!BLINDNESS_APPLIED_PLAYERS.contains(player.getUUID())) {
                    player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.UNLUCK, 200, 0, false, false, true));
                    BLINDNESS_APPLIED_PLAYERS.add(player.getUUID());
                }
                Long lastTime = LAST_COIN_DEDUCT_TIME.get(player.getUUID());
                long now = System.currentTimeMillis();
                if (lastTime != null && now - lastTime < 60000L) continue;
                SREPlayerShopComponent shop = (SREPlayerShopComponent)SREPlayerShopComponent.KEY.get((Object)player);
                int newBalance = Math.max(0, shop.balance - 10);
                shop.setBalance(newBalance);
                shop.sync();
                LAST_COIN_DEDUCT_TIME.put(player.getUUID(), now);
                player.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.reimu_curse.coin_deduct").withStyle(ChatFormatting.RED), true);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)server.overworld());
            if (gameWorld == null || gameWorld.isRunning()) return;
            WorldModifierComponent modifiers = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)server.overworld());
            if (modifiers == null) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!modifiers.isModifier(player.getUUID(), REIMU_CURSE)) continue;
                modifiers.removeModifier(player.getUUID(), REIMU_CURSE);
                player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(net.minecraft.world.effect.MobEffects.UNLUCK);
                LAST_COIN_DEDUCT_TIME.remove(player.getUUID());
                BLINDNESS_APPLIED_PLAYERS.remove(player.getUUID());
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!modifiers.isModifier(player.getUUID(), AHA_BLESSING)) continue;
                modifiers.removeModifier(player.getUUID(), AHA_BLESSING);
                var scaleAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
                if (scaleAttr != null) {
                    scaleAttr.removeModifier(AhaBlessingHandler.SHRINK_MODIFIER_ID);
                }
            }
        });
    }

    static {
        LAST_COIN_DEDUCT_TIME = new HashMap<UUID, Long>();
    }
}