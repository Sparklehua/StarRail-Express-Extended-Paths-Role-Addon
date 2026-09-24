package org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter;

import io.wifi.starrailexpress.DeathInfo;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.event.AllowPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.OnGameEnd;
import io.wifi.starrailexpress.event.OnPlayerDeath;
import io.wifi.starrailexpress.event.OnPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.OnShieldBroken;
import io.wifi.starrailexpress.game.GameUtils;
import io.wifi.starrailexpress.index.TMMItems;
import io.wifi.starrailexpress.rules.ArmorRules;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.events.GameInitializeEvent;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.content.item.TargetRevolverItem;
import org.agmas.pathsrole.content.item.WantedPosterItem;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;

public class BountyHunterEvents {

    public static void registerEvents() {
        registerSkills();
        registerPosterInteractions();
        registerShieldBrokenEvents();
        registerTargetRevolverEvents();
        registerDeathEvents();
        registerLifecycleEvents();
    }

    private static void registerSkills() {
    }

    private static void registerShieldBrokenEvents() {
        OnShieldBroken.EVENT.register((victim, killer) -> {
            if (victim == null || killer == null || victim.level() == null) {
                return;
            }
            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(victim.level());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }
            if (!gameWorld.isRole(victim, ModRoles.BOUNTY_HUNTER)) {
                return;
            }
            if (!GameUtils.isPlayerAliveAndSurvival(victim)) {
                return;
            }
            BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(victim);
            if (comp == null) {
                return;
            }
            comp.setForcedTarget(killer.getUUID(), killer.getGameProfile().getName());
            victim.displayClientMessage(
                Component.translatable("message.pathsrole.bounty_hunter.shield_broken_forced_target", killer.getGameProfile().getName()).withStyle(ChatFormatting.RED),
                true);
        });
    }

    private static void registerPosterInteractions() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            return InteractionResult.PASS;
        });
    }

    private static void registerTargetRevolverEvents() {
        AllowPlayerDeathWithKiller.EVENT.register((player, killer, deathReason) -> {
            if (killer == null) {
                return true;
            }
            if (!killer.getMainHandItem().is(ModItems.TARGET_REVOLVER)) {
                return true;
            }
            UUID hunterUUID = TargetRevolverItem.getHunterUUID(killer.getMainHandItem());
            if (hunterUUID == null) {
                return false;
            }
            if (!hunterUUID.equals(player.getUUID())) {
                return false;
            }
            return true;
        });

        ArmorRules.canStickArmor.add((deathInfo) -> {
            try {
                if (deathInfo == null || deathInfo.killer() == null) {
                    return false;
                }
                if (ModRoles.BOUNTY_HUNTER == null) {
                    return false;
                }
                SREGameWorldComponent game = SREGameWorldComponent.KEY.get(deathInfo.killer().level());
                if (game != null && game.isRunning() && game.isRole(deathInfo.killer(), ModRoles.BOUNTY_HUNTER)) {
                    return false;
                }
            } catch (Exception ignored) {
            }
            return false;
        });
    }

    private static void registerDeathEvents() {
        AllowPlayerDeathWithKiller.EVENT.register((player, killer, deathReason) -> {
            if (killer == null) {
                return true;
            }
            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return true;
            }
            if (!gameWorld.isRole(killer, ModRoles.BOUNTY_HUNTER)) {
                return true;
            }
            BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(killer);
            if (comp == null) {
                return true;
            }
            if (!comp.isHunting()) {
                return false;
            }
            if (!comp.isTarget(player)) {
                return false;
            }
            return true;
        });

        OnPlayerDeathWithKiller.EVENT.register((player, killer, deathReason) -> {
            if (player == null || player.level() == null) {
                return;
            }
            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }
            if (killer != null && gameWorld.isRole(killer, ModRoles.BOUNTY_HUNTER)) {
                BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(killer);
                if (comp != null && comp.isHunting()) {
                    if (comp.isMainTarget(player)) {
                        comp.startCooldown();
                    } else if (comp.isForcedTarget(player)) {
                        comp.clearForcedTarget();
                    }
                }
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                for (ServerPlayer sp : serverLevel.players()) {
                    if (sp == null) {
                        continue;
                    }
                    BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(sp);
                    if (comp == null) {
                        continue;
                    }
                    UUID targetUUID = comp.getTargetUUID();
                    if (targetUUID != null && targetUUID.equals(player.getUUID())) {
                        if (killer == null || !killer.getUUID().equals(sp.getUUID())) {
                            comp.clearTarget();
                        }
                    }
                    UUID forcedUUID = comp.getForcedTargetUUID();
                    if (forcedUUID != null && forcedUUID.equals(player.getUUID())) {
                        if (killer == null || !killer.getUUID().equals(sp.getUUID())) {
                            comp.clearForcedTarget();
                        }
                    }
                }
            }
            if (gameWorld.isRole(player, ModRoles.BOUNTY_HUNTER)) {
                BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(player);
                if (comp != null) {
                    comp.clearTarget();
                    comp.clearForcedTarget();
                }
            }
        });

        OnPlayerDeath.EVENT.register((player, deathReason) -> {
            if (player == null || player.level() == null) {
                return;
            }
            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }
            if (gameWorld.isRole(player, ModRoles.BOUNTY_HUNTER)) {
                BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(player);
                if (comp != null) {
                    comp.clearTarget();
                    comp.clearForcedTarget();
                }
                if (player.level() instanceof ServerLevel serverLevel) {
                    for (ServerPlayer sp : serverLevel.players()) {
                        if (sp == null || sp.getInventory() == null) {
                            continue;
                        }
                        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                            ItemStack stack = sp.getInventory().getItem(i);
                            if (stack.is(ModItems.TARGET_REVOLVER)) {
                                UUID hunterUUID = TargetRevolverItem.getHunterUUID(stack);
                                if (hunterUUID != null && hunterUUID.equals(player.getUUID())) {
                                    sp.getInventory().setItem(i, ItemStack.EMPTY);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private static void registerLifecycleEvents() {
        ModdedRoleAssigned.EVENT.register((player, role) -> {
            if (role.identifier().equals(ModRoles.BOUNTY_HUNTER_ID)) {
                player.addItem(TMMItems.REVOLVER.getDefaultInstance());
                SREPlayerShopComponent.KEY.get(player).setBalance(100);
            }
        });

        GameInitializeEvent.EVENT.register((level, gameWorldComponent, readyPlayerList) -> {
            for (ServerPlayer sp : level.players()) {
                BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(sp);
                if (comp == null) {
                    continue;
                }
                comp.init();
            }
        });

        ResetPlayerEvent.EVENT.register(player -> {
            BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(player);
            if (comp != null) {
                comp.clear();
            }
        });

        OnGameEnd.EVENT.register((level, gameWorldComponent) -> {
            for (ServerPlayer sp : level.players()) {
                BountyHunterPlayerComponent comp = PathsroleComponents.getBountyHunterComponent(sp);
                if (comp == null) {
                    continue;
                }
                comp.init();
            }
        });
    }
}