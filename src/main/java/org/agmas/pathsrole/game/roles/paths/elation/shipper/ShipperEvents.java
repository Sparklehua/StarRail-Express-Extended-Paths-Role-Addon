package org.agmas.pathsrole.game.roles.paths.elation.shipper;

import io.wifi.starrailexpress.SRE;
import io.wifi.starrailexpress.api.RoleSkill;
import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.api.replay.GameReplayUtils;
import io.wifi.starrailexpress.cca.SREArmorPlayerComponent;
import io.wifi.starrailexpress.cca.SREGameRoundEndComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerSkinsComponent;
import io.wifi.starrailexpress.data.PlayerEconomyManager;
import io.wifi.starrailexpress.event.AllowGameEnd;
import io.wifi.starrailexpress.event.AllowPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.AllowShootRevolverDrop;
import io.wifi.starrailexpress.event.OnGameEnd;
import io.wifi.starrailexpress.event.OnPlayerDeath;
import io.wifi.starrailexpress.event.OnPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.OnRevolverUsed;
import io.wifi.starrailexpress.event.OnShieldBroken;
import io.wifi.starrailexpress.game.GameConstants;
import io.wifi.starrailexpress.game.GameUtils;
import io.wifi.starrailexpress.index.TMMItems;
import io.wifi.starrailexpress.stats.PlayerStatsManager;
import io.wifi.starrailexpress.util.TrueFalseResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ItemLike;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.events.GameInitializeEvent;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.harpymodloader.events.ModifierAssigned;
import org.agmas.harpymodloader.events.ModifierRemoved;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.harpymodloader.modifiers.SREModifier;
import org.agmas.noellesroles.init.ModEffects;
import org.agmas.noellesroles.utils.RoleUtils;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModModifiers;
import org.agmas.pathsrole.init.ModRoles;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.modifier.lovers.cca.LoversComponent;

public class ShipperEvents {
    public static final Map<UUID, Long> betrayalEndTimes = new HashMap<UUID, Long>();

    public static void registerEvents() {
        ShipperEvents.registerSkills();
        GameUtils.CustomWinnersPredicates.add(entry -> {
            if (!ModRoles.SHIPPER.identifier().getPath().equals(entry.getValue())) {
                return false;
            }
            Player player = (Player)entry.getKey();
            SREGameRoundEndComponent roundEnd = (SREGameRoundEndComponent)SREGameRoundEndComponent.KEY.get((Object)player.level());
            if (roundEnd == null) {
                return false;
            }
            return roundEnd.CustomWinnerPlayers.contains(player.getUUID());
        });
        ModdedRoleAssigned.EVENT.register((player, role) -> {
            if (role.identifier().equals((Object)ModRoles.SHIPPER.identifier())) {
                player.addItem(ModItems.SHIPPER_BOOK.getDefaultInstance());
            }
        });
        GameInitializeEvent.EVENT.register((level, gameWorldComponent, readyPlayerList) -> {
            boolean hasShipper = false;
            for (ServerPlayer sp : level.players()) {
                if (!gameWorldComponent.isRole((Player)sp, ModRoles.SHIPPER)) continue;
                hasShipper = true;
                break;
            }
            if (hasShipper) {
                Harpymodloader.MODIFIER_MAX.put(StupidExpress.id((String)"lovers"), 0);
            }
            for (ServerPlayer sp : level.players()) {
                ShipperPlayerComponent comp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)sp);
                if (comp == null) continue;
                comp.init();
            }
        });
        ModifierAssigned.EVENT.register((player, modifier) -> {
            if (!modifier.equals(SEModifiers.LOVERS)) {
                return;
            }
            if (player instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)player;
                SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)sp.level());
                if (gameWorld == null || !gameWorld.isRunning()) {
                    return;
                }
                for (ServerPlayer p : sp.serverLevel().players()) {
                    if (gameWorld.isRole((Player)p, ModRoles.SHIPPER)) {
                        WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)sp.level());
                        if (wmc != null) {
                            wmc.removeModifier(sp.getUUID(), SEModifiers.LOVERS);
                        }
                        return;
                    }
                }
            }
        });
        OnGameEnd.EVENT.register((level, gameWorldComponent) -> {
            int loversWinCount = 0;
            for (ServerPlayer sp : level.players()) {
                if (sp == null) continue;
                try {
                    ShipperPlayerComponent comp = (ShipperPlayerComponent) ShipperPlayerComponent.KEY.get((Object) sp);
                    if (comp == null) continue;
                    comp.init();
                } catch (Exception e) {
                    PathsRoleMod.LOGGER.error("[ShipperEvents] OnGameEnd 重置组件时出错", e);
                }
            }
            // 嗑学家印记CP获胜时计入恋人胜利次数
            try {
                SREGameRoundEndComponent roundEnd = (SREGameRoundEndComponent) SREGameRoundEndComponent.KEY.get((Object) level);
                WorldModifierComponent wmc = (WorldModifierComponent) WorldModifierComponent.KEY.get((Object) level);
                if (roundEnd == null || wmc == null) return;
                if (roundEnd.CustomWinnerPlayers == null || roundEnd.CustomWinnerPlayers.isEmpty()) return;
                if (!roundEnd.getWinStatus().equals(GameUtils.WinStatus.CUSTOM)) return;
                if (roundEnd.CustomWinnerID == null || !roundEnd.CustomWinnerID.equals(ModRoles.SHIPPER.identifier().getPath())) return;

                for (ServerPlayer sp : level.players()) {
                    if (sp == null) continue;
                    try {
                        if (!roundEnd.CustomWinnerPlayers.contains(sp.getUUID())) continue;
                        for (SREModifier modifier : wmc.getModifiers(sp.getUUID())) {
                            if (modifier != null && ModModifiers.SHIPPER_MARK != null
                                    && modifier.equals(ModModifiers.SHIPPER_MARK)) {
                                PlayerStatsManager.get(sp).incrementTotalLoversWins();
                                loversWinCount++;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        PathsRoleMod.LOGGER.error("[ShipperEvents] 处理嗑学家CP恋人胜利统计时出错: {}", sp.getName().getString(), e);
                    }
                }
            } catch (Exception e) {
                PathsRoleMod.LOGGER.error("[ShipperEvents] 嗑学家CP恋人胜利统计初始化时出错", e);
            }
        });
        ModifierRemoved.EVENT.register((player, modifier) -> {
            if (modifier != null && modifier.equals(ModModifiers.SHIPPER_MARK) && player instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)player;
                if (sp.isSpectator() || sp.getHealth() <= 0.0f) {
                    return;
                }
                SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)sp.level());
                if (gameWorld == null || !gameWorld.isRunning()) {
                    return;
                }
                ShipperPlayerComponent shipperComp = ShipperEvents.findShipperComponent(sp);
                if (shipperComp != null && shipperComp.getPairedLovers().contains(sp.getUUID()) && !shipperComp.isRageActive()) {
                    WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)sp.level());
                    if (wmc != null) {
                        wmc.addModifier(sp.getUUID(), ModModifiers.SHIPPER_MARK, false);
                        wmc.sync();
                    }
                }
            }
        });
        ResetPlayerEvent.EVENT.register(player -> {
            ShipperPlayerComponent comp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)player);
            if (comp != null) {
                comp.init();
            }
        });
        OnPlayerDeathWithKiller.EVENT.register((player, killer, deathReason) -> {
            if (!(player instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer deadPlayer = (ServerPlayer)player;
            if (killer == null) {
                return;
            }
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)deadPlayer.level());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }
            WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)deadPlayer.level());
            if (wmc == null || !wmc.isModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK)) {
                return;
            }
            ShipperPlayerComponent shipperComp = ShipperEvents.findShipperComponent(deadPlayer);
            if (shipperComp == null) {
                return;
            }
            ServerPlayer shipperPlayer = (ServerPlayer)shipperComp.getPlayer();
            if (!GameUtils.isPlayerAliveAndSurvival((Player)shipperPlayer)) {
                return;
            }
            if (killer.getUUID().equals(shipperPlayer.getUUID())) {
                return;
            }
            if (!GameUtils.isPlayerAliveAndSurvival((Player)killer)) {
                return;
            }
            if (shipperComp.isRageActive()) {
                return;
            }
            shipperComp.setRageKiller(killer.getUUID(), ShipperEvents.getSafeName(killer), deadPlayer.getUUID());
            shipperComp.setRePairCooldownEnd(0L);
            shipperComp.addAbsorbedPlayer(deadPlayer.getUUID(), ShipperEvents.getSafeName(deadPlayer));
            killer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 800, 0, false, false, true));
            if (killer instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)killer;
                sp.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.killed_shipper_marked").withStyle(ChatFormatting.RED), true);
            }
        });
        OnShieldBroken.EVENT.register((victim, killer) -> {
            if (!(victim instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer sp = (ServerPlayer)victim;
            WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)sp.level());
            if (wmc == null) {
                return;
            }
            if (wmc.isModifier(sp.getUUID(), ModModifiers.SHIPPER_MARK)) {
                sp.displayClientMessage(Component.translatable("message.pathsrole.shipper.shield_broken").withStyle(ChatFormatting.RED), true);
            }
        });
        OnPlayerDeath.EVENT.register((player, deathReason) -> {
            if (!(player instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer deadPlayer = (ServerPlayer)player;
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)deadPlayer.level());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return;
            }
            ShipperEvents.handleShipperMomentLinkedDeath(deadPlayer, gameWorld);
            if (gameWorld.isRole((Player)deadPlayer, ModRoles.SHIPPER)) {
                ShipperEvents.handleShipperDeath(deadPlayer);
            }
            ShipperEvents.handleLoverDeath(deadPlayer);
        });
        AllowPlayerDeathWithKiller.EVENT.register((victim, killer, deathReason) -> {
            ServerPlayer shipperPlayer;
            if (killer == null) {
                if (!(victim instanceof ServerPlayer)) {
                    return true;
                }
                ServerPlayer deadPlayer = (ServerPlayer)victim;
                SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)deadPlayer.level());
                if (gameWorld == null || !gameWorld.isRunning()) {
                    return true;
                }
                if (deathReason.equals(GameConstants.DeathReasons.GRENADE)) {
                    if (gameWorld.isRole(deadPlayer, ModRoles.SHIPPER)) {
                        return false;
                    }
                    ShipperPlayerComponent shipperComp = ShipperEvents.findShipperComponent(deadPlayer);
                    if (shipperComp != null) {
                        WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)deadPlayer.level());
                        if (wmc != null && wmc.isModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK) && shipperComp.getPairedLovers().contains(deadPlayer.getUUID())) {
                            return false;
                        }
                    }
                }
                return true;
            }
            if (!(victim instanceof ServerPlayer)) {
                return true;
            }
            ServerPlayer deadPlayer = (ServerPlayer)victim;
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)deadPlayer.level());
            if (gameWorld == null || !gameWorld.isRunning()) {
                return true;
            }
            WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)deadPlayer.level());
            ShipperPlayerComponent shipperForLoverCheck = ShipperEvents.findShipperComponent(deadPlayer);
            if (shipperForLoverCheck == null && killer instanceof ServerPlayer) {
                ServerPlayer killerServer = (ServerPlayer)killer;
                shipperForLoverCheck = ShipperEvents.findShipperComponent(killerServer);
            }
            if (shipperForLoverCheck != null && shipperForLoverCheck.isShipperMomentActive()) {
                boolean killerIsLover;
                boolean victimIsLover = wmc != null && wmc.isModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK) && shipperForLoverCheck.getPairedLovers().contains(deadPlayer.getUUID());
                boolean bl = killerIsLover = wmc != null && wmc.isModifier(killer.getUUID(), ModModifiers.SHIPPER_MARK) && shipperForLoverCheck.getPairedLovers().contains(killer.getUUID());
                if (victimIsLover && killerIsLover) {
                    shipperForLoverCheck.setPendingBetrayalKiller(killer.getUUID());
                }
            }
            if (shipperForLoverCheck != null && shipperForLoverCheck.isShipperMomentActive() && shipperForLoverCheck.getPairedLovers().contains(deadPlayer.getUUID()) && shipperForLoverCheck.getPairedLovers().contains(killer.getUUID())) {
                return false;
            }
            if (wmc != null && wmc.isModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK) && wmc.isModifier(killer.getUUID(), ModModifiers.SHIPPER_MARK)) {
                wmc.removeModifier(killer.getUUID(), ModModifiers.SHIPPER_MARK, false);
                wmc.removeModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK, false);
                if (shipperForLoverCheck != null) {
                    if (!shipperForLoverCheck.isShipperMomentActive()) {
                        shipperForLoverCheck.clearPairedLovers();
                    }
                    if (!shipperForLoverCheck.isRageActive()) {
                        shipperForLoverCheck.setRageKiller(killer.getUUID(), ShipperEvents.getSafeName(killer), deadPlayer.getUUID());
                        shipperForLoverCheck.setRePairCooldownEnd(0L);
                        shipperForLoverCheck.addAbsorbedPlayer(deadPlayer.getUUID(), ShipperEvents.getSafeName(deadPlayer));
                    }
                }
                if (killer instanceof ServerPlayer) {
                    ServerPlayer sp = (ServerPlayer)killer;
                    int victimCoins = 0;
                    SREPlayerSkinsComponent victimSkins = (SREPlayerSkinsComponent)SREPlayerSkinsComponent.KEY.get((Object)deadPlayer);
                    if (victimSkins != null) {
                        victimCoins = victimSkins.getCoinNum();
                    }
                    if (victimCoins > 0) {
                        victimSkins.addCoinNum(Integer.valueOf(-victimCoins));
                        PlayerEconomyManager.addCoinNum((Player)sp, (int)victimCoins);
                    }
                    for (int i = 0; i < deadPlayer.getInventory().getContainerSize(); ++i) {
                        ItemStack stack = deadPlayer.getInventory().getItem(i);
                        if (stack.isEmpty() || stack.is(TMMItems.LETTER) || stack.is(org.agmas.noellesroles.init.ModItems.COURIER_MAIL) || stack.is(org.agmas.noellesroles.init.ModItems.RECEIVED_MAIL)) continue;
                        deadPlayer.getInventory().setItem(i, ItemStack.EMPTY);
                        if (sp.getInventory().add(stack)) continue;
                        sp.drop(stack, false);
                    }
                    sp.containerMenu.broadcastChanges();
                    sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 800, 0, false, false, true));
                }
                return true;
            }
            if (shipperForLoverCheck != null) {
                boolean isKillerRageTarget;
                shipperPlayer = (ServerPlayer)shipperForLoverCheck.getPlayer();
                boolean bl = isKillerRageTarget = shipperForLoverCheck.isRageActive() && killer.getUUID().equals(shipperPlayer.getUUID()) && deadPlayer.getUUID().equals(shipperForLoverCheck.getRageKiller());
                if (!isKillerRageTarget) {
                    boolean victimIsLover = wmc != null && wmc.isModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK) && shipperForLoverCheck.getPairedLovers().contains(deadPlayer.getUUID());
                    boolean killerIsLover = wmc != null && wmc.isModifier(killer.getUUID(), ModModifiers.SHIPPER_MARK) && shipperForLoverCheck.getPairedLovers().contains(killer.getUUID());
                    boolean victimIsShipper = gameWorld.isRole((Player)deadPlayer, ModRoles.SHIPPER);
                    boolean killerIsShipper = gameWorld.isRole(killer, ModRoles.SHIPPER);
                    if (victimIsLover && killerIsShipper || victimIsShipper && killerIsLover) {
                        return false;
                    }
                }
            }
            if (wmc != null && wmc.isModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK) && shipperForLoverCheck != null && !shipperForLoverCheck.isRageActive() && GameUtils.isPlayerAliveAndSurvival((Player)(shipperPlayer = (ServerPlayer)shipperForLoverCheck.getPlayer())) && !killer.getUUID().equals(shipperPlayer.getUUID()) && GameUtils.isPlayerAliveAndSurvival((Player)killer)) {
                shipperForLoverCheck.setRageKiller(killer.getUUID(), ShipperEvents.getSafeName(killer), deadPlayer.getUUID());
                shipperForLoverCheck.setRePairCooldownEnd(0L);
                shipperForLoverCheck.addAbsorbedPlayer(deadPlayer.getUUID(), ShipperEvents.getSafeName(deadPlayer));
                killer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 800, 0, false, false, true));
                if (killer instanceof ServerPlayer) {
                    ServerPlayer sp = (ServerPlayer)killer;
                    sp.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.killed_shipper_marked").withStyle(ChatFormatting.RED), true);
                }
            }
            return true;
        });
        UseEntityCallback.EVENT.register((player, level, hand, target, hitResult) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer)) {
                return InteractionResult.PASS;
            }
            ServerPlayer shipper = (ServerPlayer)player;
            if (!(target instanceof ServerPlayer)) {
                return InteractionResult.PASS;
            }
            ServerPlayer targetPlayer = (ServerPlayer)target;
            ItemStack mainHand = shipper.getMainHandItem();
            boolean isShipperBook = mainHand.is((Item)ModItems.SHIPPER_BOOK);
            boolean isRageShipper = mainHand.is((Item)ModItems.RAGE_SHIPPER);
            if (!isShipperBook && !isRageShipper) {
                return InteractionResult.PASS;
            }
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)level);
            if (gameWorld == null || !gameWorld.isRunning()) {
                return InteractionResult.PASS;
            }
            if (!gameWorld.isRole((Player)shipper, ModRoles.SHIPPER)) {
                return InteractionResult.PASS;
            }
            if (shipper.isSpectator() || !shipper.isAlive()) {
                return InteractionResult.PASS;
            }
            ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)shipper);
            if (isRageShipper && shipperComp != null && (shipperComp.isRageActive() || shipperComp.isShipperMomentActive())) {
                if (targetPlayer.getUUID().equals(shipper.getUUID())) {
                    return InteractionResult.FAIL;
                }
                WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)level);
                if (wmc != null && wmc.isModifier(targetPlayer.getUUID(), ModModifiers.SHIPPER_MARK)) {
                    shipper.displayClientMessage(
                        Component.translatable("message.pathsrole.shipper.rage_shipper_cannot_absorb_cp").withStyle(ChatFormatting.RED),
                        true);
                    return InteractionResult.FAIL;
                }
                if (shipper.getCooldowns().isOnCooldown(ModItems.RAGE_SHIPPER)) {
                    shipper.displayClientMessage(
                        Component.translatable("message.pathsrole.shipper.rage_shipper_cooldown").withStyle(ChatFormatting.RED),
                        true);
                    return InteractionResult.FAIL;
                }
                shipperComp.addAbsorbedPlayer(targetPlayer.getUUID(), ShipperEvents.getSafeName(targetPlayer));
                GameUtils.killPlayer((Player)targetPlayer, true, (Player)shipper, PathsRoleMod.id("absorbed_by_book"));
                shipper.getCooldowns().addCooldown(ModItems.RAGE_SHIPPER, 200);
                shipper.getCooldowns().addCooldown((Item)TMMItems.REVOLVER, 200);
                // 磕学时刻中每吸入1人提升1级速度（最高速度3），吸入2人获得阿哈祝福护盾
                if (shipperComp.isShipperMomentActive()) {
                    shipperComp.incrementShipperMomentAbsorbedCount();
                    int absorbedCount = shipperComp.getShipperMomentAbsorbedCount();
                    int speedLevel = Math.min(absorbedCount - 1, 2);
                    if (speedLevel >= 0) {
                        shipper.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
                        shipper.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 999999, speedLevel, false, true, true));
                    }
                    if (absorbedCount >= 2 && !shipperComp.isShipperMomentArmorGiven()) {
                        if (wmc != null) {
                            wmc.addModifier(shipper.getUUID(), ModModifiers.AHA_BLESSING);
                        }
                        shipperComp.setShipperMomentArmorGiven(true);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (shipperComp != null && shipperComp.isRageActive() && shipperComp.getRageKiller() != null && shipperComp.getRageKiller().equals(targetPlayer.getUUID())) {
                if (shipperComp.isBookOnCooldown()) {
                    shipper.displayClientMessage(
                        Component.translatable("message.pathsrole.shipper.book_cooldown",
                            shipperComp.getBookCooldownRemainingSeconds()).withStyle(ChatFormatting.RED),
                        true);
                    return InteractionResult.FAIL;
                }
                ShipperEvents.handleRageAbsorption(shipper, targetPlayer, shipperComp);
                return InteractionResult.SUCCESS;
            }
            if (isRageShipper) {
                return InteractionResult.PASS;
            }
            if (shipper.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ShipperEvents.handleShipperBookUse(shipper, targetPlayer, gameWorld);
            return InteractionResult.FAIL;
        });
        AllowGameEnd.EVENT.register((serverWorld, winStatus, isLooseEndsMode) -> {
            if (isLooseEndsMode) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            SREGameWorldComponent gameWorldComponent = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)serverWorld);
            if (gameWorldComponent == null) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            List<? extends Player> remainingPlayers = serverWorld.getPlayers(p -> GameUtils.isPlayerAliveAndSurvival((Player)p) && !(ModRoles.REIMU != null && gameWorldComponent.isRole(p, ModRoles.REIMU)));
            boolean shipperAlive = false;
            ServerPlayer shipperPlayer = null;
            for (Player player : remainingPlayers) {
                if (!gameWorldComponent.isRole(player, ModRoles.SHIPPER)) continue;
                shipperAlive = true;
                shipperPlayer = (ServerPlayer)player;
                break;
            }
            if (!shipperAlive) {
                GameUtils.WinStatus deadLoversResult;
                ShipperPlayerComponent deadShipperComp = ShipperEvents.findDeadShipperComponent(serverWorld);
                if (deadShipperComp != null && deadShipperComp.hasLivingLovers() && (deadLoversResult = ShipperEvents.checkLoversWinWithShipper(serverWorld, null, deadShipperComp, remainingPlayers, winStatus)) != GameUtils.WinStatus.NOT_MODIFY) {
                    return deadLoversResult;
                }
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get(shipperPlayer);
            if (shipperComp == null) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            
            // ⬅️ 新增：如果磕学时刻未激活，立即检查是否应该激活（解决时序竞争问题）
            if (!shipperComp.isShipperMomentActive()) {
                ShipperEvents.checkShipperMomentTrigger(shipperPlayer, shipperComp);
            }
            
            if (shipperComp.isShipperMomentActive()) {
                return ShipperEvents.handleShipperMomentWinCheck(serverWorld, shipperPlayer, shipperComp, remainingPlayers, winStatus);
            }
            GameUtils.WinStatus loversResult = ShipperEvents.checkLoversWinWithShipper(serverWorld, shipperPlayer, shipperComp, remainingPlayers, winStatus);
            if (loversResult != GameUtils.WinStatus.NOT_MODIFY) {
                return loversResult;
            }
            return GameUtils.WinStatus.NOT_MODIFY;
        });
        AllowShootRevolverDrop.EVENT.register((player, target) -> {
            SREGameWorldComponent gameWorldComponent = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)player.level());
            if (gameWorldComponent != null && gameWorldComponent.isRunning() && gameWorldComponent.isRole(player, ModRoles.SHIPPER)) {
                return TrueFalseResult.FALSE;
            }
            return TrueFalseResult.PASS;
        });
        OnRevolverUsed.EVENT.register((player, target) -> {
            if (!(player instanceof ServerPlayer shipper)) return;
            SREGameWorldComponent gameWorldComponent = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)shipper.level());
            if (gameWorldComponent == null || !gameWorldComponent.isRunning() || !gameWorldComponent.isRole(shipper, ModRoles.SHIPPER)) {
                return;
            }
            ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)shipper);
            if (shipperComp == null || (!shipperComp.isRageActive() && !shipperComp.isShipperMomentActive())) {
                return;
            }
            shipper.getCooldowns().addCooldown(ModItems.RAGE_SHIPPER, 200);
        });
    }

    /**
     * 磕学家优先胜利判定。在 AllowGameEnd.EVENT 触发前调用，
     * 确保磕学家+CP的胜利条件优先于中立角色的独立胜利条件。
     */
    public static GameUtils.WinStatus checkShipperGameEnd(ServerLevel serverWorld, GameUtils.WinStatus winStatus, boolean isLooseEndsMode) {
        try {
            if (isLooseEndsMode) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            SREGameWorldComponent gameWorldComponent = (SREGameWorldComponent) SREGameWorldComponent.KEY.get(serverWorld);
            if (gameWorldComponent == null) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            List<? extends Player> remainingPlayers = serverWorld.getPlayers(p -> GameUtils.isPlayerAliveAndSurvival(p) && !(ModRoles.REIMU != null && gameWorldComponent.isRole(p, ModRoles.REIMU)));
            ServerPlayer shipperPlayer = null;
            for (Player player : remainingPlayers) {
                if (gameWorldComponent.isRole(player, ModRoles.SHIPPER)) {
                    shipperPlayer = (ServerPlayer) player;
                    break;
                }
            }
            if (shipperPlayer == null) {
                ShipperPlayerComponent deadShipperComp = ShipperEvents.findDeadShipperComponent(serverWorld);
                if (deadShipperComp != null && deadShipperComp.hasLivingLovers()) {
                    GameUtils.WinStatus deadLoversResult = ShipperEvents.checkLoversWinWithShipper(serverWorld, null, deadShipperComp, remainingPlayers, winStatus);
                    if (deadLoversResult != GameUtils.WinStatus.NOT_MODIFY) {
                        return deadLoversResult;
                    }
                }
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            ShipperPlayerComponent shipperComp = (ShipperPlayerComponent) ShipperPlayerComponent.KEY.get(shipperPlayer);
            if (shipperComp == null) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            if (!shipperComp.isShipperMomentActive()) {
                ShipperEvents.checkShipperMomentTrigger(shipperPlayer, shipperComp);
            }
            if (shipperComp.isShipperMomentActive()) {
                return ShipperEvents.handleShipperMomentWinCheck(serverWorld, shipperPlayer, shipperComp, remainingPlayers, winStatus);
            }
            GameUtils.WinStatus loversResult = ShipperEvents.checkLoversWinWithShipper(serverWorld, shipperPlayer, shipperComp, remainingPlayers, winStatus);
            if (loversResult != GameUtils.WinStatus.NOT_MODIFY) {
                return loversResult;
            }
            return GameUtils.WinStatus.NOT_MODIFY;
        } catch (Exception e) {
            PathsRoleMod.LOGGER.error("[Shipper checkShipperGameEnd] Error during win check, returning NOT_MODIFY to prevent crash", e);
            return GameUtils.WinStatus.NOT_MODIFY;
        }
    }

    private static void handleRageAbsorption(ServerPlayer shipper, ServerPlayer target, ShipperPlayerComponent shipperComp) {
        if (shipper == null || target == null || shipperComp == null) {
            return;
        }
        shipperComp.clearRage();
        shipperComp.clearPairedLovers();
        shipperComp.setObservationActive(true);
        shipperComp.setObservationCooldownEnd(System.currentTimeMillis() + 5000L);
        shipperComp.addAbsorbedPlayer(target.getUUID(), ShipperEvents.getSafeName(target));
        GameUtils.killPlayer((Player)target, (boolean)true, (Player)shipper, (ResourceLocation)PathsRoleMod.id("absorbed_by_book"));
        if (GameUtils.isPlayerAliveAndSurvival((Player)target)) {
            shipperComp.setBookCooldownEnd(System.currentTimeMillis() + 5000L);
            shipper.displayClientMessage(
                (Component)Component.translatable("message.pathsrole.shipper.absorb_blocked").withStyle(ChatFormatting.RED),
                true);
            target.displayClientMessage(
                (Component)Component.translatable("message.pathsrole.shipper.absorb_blocked_target").withStyle(ChatFormatting.GREEN),
                true);
        }
    }

    private static void handleShipperBookUse(ServerPlayer shipper, ServerPlayer target, SREGameWorldComponent gameWorld) {
        ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)shipper);
        if (shipperComp == null) {
            return;
        }
        if (shipper.level().getGameTime() == shipperComp.getLastInteractionTick()) {
            return;
        }
        shipperComp.updateLastInteractionTick();
        if (shipper.hasEffect(ModEffects.SAFE_TIME)) {
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.safe_time_blocked").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (target.equals((Object)shipper)) {
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.cannot_self").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!GameUtils.isPlayerAliveAndSurvivalIgnoreShitSplit((Player)target)) {
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.target_disconnected").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (ModRoles.REIMU != null && gameWorld.isRole(target, ModRoles.REIMU)) {
            shipper.displayClientMessage(Component.literal("§c对方是博丽灵梦，无法被磕CP。"), true);
            return;
        }
        WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)shipper.serverLevel());
        if (wmc == null) {
            return;
        }
        if (wmc.isModifier(target.getUUID(), ModModifiers.SHIPPER_MARK)) {
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.already_lover").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (wmc.isModifier(target.getUUID(), SEModifiers.LOVERS)) {
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.target_has_lovers").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (shipperComp.isInRePairCooldown()) {
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.repair_cooldown", (Object[])new Object[]{shipperComp.getRePairCooldownRemainingSeconds()}).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (shipperComp.isRageActive()) {
            shipper.displayClientMessage(Component.translatable("message.pathsrole.shipper.rage_clear_hint").withStyle(ChatFormatting.BOLD), true);
            return;
        }
        if (shipperComp.hasLivingLovers()) {
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.has_living_lovers").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (shipperComp.isObservationOnCooldown()) {
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.observation_cooldown", (Object[])new Object[]{shipperComp.getObservationCooldownRemainingSeconds()}).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (shipperComp.getFirstTarget() == null) {
            shipperComp.setFirstTarget(target.getUUID());
            shipper.displayClientMessage(Component.translatable("message.pathsrole.shipper.first_target", new Object[]{target.getName()}).withStyle(ChatFormatting.GOLD), true);
            SRE.REPLAY_MANAGER.recordCustomEvent((Component)Component.translatable((String)"replay.event.pathsrole.shipper.marked", (Object[])new Object[]{GameReplayUtils.getReplayPlayerDisplayText((Player)shipper, (boolean)true), GameReplayUtils.getReplayPlayerDisplayText((Player)target, (boolean)true)}));
            return;
        }
        if (shipperComp.getFirstTarget() != null && shipperComp.getFirstTarget().equals(target.getUUID())) {
            if (shipper.level().getGameTime() == shipperComp.getLastInteractionTick()) {
                return;
            }
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.same_player").withStyle(ChatFormatting.RED), true);
            return;
        }
        Player firstTargetPlayer = shipper.serverLevel().getPlayerByUUID(shipperComp.getFirstTarget());
        if (firstTargetPlayer == null || !GameUtils.isPlayerAliveAndSurvivalIgnoreShitSplit((Player)firstTargetPlayer)) {
            shipperComp.clearFirstTarget();
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.first_target_lost").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (wmc.isModifier(firstTargetPlayer.getUUID(), ModModifiers.SHIPPER_MARK)) {
            shipperComp.clearFirstTarget();
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.first_target_became_lover").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (wmc.isModifier(firstTargetPlayer.getUUID(), SEModifiers.LOVERS) || wmc.isModifier(target.getUUID(), SEModifiers.LOVERS)) {
            shipperComp.clearFirstTarget();
            shipper.displayClientMessage((Component)Component.translatable((String)"message.pathsrole.shipper.target_has_lovers").withStyle(ChatFormatting.RED), true);
            return;
        }
        ShipperEvents.bindLovers(shipper, (ServerPlayer)firstTargetPlayer, target, wmc, shipperComp);
    }

    private static void clearModifiersForPlayer(ServerPlayer player, WorldModifierComponent wmc) {
        HashSet<SREModifier> modifiers = new HashSet<SREModifier>(wmc.getModifiers(player.getUUID()));
        for (SREModifier modifier : modifiers) {
            LoversComponent loversComp;
            if (modifier.equals(SEModifiers.BLACK_WHITE) || modifier.equals(SEModifiers.REFUGEE)) continue;
            wmc.removeModifier(player.getUUID(), modifier, false);
            if (!modifier.equals(SEModifiers.LOVERS) || (loversComp = (LoversComponent)LoversComponent.KEY.get((Object)player)) == null) continue;
            loversComp.reset();
        }
    }

    private static void bindLovers(ServerPlayer shipper, ServerPlayer first, ServerPlayer second, WorldModifierComponent wmc, ShipperPlayerComponent shipperComp) {
        if (wmc == null || shipperComp == null) {
            return;
        }
        ShipperEvents.clearModifiersForPlayer(first, wmc);
        ShipperEvents.clearModifiersForPlayer(second, wmc);
        wmc.addModifier(first.getUUID(), ModModifiers.SHIPPER_MARK, false);
        wmc.addModifier(second.getUUID(), ModModifiers.SHIPPER_MARK, false);
        wmc.sync();
        String firstName = first.getGameProfile() != null ? first.getGameProfile().getName() : first.getName().getString();
        String secondName = second.getGameProfile() != null ? second.getGameProfile().getName() : second.getName().getString();
        shipperComp.addPairedLovers(first.getUUID(), second.getUUID(), firstName, secondName);
        shipper.displayClientMessage(Component.translatable("message.pathsrole.shipper.paired", new Object[]{first.getName(), second.getName()}).withStyle(ChatFormatting.GOLD), true);
        first.displayClientMessage(Component.translatable("message.pathsrole.shipper.became_lover_hud"), false);
        second.displayClientMessage(Component.translatable("message.pathsrole.shipper.became_lover_hud"), false);
        SRE.REPLAY_MANAGER.recordCustomEvent((Component)Component.translatable((String)"replay.event.pathsrole.shipper.paired", (Object[])new Object[]{GameReplayUtils.getReplayPlayerDisplayText((Player)shipper, (boolean)true), GameReplayUtils.getReplayPlayerDisplayText((Player)first, (boolean)true), GameReplayUtils.getReplayPlayerDisplayText((Player)second, (boolean)true)}));
    }

    private static void handleShipperDeath(ServerPlayer deadShipper) {
        ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)deadShipper);
        if (shipperComp == null) {
            return;
        }
        if (shipperComp.isShipperMomentActive()) {
            shipperComp.setShipperMomentActive(false);
            return;
        }
        for (UUID uuid : new ArrayList<UUID>(shipperComp.getPairedLovers())) {
            SREArmorPlayerComponent armor;
            ServerPlayer sp;
            Player lover = deadShipper.serverLevel().getPlayerByUUID(uuid);
            if (!(lover instanceof ServerPlayer) || !GameUtils.isPlayerAliveAndSurvival((Player)(sp = (ServerPlayer)lover)) || (armor = (SREArmorPlayerComponent)SREArmorPlayerComponent.KEY.get((Object)sp)) == null) continue;
            armor.addArmor();
        }
    }

    private static void handleLoverDeath(ServerPlayer deadPlayer) {
        SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)deadPlayer.level());
        if (gameWorld == null || !gameWorld.isRunning()) {
            return;
        }
        WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)deadPlayer.serverLevel());
        if (wmc == null) {
            return;
        }
        if (!wmc.isModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK)) {
            return;
        }
        ShipperPlayerComponent shipperComp = ShipperEvents.findShipperComponent(deadPlayer);
        if (shipperComp == null) {
            return;
        }
        Player shipper = shipperComp.getPlayer();
        if (shipper instanceof ServerPlayer sp && sp.connection != null) {
            sp.displayClientMessage(Component.literal("有人阻碍你的磕学，找出凶手，将他吸入书中").withStyle(ChatFormatting.RED), true);
        }
        if (shipperComp.isShipperMomentActive()) {
            shipperComp.setShipperMomentActive(false);
            shipperComp.clearPairedLovers();
            return;
        }
        if (!shipperComp.getPairedLovers().contains(deadPlayer.getUUID())) {
            wmc.removeModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK, false);
            return;
        }
        for (UUID uuid : new ArrayList<UUID>(shipperComp.getPairedLovers())) {
            ServerPlayer sp;
            Player lover;
            if (uuid.equals(deadPlayer.getUUID()) || !((lover = deadPlayer.serverLevel().getPlayerByUUID(uuid)) instanceof ServerPlayer) || !GameUtils.isPlayerAliveAndSurvival((Player)(sp = (ServerPlayer)lover))) continue;
            GameUtils.forceKillPlayer((Player)sp, (boolean)true, (Player)deadPlayer, (ResourceLocation)PathsRoleMod.id("broken_heart"));
        }
        shipperComp.clearPairedLovers();
        if (!shipperComp.isRageActive()) {
            shipperComp.setRePairCooldownEnd(System.currentTimeMillis() + 7000L);
        }
    }

    private static ShipperPlayerComponent findShipperComponent(ServerPlayer player) {
        for (ServerPlayer sp : player.serverLevel().players()) {
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)sp.level());
            if (gameWorld == null || !gameWorld.isRole((Player)sp, ModRoles.SHIPPER)) continue;
            return (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)sp);
        }
        return null;
    }

    private static ShipperPlayerComponent findDeadShipperComponent(ServerLevel serverWorld) {
        for (ServerPlayer sp : serverWorld.players()) {
            ShipperPlayerComponent comp;
            SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)sp.level());
            if (gameWorld == null || !gameWorld.isRole((Player)sp, ModRoles.SHIPPER) || (comp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)sp)) == null) continue;
            return comp;
        }
        return null;
    }

    private static GameUtils.WinStatus checkLoversWinWithShipper(ServerLevel serverWorld, ServerPlayer shipperPlayer, ShipperPlayerComponent shipperComp, List<? extends Player> remainingPlayers, GameUtils.WinStatus winStatus) {
        boolean bl = true;
        boolean hasMarkedLovers = shipperComp.hasLivingLovers();
        if (!hasMarkedLovers) {
            return GameUtils.WinStatus.NOT_MODIFY;
        }
        WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)serverWorld);
        boolean allAreShipperOrLovers = true;
        boolean hasLover = false;
        for (Player player : remainingPlayers) {
            if (player.equals((Object)shipperPlayer)) continue;
            if (wmc != null && wmc.isModifier(player.getUUID(), ModModifiers.SHIPPER_MARK)) {
                hasLover = true;
                continue;
            }
            allAreShipperOrLovers = false;
            break;
        }
        if (allAreShipperOrLovers && hasLover) {
            SREGameRoundEndComponent gameRoundEndComponent = (SREGameRoundEndComponent)SREGameRoundEndComponent.KEY.get((Object)serverWorld);
            if (gameRoundEndComponent == null) {
                PathsRoleMod.LOGGER.error("[Shipper checkLoversWin] gameRoundEndComponent is null, cannot declare custom win");
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            gameRoundEndComponent.CustomWinnerPlayers.clear();
            for (Player player : remainingPlayers) {
                gameRoundEndComponent.CustomWinnerPlayers.add(player.getUUID());
            }
            RoleUtils.customWinnerWin((ServerLevel)serverWorld, (GameUtils.WinStatus)GameUtils.WinStatus.CUSTOM, (String)ModRoles.SHIPPER.identifier().getPath(), (OptionalInt)OptionalInt.of(ModRoles.SHIPPER.color()));
            return GameUtils.WinStatus.CUSTOM;
        }
        SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)serverWorld);
        boolean bl2 = true;
        for (Player player : remainingPlayers) {
            if (gameWorld == null || !gameWorld.isKillerTeam((Player)player)) continue;
            bl = false;
            break;
        }
        if (bl) {
            ShipperEvents.checkShipperMomentTrigger(shipperPlayer, shipperComp);
            return GameUtils.WinStatus.NONE;
        }
        return GameUtils.WinStatus.NOT_MODIFY;
    }

    private static GameUtils.WinStatus handleShipperMomentWinCheck(ServerLevel serverWorld, ServerPlayer shipperPlayer, ShipperPlayerComponent shipperComp, List<? extends Player> remainingPlayers, GameUtils.WinStatus winStatus) {
        if (shipperComp == null) {
            return GameUtils.WinStatus.NOT_MODIFY;
        }
        WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)serverWorld);
        boolean onlyShipperAndLovers = true;
        boolean hasLivingLovers = false;
        for (Player player : remainingPlayers) {
            if (player.equals((Object)shipperPlayer)) continue;
            if (wmc != null && wmc.isModifier(player.getUUID(), ModModifiers.SHIPPER_MARK)) {
                hasLivingLovers = true;
                continue;
            }
            onlyShipperAndLovers = false;
        }
        if (onlyShipperAndLovers && hasLivingLovers) {
            SREGameRoundEndComponent gameRoundEndComponent = (SREGameRoundEndComponent)SREGameRoundEndComponent.KEY.get((Object)serverWorld);
            if (gameRoundEndComponent == null) {
                PathsRoleMod.LOGGER.error("[Shipper handleShipperMomentWinCheck] gameRoundEndComponent is null, cannot declare custom win");
                shipperComp.setShipperMomentActive(false);
                shipperComp.setShipperMomentCompleted(true);
                return GameUtils.WinStatus.NOT_MODIFY;
            }
            gameRoundEndComponent.CustomWinnerPlayers.clear();
            for (Player player : remainingPlayers) {
                gameRoundEndComponent.CustomWinnerPlayers.add(player.getUUID());
            }
            RoleUtils.customWinnerWin((ServerLevel)serverWorld, (GameUtils.WinStatus)GameUtils.WinStatus.CUSTOM, (String)ModRoles.SHIPPER.identifier().getPath(), (OptionalInt)OptionalInt.of(ModRoles.SHIPPER.color()));
            shipperComp.setShipperMomentActive(false);
            shipperComp.setShipperMomentCompleted(true);
            return GameUtils.WinStatus.CUSTOM;
        }
        
        // ⬅️ 修复：检查磕学家是否还存活
        boolean shipperStillAlive = false;
        for (Player player : remainingPlayers) {
            SREGameWorldComponent gameWorldComponent2 = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)serverWorld);
            if (gameWorldComponent2 != null && gameWorldComponent2.isRole(player, ModRoles.SHIPPER)) {
                shipperStillAlive = true;
                break;
            }
        }
        
        // 如果磕学家已死亡，取消磕学时刻，允许正常游戏结束
        if (!shipperStillAlive) {
            shipperComp.setShipperMomentActive(false);
            shipperComp.setShipperMomentCompleted(true);
            return GameUtils.WinStatus.NOT_MODIFY;
        }
        
        return GameUtils.WinStatus.NONE;
    }

    public static void checkShipperMomentTrigger(ServerPlayer shipperPlayer, ShipperPlayerComponent shipperComp) {
        if (shipperPlayer == null || shipperComp == null) {
            return;
        }
        SREGameWorldComponent gameWorld = (SREGameWorldComponent)SREGameWorldComponent.KEY.get((Object)shipperPlayer.level());
        if (gameWorld == null || !gameWorld.isRunning()) {
            return;
        }
        if (shipperComp.isShipperMomentActive() || shipperComp.isShipperMomentCompleted()) {
            return;
        }
        if (!GameUtils.isPlayerAliveAndSurvival((Player)shipperPlayer)) {
            return;
        }
        if (!shipperComp.hasLivingLovers()) {
            return;
        }
        List<? extends Player> remainingPlayers = shipperPlayer.serverLevel().getPlayers(p -> GameUtils.isPlayerAliveAndSurvival((Player)p));
        boolean allKillersDead = true;
        for (Player player : remainingPlayers) {
            SRERole role = gameWorld.getRole((Player)player);
            if (role == null || !role.canUseKiller() || role.isNeutralForKiller()) continue;
            allKillersDead = false;
            break;
        }
        if (!allKillersDead) {
            return;
        }
        boolean loversAlive = true;
        for (UUID uuid : shipperComp.getPairedLovers()) {
            Player lover = shipperPlayer.level().getPlayerByUUID(uuid);
            if (lover instanceof ServerPlayer && GameUtils.isPlayerAliveAndSurvival((Player)lover)) continue;
            loversAlive = false;
            break;
        }
        if (!loversAlive) {
            return;
        }
        shipperComp.setShipperMomentActive(true);
        shipperPlayer.displayClientMessage(
            Component.translatable("message.pathsrole.shipper.moment_hint").withStyle(ChatFormatting.GOLD),
            true);
        // 全服标题公告：磕学时刻
        Component titleText = Component.translatable("message.pathsrole.shipper_moment.title")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        if (shipperPlayer.serverLevel() != null) {
            for (ServerPlayer sp : shipperPlayer.serverLevel().players()) {
                if (sp == null || sp.connection == null) continue;
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(net.minecraft.network.chat.Component.empty()));
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(titleText));
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 60, 10));
            }
            WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)shipperPlayer.serverLevel());
            if (wmc != null) {
                for (ServerPlayer sp : shipperPlayer.serverLevel().players()) {
                    if (sp == null) continue;
                    if (wmc.isModifier(sp.getUUID(), ModModifiers.SHIPPER_MARK)) {
                        wmc.addModifier(sp.getUUID(), SEModifiers.FEATHER, false);
                        SRERole spRole = gameWorld.getRole((Player)sp);
                        if (spRole != null && spRole.isInnocent()) {
                            RoleUtils.dropAndClearAllGuns(sp);
                        }
                    }
                }
            }
            // 嗑学时刻：随机给两名无辜者发左轮手枪
            if (wmc != null && shipperComp != null) {
                java.util.List<ServerPlayer> eligibleInnocents = new ArrayList<>();
                for (Player p : remainingPlayers) {
                    if (p == null || p == shipperPlayer) continue;
                    SRERole role = gameWorld.getRole(p);
                    if (role == null || !role.isInnocent()) continue;
                    if (wmc.isModifier(p.getUUID(), ModModifiers.SHIPPER_MARK)) continue;
                    if (gameWorld.isRole(p, ModRoles.SHIPPER)) continue;
                    eligibleInnocents.add((ServerPlayer) p);
                }
                if (!eligibleInnocents.isEmpty()) {
                    java.util.Random random = new java.util.Random();
                    for (int i = 0; i < 2 && !eligibleInnocents.isEmpty(); i++) {
                        int index = random.nextInt(eligibleInnocents.size());
                        ServerPlayer innocent = eligibleInnocents.remove(index);
                        if (innocent == null || ModItems.SHIPPER_MOMENT_REVOLVER == null) continue;
                        ItemStack revolver = new ItemStack(ModItems.SHIPPER_MOMENT_REVOLVER);
                        revolver.set(DataComponents.CUSTOM_NAME,
                                Component.literal("啊哈啊哈的给了你一把枪，天呐，随机给两名无辜者一把枪——这叫公平对决吗，对磕学来说"));
                        innocent.addItem(revolver);
                        shipperComp.addMomentRevolverRecipient(innocent.getUUID());
                    }
                }
            }
        }
        shipperPlayer.getInventory().clearContent();
        shipperPlayer.addItem(new ItemStack((net.minecraft.world.level.ItemLike)TMMItems.REVOLVER));
        shipperPlayer.addItem(new ItemStack((net.minecraft.world.level.ItemLike)TMMItems.CROWBAR));
        shipperPlayer.addItem(ModItems.RAGE_SHIPPER.getDefaultInstance());
        UUID lover1 = null;
        UUID lover2 = null;
        if (shipperComp.getPairedLovers().size() >= 2) {
            lover1 = shipperComp.getPairedLovers().get(0);
            lover2 = shipperComp.getPairedLovers().get(1);
        }
        Player lover1Player = lover1 != null ? shipperPlayer.level().getPlayerByUUID(lover1) : null;
        Player lover2Player = lover2 != null ? shipperPlayer.level().getPlayerByUUID(lover2) : null;
        SRE.REPLAY_MANAGER.recordCustomEvent((Component)Component.translatable((String)"replay.event.pathsrole.shipper.shipper_moment", (Object[])new Object[]{GameReplayUtils.getReplayPlayerDisplayText((Player)shipperPlayer, (boolean)true), lover1Player != null ? GameReplayUtils.getReplayPlayerDisplayText((Player)lover1Player, (boolean)true) : Component.literal("?"), lover2Player != null ? GameReplayUtils.getReplayPlayerDisplayText((Player)lover2Player, (boolean)true) : Component.literal("?")}));
    }

    private static void handleShipperMomentLinkedDeath(ServerPlayer deadPlayer, SREGameWorldComponent gameWorld) {
        boolean hasPendingBetrayal;
        WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)deadPlayer.serverLevel());
        if (wmc == null) {
            return;
        }
        if (deadPlayer.serverLevel().getServer() == null || !deadPlayer.serverLevel().getServer().isRunning()) {
            return;
        }
        boolean isShipper = gameWorld.isRole((Player)deadPlayer, ModRoles.SHIPPER);
        boolean isMarkedLover = wmc.isModifier(deadPlayer.getUUID(), ModModifiers.SHIPPER_MARK);
        ShipperPlayerComponent shipperComp = ShipperEvents.findShipperComponent(deadPlayer);
        boolean bl = hasPendingBetrayal = shipperComp != null && shipperComp.getPendingBetrayalKiller() != null;
        if (!(isShipper || isMarkedLover || hasPendingBetrayal)) {
            return;
        }
        if (shipperComp == null || !shipperComp.isShipperMomentActive()) {
            return;
        }
        if (isShipper) {
            for (UUID uuid : new ArrayList<UUID>(shipperComp.getPairedLovers())) {
                ServerPlayer sp;
                Player lover = deadPlayer.serverLevel().getPlayerByUUID(uuid);
                if (!(lover instanceof ServerPlayer) || !GameUtils.isPlayerAliveAndSurvival((Player)(sp = (ServerPlayer)lover))) continue;
                GameUtils.forceKillPlayer((Player)sp, (boolean)true, (Player)deadPlayer, (ResourceLocation)PathsRoleMod.id("shipper_moment_bond"));
            }
            return;
        }
        UUID betrayalKiller = shipperComp.getPendingBetrayalKiller();
        if (betrayalKiller != null) {
            ServerPlayer sp;
            shipperComp.clearPendingBetrayalKiller();
            Player killer = deadPlayer.serverLevel().getPlayerByUUID(betrayalKiller);
            if (killer instanceof ServerPlayer && GameUtils.isPlayerAliveAndSurvival((Player)(sp = (ServerPlayer)killer))) {
                GameUtils.forceKillPlayer((Player)sp, (boolean)true, (Player)deadPlayer, (ResourceLocation)PathsRoleMod.id("shipper_moment_bond"));
            }
            return;
        }
        ServerPlayer shipper = null;
        for (ServerPlayer sp : deadPlayer.serverLevel().players()) {
            if (!gameWorld.isRole((Player)sp, ModRoles.SHIPPER) || !GameUtils.isPlayerAliveAndSurvival((Player)sp)) continue;
            shipper = sp;
            break;
        }
        if (shipper != null) {
            GameUtils.forceKillPlayer(shipper, (boolean)true, (Player)deadPlayer, (ResourceLocation)PathsRoleMod.id("shipper_moment_bond"));
        }
        for (UUID uuid : new ArrayList<UUID>(shipperComp.getPairedLovers())) {
            ServerPlayer sp;
            Player otherLover;
            if (uuid.equals(deadPlayer.getUUID()) || !((otherLover = deadPlayer.serverLevel().getPlayerByUUID(uuid)) instanceof ServerPlayer) || !GameUtils.isPlayerAliveAndSurvival((Player)(sp = (ServerPlayer)otherLover))) continue;
            GameUtils.forceKillPlayer((Player)sp, (boolean)true, (Player)deadPlayer, (ResourceLocation)PathsRoleMod.id("shipper_moment_bond"));
        }
    }

    public static void registerSkills() {
        RoleSkill.register((SRERole)ModRoles.SHIPPER, new RoleSkill.Definition[]{
            RoleSkill.skill(PathsRoleMod.id("shipper_book"), "skill.pathsrole.shipper_observation", context -> {
                ServerPlayer player = context.player();
                ShipperPlayerComponent comp = ShipperPlayerComponent.KEY.get(player);
                if (comp == null) return false;
                if (comp.isObservationOnCooldown()) {
                    long remaining = comp.getObservationCooldownRemainingSeconds();
                    player.displayClientMessage(
                            Component.translatable("message.pathsrole.shipper.observation_cooldown", remaining),
                            true);
                    return false;
                }
                boolean result = comp.toggleObservation();
                if (result) {
                    String state = comp.isObservationActive() ? "开启" : "关闭";
                    player.displayClientMessage(
                            Component.translatable("message.pathsrole.shipper.observation_toggle", state),
                            true);
                }
                return result;
            })
                .cooldownTicks(100).toggleable(true).showOnHud(true).announceToSelf(false).build()
        });
    }

    private static String getSafeName(Player player) {
        if (player == null) return "?";
        return player.getGameProfile() != null ? player.getGameProfile().getName() : player.getName().getString();
    }
}