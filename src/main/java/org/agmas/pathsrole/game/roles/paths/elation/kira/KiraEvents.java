package org.agmas.pathsrole.game.roles.paths.elation.kira;

import io.wifi.starrailexpress.cca.AreasWorldComponent;
import io.wifi.starrailexpress.cca.SREGameTimeComponent;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.event.AllowPlayerDeathWithKiller;
import io.wifi.starrailexpress.event.OnGameEnd;
import io.wifi.starrailexpress.event.OnGameTrueStarted;
import io.wifi.starrailexpress.event.OnPlayerKilledPlayer;
import io.wifi.starrailexpress.game.GameConstants;
import io.wifi.starrailexpress.game.GameUtils;
import io.wifi.starrailexpress.game.ServerTaskInfoClasses;
import org.agmas.noellesroles.Noellesroles;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.abundance.josuke.JosukeFistPunchHandler;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.init.ModSounds;
import org.agmas.noellesroles.init.ModEffects;

public class KiraEvents {
    public static void registerEvents() {
        OnGameEnd.EVENT.register((serverLevel, gameWorldComponent) -> {
            JosukeFistPunchHandler.PUNCH_RECORDS.clear();
        });

        OnGameTrueStarted.EVENT.register((serverLevel) -> {
            JosukeFistPunchHandler.PUNCH_RECORDS.clear();
        });

        io.wifi.starrailexpress.event.AllowGameEnd.EVENT.register((serverLevel, winStatus, isLooseEndsMode) -> {
            if (isLooseEndsMode || winStatus == GameUtils.WinStatus.TIME) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }

            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(serverLevel);
            if (gameWorld == null || !gameWorld.isRunning()) {
                return GameUtils.WinStatus.NOT_MODIFY;
            }

            boolean kiraAlive = false;
            boolean otherAlive = false;
            for (ServerPlayer player : serverLevel.players()) {
                if (!GameUtils.isPlayerAliveAndSurvival(player)) {
                    continue;
                }
                if (gameWorld.isRole(player, ModRoles.KIRA)) {
                    kiraAlive = true;
                } else {
                    otherAlive = true;
                }
            }

            if (kiraAlive && !otherAlive) {
                return GameUtils.WinStatus.KILLERS;
            }
            return GameUtils.WinStatus.NOT_MODIFY;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return net.minecraft.world.InteractionResult.PASS;
            if (level.isClientSide()) return net.minecraft.world.InteractionResult.PASS;

            if (!(entity instanceof Player targetPlayer)) return net.minecraft.world.InteractionResult.PASS;

            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(level);
            if (gameWorld == null || !gameWorld.isRunning() || !gameWorld.isRole(player, ModRoles.KIRA)) return net.minecraft.world.InteractionResult.PASS;

            if (player.hasEffect(ModEffects.SAFE_TIME)) {
                player.displayClientMessage(
                    Component.translatable("message.pathsrole.kira.safe_time_cannot_mark").withStyle(ChatFormatting.RED),
                    true
                );
                return net.minecraft.world.InteractionResult.PASS;
            }

            KiraPlayerComponent kiraComp = PathsroleComponents.getKiraComponent(player);
            SREPlayerShopComponent shopComp = SREPlayerShopComponent.KEY.get(player);
            if (kiraComp == null || shopComp == null || targetPlayer == player) {
                return net.minecraft.world.InteractionResult.PASS;
            }

            if (kiraComp.isMarked(targetPlayer.getUUID())) {
                player.displayClientMessage(
                    Component.translatable("message.pathsrole.kira.already_marked"),
                    true
                );
                return net.minecraft.world.InteractionResult.PASS;
            }

            if (kiraComp.getMarkCooldown() > 0) {
                int seconds = (kiraComp.getMarkCooldown() + 19) / 20;
                player.displayClientMessage(
                    Component.translatable("message.pathsrole.kira.mark_cooldown", seconds)
                        .withStyle(ChatFormatting.RED),
                    true
                );
                return net.minecraft.world.InteractionResult.PASS;
            }

            if (!kiraComp.canAddMark()) {
                player.displayClientMessage(
                    Component.translatable("message.pathsrole.kira.max_marks_reached", kiraComp.getMarkCount(), KiraPlayerComponent.MAX_MARKS)
                        .withStyle(ChatFormatting.RED),
                    true
                );
                return net.minecraft.world.InteractionResult.PASS;
            }

            if (shopComp == null || kiraComp == null || shopComp.balance < 10) {
                player.displayClientMessage(
                    Component.translatable("message.noellesroles.insufficient_funds"),
                    true
                );
                return net.minecraft.world.InteractionResult.PASS;
            }

            shopComp.addToBalance(-10);
            kiraComp.addMark(targetPlayer.getUUID());

            player.displayClientMessage(
                Component.translatable("message.pathsrole.kira.marked_success", targetPlayer.getName(), kiraComp.getMarkCount(), KiraPlayerComponent.MAX_MARKS)
                    .withStyle(ChatFormatting.GREEN),
                true
            );

            return net.minecraft.world.InteractionResult.SUCCESS;
        });

        OnPlayerKilledPlayer.EVENT.register((victim, killer, reason) -> {
            if (killer == null || killer.level().isClientSide()) return;
            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(killer.level());
            if (gameWorld != null && gameWorld.isRunning() && gameWorld.isRole(killer, ModRoles.KIRA)) {
                KiraPlayerComponent kiraComp = PathsroleComponents.getKiraComponent(killer);
                if (kiraComp != null && !isKiraExplosionReason(reason)) {
                    kiraComp.incrementKillCount();
                }
            }
        });

        AllowPlayerDeathWithKiller.EVENT.register((victim, killer, deathReason) -> {
            if (victim == null || victim.level().isClientSide())
                return true;

            SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(victim.level());
            if (gameWorld == null || !gameWorld.isRunning() || !gameWorld.isRole(victim, ModRoles.KIRA))
                return true;

            KiraPlayerComponent kiraComp = PathsroleComponents.getKiraComponent(victim);
            if (kiraComp == null || !kiraComp.isBitesTheDustUnlocked() || kiraComp.isBitesTheDustUsed())
                return true;

            if (killer == null
                || deathReason.equals(GameConstants.DeathReasons.BROKEN_HEART)
                || deathReason.equals(GameConstants.DeathReasons.GOD_COMMAND)
                || deathReason.equals(Noellesroles.id("voodoo"))) {
                return true;
            }

            if (victim instanceof ServerPlayer sp) {
                ServerLevel level = sp.serverLevel();

                for (ServerPlayer p : level.players()) {
                    p.connection.send(new ClientboundSoundPacket(
                        net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.KIRA_BITES_THE_DUST),
                        net.minecraft.sounds.SoundSource.MASTER,
                        p.getX(), p.getY(), p.getZ(), 5.0f, 1.0f, p.getRandom().nextLong()));
                }

                if (!kiraComp.useBitesTheDust()) {
                    return true;
                }
                int remaining = kiraComp.getRemainingBitesTheDust();

                sp.setHealth(sp.getMaxHealth());
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 255, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 255, false, false));

                for (Player p : level.players()) {
                    p.displayClientMessage(
                        Component.translatable("message.pathsrole.kira.bites_the_dust_activated", remaining)
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                        true
                    );
                }

                GameUtils.serverAsynTaskLists.add(new ServerTaskInfoClasses.SchedulerTask(20, () -> {
                    sp.setHealth(sp.getMaxHealth());
                    sp.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);

                    sp.removeAllEffects();

                    if (sp.isRemoved()) {
                        return;
                    }

                    if (kiraComp.hasMarkedPosition()) {
                        net.minecraft.world.phys.Vec3 pos = kiraComp.getMarkedPosition();
                        sp.teleportTo(pos.x, pos.y, pos.z);
                    } else {
                        net.minecraft.world.phys.Vec3 spawnPos = GameUtils.getSpawnPos(
                            AreasWorldComponent.KEY.get(level),
                            GameUtils.roomToPlayer.getOrDefault(sp.getUUID(), 1)
                        );
                        if (spawnPos != null) {
                            sp.teleportTo(spawnPos.x, spawnPos.y + 1, spawnPos.z);
                        }
                    }

                    AreasWorldComponent areas = AreasWorldComponent.KEY.get(level);
                    if (areas != null) {
                        for (ServerPlayer player : level.players()) {
                            if (player != sp && GameUtils.isPlayerAliveAndSurvival(player)) {
                                net.minecraft.world.phys.Vec3 spawnPos = GameUtils.getSpawnPos(
                                    areas,
                                    GameUtils.roomToPlayer.getOrDefault(player.getUUID(), 1)
                                );
                                if (spawnPos != null) {
                                    player.teleportTo(spawnPos.x, spawnPos.y + 1, spawnPos.z);
                                }
                            }
                        }
                    }

                    if (sp.isRemoved()) {
                        return;
                    }
                    SREGameTimeComponent gameTimeComponent = SREGameTimeComponent.KEY.get(level);
                    if (gameTimeComponent != null) {
                        gameTimeComponent.addTime(-KiraPlayerComponent.BITES_THE_DUST_ROLLBACK_TICKS);
                    }
                }));

            }

            return false;
        });
    }

    private static boolean isKiraExplosionReason(OnPlayerKilledPlayer.DeathReason reason) {
        return reason == OnPlayerKilledPlayer.DeathReason.GRENADE;
    }
}