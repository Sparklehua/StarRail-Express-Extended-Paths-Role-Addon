package org.agmas.pathsrole.network;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.game.GameUtils;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.init.ModEffects;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.content.entity.SheerHeartEntity;
import org.agmas.pathsrole.game.roles.paths.elation.kira.KiraPlayerComponent;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PacketHandler {
    public static void register() {
        PayloadTypeRegistry.playC2S().register(RecallSheerHeartPacket.ID, RecallSheerHeartPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(KiraC2SPacket.ID, KiraC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(JosukeSkillC2SPacket.ID, JosukeSkillC2SPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RecallSheerHeartPacket.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null || player.level().isClientSide() || !player.isCrouching()) {
                return;
            }

            List<SheerHeartEntity> entities = player.level().getEntitiesOfClass(SheerHeartEntity.class,
                player.getBoundingBox().inflate(200),
                entity -> entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID()));

            if (!entities.isEmpty()) {
                SheerHeartEntity entity = entities.get(0);
                entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);

                if (!player.getInventory().add(new ItemStack(ModItems.KIRA_SHEER_HEART_ATTACK))) {
                    player.drop(new ItemStack(ModItems.KIRA_SHEER_HEART_ATTACK), false);
                }

                player.displayClientMessage(Component.translatable("message.pathsrole.kira.recalled_sheer_heart"), true);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(KiraC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player == null || player.level().isClientSide()) {
                    return;
                }
                SREGameWorldComponent gameWorldComponent = SREGameWorldComponent.KEY.get(player.level());

                if (gameWorldComponent == null || !gameWorldComponent.isRunning()
                        || !gameWorldComponent.isRole(player, ModRoles.KIRA)) {
                    return;
                }

                UUID targetUuid = payload.player();
                Player targetPlayer = targetUuid != null ? player.level().getPlayerByUUID(targetUuid) : null;

                KiraPlayerComponent kiraComponent = PathsroleComponents.getKiraComponent(player);
                SREPlayerShopComponent shopComponent = SREPlayerShopComponent.KEY.get(player);
                if (kiraComponent == null || shopComponent == null) {
                    return;
                }

                if (!payload.isValidAction()) {
                    return;
                }
                int action = payload.action();

                if (action == KiraC2SPacket.ACTION_MARK) {
                    if (targetPlayer != null && targetPlayer != player) {
                        handleKiraMark(player, targetPlayer, targetUuid, kiraComponent, shopComponent);
                    }
                } else if (action == KiraC2SPacket.ACTION_DETONATE) {
                    if (targetUuid != null && targetPlayer != null && targetPlayer != player) {
                        handleKiraDetonate(player, targetPlayer, targetUuid, kiraComponent, shopComponent);
                    } else {
                        handleKiraDetonateAll(player, kiraComponent, shopComponent);
                    }
                } else if (action == KiraC2SPacket.ACTION_TOGGLE_JEB) {
                    if (targetPlayer != null) {
                        handleKiraToggleJeb(player, targetPlayer, targetUuid, gameWorldComponent);
                    }
                } else if (action == KiraC2SPacket.ACTION_ANCHOR_MARK) {
                    handleKiraAnchorMark(player, kiraComponent);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(JosukeSkillC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player == null || player.level().isClientSide()) {
                    return;
                }
                SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
                if (gameWorld == null || !gameWorld.isRunning()
                        || !gameWorld.isRole(player, ModRoles.JOSUKE)) {
                    return;
                }
                handleJosukeSkill(player, payload.targetUuid());
            });
        });
    }

    private static void handleKiraMark(ServerPlayer player, Player targetPlayer, UUID targetUuid,
                                      KiraPlayerComponent kiraComponent, SREPlayerShopComponent shopComponent) {
        if (!GameUtils.isPlayerAliveAndSurvival(targetPlayer)) {
            return;
        }

        if (player.hasEffect(ModEffects.SAFE_TIME)) {
            player.displayClientMessage(Component.translatable("message.pathsrole.kira.safe_time_cannot_mark").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (kiraComponent == null || shopComponent == null || kiraComponent.isMarked(targetUuid)) {
            player.displayClientMessage(Component.translatable("message.pathsrole.kira.already_marked"), true);
            return;
        }

        if (!kiraComponent.canAddMark()) {
            if (kiraComponent.getMarkCount() >= KiraPlayerComponent.MAX_MARKS) {
                player.displayClientMessage(
                    Component.translatable("message.pathsrole.kira.max_marks_reached", kiraComponent.getMarkCount(), KiraPlayerComponent.MAX_MARKS)
                        .withStyle(ChatFormatting.RED),
                    true
                );
            } else {
                int remainingSeconds = (kiraComponent.getMarkCooldown() + 19) / 20;
                player.displayClientMessage(
                    Component.translatable("message.pathsrole.kira.mark_cooldown", remainingSeconds)
                        .withStyle(ChatFormatting.RED),
                    true
                );
            }
            return;
        }

        if (shopComponent.balance < 10) {
            player.displayClientMessage(Component.translatable("message.noellesroles.insufficient_funds"), true);
            return;
        }

        shopComponent.addToBalance(-10);
        kiraComponent.addMark(targetUuid);

        player.displayClientMessage(
            Component.translatable("message.pathsrole.kira.marked_success", targetPlayer.getName(), kiraComponent.getMarkCount(), KiraPlayerComponent.MAX_MARKS)
                .withStyle(ChatFormatting.GREEN),
            true
        );
    }

    private static void handleKiraDetonate(ServerPlayer player, Player targetPlayer, UUID targetUuid,
                                          KiraPlayerComponent kiraComponent, SREPlayerShopComponent shopComponent) {
        if (!GameUtils.isPlayerAliveAndSurvival(targetPlayer)) {
            return;
        }

        if (kiraComponent == null || shopComponent == null || !kiraComponent.isMarked(targetUuid)) {
            player.displayClientMessage(Component.translatable("message.pathsrole.kira.not_marked"), true);
            return;
        }

        if (!kiraComponent.canDetonate()) {
            int remainingSeconds = (kiraComponent.getDetonateCooldown() + 19) / 20;
            player.displayClientMessage(
                Component.translatable("message.pathsrole.kira.detonate_cooldown", remainingSeconds)
                    .withStyle(ChatFormatting.RED),
                true
            );
            return;
        }

        if (shopComponent.balance < 90) {
            player.displayClientMessage(Component.translatable("message.noellesroles.insufficient_funds"), true);
            return;
        }

        shopComponent.addToBalance(-90);
        kiraComponent.removeMark(targetUuid);

        WorldModifierComponent worldModifierComponent = WorldModifierComponent.KEY.get(player.level());
        if (worldModifierComponent != null) {
            worldModifierComponent.removeModifier(targetUuid, SEModifiers.JEB_);
        }

        if (targetPlayer instanceof ServerPlayer serverTarget) {
            serverTarget.level().playSound(
                null,
                serverTarget.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                2.0f,
                1.0f
            );

            ((ServerLevel) serverTarget.level()).sendParticles(
                ParticleTypes.EXPLOSION,
                serverTarget.getX(),
                serverTarget.getY(),
                serverTarget.getZ(),
                1,
                0,
                0,
                0,
                0
            );

            GameUtils.killPlayer(serverTarget, true, player, PathsRoleMod.id("kira_bomb"));

            shopComponent.addToBalance(40);
            kiraComponent.setDetonateCooldown(200);

            player.displayClientMessage(
                Component.translatable("message.pathsrole.kira.detonated", targetPlayer.getName()),
                true
            );
        }
    }

    private static void handleKiraDetonateAll(ServerPlayer player,
                                             KiraPlayerComponent kiraComponent, SREPlayerShopComponent shopComponent) {
        Set<UUID> markedUuids = kiraComponent.getMarkedPlayers();

        if (kiraComponent == null || shopComponent == null || markedUuids.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.pathsrole.kira.no_marks"), true);
            return;
        }

        if (!kiraComponent.canDetonate()) {
            int remainingSeconds = (kiraComponent.getDetonateCooldown() + 19) / 20;
            player.displayClientMessage(
                Component.translatable("message.pathsrole.kira.detonate_cooldown", remainingSeconds)
                    .withStyle(ChatFormatting.RED),
                true
            );
            return;
        }

        int validTargets = 0;
        for (UUID targetUuid : markedUuids) {
            Player targetPlayer = player.level().getPlayerByUUID(targetUuid);
            if (targetPlayer instanceof ServerPlayer && GameUtils.isPlayerAliveAndSurvival(targetPlayer)) {
                validTargets++;
            }
        }
        if (validTargets == 0) {
            player.displayClientMessage(Component.translatable("message.pathsrole.kira.no_valid_targets"), true);
            return;
        }
        int totalCost = validTargets * 90;
        if (shopComponent.balance < totalCost) {
            player.displayClientMessage(Component.translatable("message.noellesroles.insufficient_funds"), true);
            return;
        }

        shopComponent.addToBalance(-totalCost);

        int kills = 0;
        WorldModifierComponent worldModifierComponent = WorldModifierComponent.KEY.get(player.level());
        if (worldModifierComponent == null) {
            shopComponent.addToBalance(totalCost);
            return;
        }

        for (UUID targetUuid : markedUuids) {
            Player targetPlayer = player.level().getPlayerByUUID(targetUuid);
            if (targetPlayer instanceof ServerPlayer serverTarget && GameUtils.isPlayerAliveAndSurvival(targetPlayer)) {
                worldModifierComponent.removeModifier(targetUuid, SEModifiers.JEB_);

                serverTarget.level().playSound(
                    null,
                    serverTarget.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS,
                    2.0f,
                    1.0f
                );

                ((ServerLevel) serverTarget.level()).sendParticles(
                    ParticleTypes.EXPLOSION,
                    serverTarget.getX(),
                    serverTarget.getY(),
                    serverTarget.getZ(),
                    1,
                    0,
                    0,
                    0,
                    0
                );

                GameUtils.killPlayer(serverTarget, true, player, PathsRoleMod.id("kira_bomb"));

                kills++;
            }

            kiraComponent.removeMark(targetUuid);
        }

        shopComponent.addToBalance(kills * 40);
        kiraComponent.setDetonateCooldown(200);

        player.displayClientMessage(
            Component.translatable("message.pathsrole.kira.detonated_all", kills),
            true
        );
    }

    private static void handleKiraToggleJeb(ServerPlayer player, Player targetPlayer, UUID targetUuid,
                                           SREGameWorldComponent gameWorldComponent) {
        if (targetPlayer == null || targetUuid == null || gameWorldComponent == null || !gameWorldComponent.isRunning()) {
            return;
        }
        boolean isSelfOrTeammate = targetUuid.equals(player.getUUID()) ||
            (gameWorldComponent.getRole(targetPlayer) != null &&
             gameWorldComponent.getRole(targetPlayer).isKillerTeam());

        if (!isSelfOrTeammate) {
            player.displayClientMessage(Component.translatable("message.pathsrole.kira.not_teammate"), true);
            return;
        }

        WorldModifierComponent worldModifierComponent = WorldModifierComponent.KEY.get(player.level());
        if (worldModifierComponent == null) {
            return;
        }
        if (worldModifierComponent.isModifier(targetUuid, SEModifiers.JEB_)) {
            worldModifierComponent.removeModifier(targetUuid, SEModifiers.JEB_);
            player.displayClientMessage(
                Component.translatable("message.pathsrole.kira.jeb_removed", targetPlayer.getName()),
                true
            );
        } else {
            worldModifierComponent.addModifier(targetUuid, SEModifiers.JEB_);
            player.displayClientMessage(
                Component.translatable("message.pathsrole.kira.jeb_added", targetPlayer.getName()),
                true
            );
        }
    }

    private static void handleKiraAnchorMark(ServerPlayer player, KiraPlayerComponent kiraComponent) {
        if (kiraComponent == null || !kiraComponent.isBitesTheDustUnlocked() || kiraComponent.isBitesTheDustUsed()) {
            return;
        }
        kiraComponent.markPosition(player.getX(), player.getY(), player.getZ());
        player.displayClientMessage(
            Component.translatable("message.pathsrole.kira.position_marked")
                .withStyle(ChatFormatting.GOLD),
            true
        );
    }

    private static void handleJosukeSkill(ServerPlayer player, UUID targetUuid) {
        if (!GameUtils.isPlayerAliveAndSurvival(player)) {
            return;
        }

        if (targetUuid == null || targetUuid.equals(new UUID(0, 0))) {
            boolean cleared = false;
            if (player.hasEffect(ModEffects.VOICE_SILENCE)) {
                player.removeEffect(ModEffects.VOICE_SILENCE);
                cleared = true;
            }
            WorldModifierComponent wmc = WorldModifierComponent.KEY.get(player.level());
            if (wmc != null && wmc.isModifier(player.getUUID(), SEModifiers.JEB_)) {
                wmc.removeModifier(player.getUUID(), SEModifiers.JEB_);
                cleared = true;
            }
            if (cleared) {
                player.displayClientMessage(
                    Component.translatable("message.pathsrole.josuke.self_cleared")
                            .withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(
                    Component.translatable("message.pathsrole.josuke.self_nothing")
                            .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        Player target = player.serverLevel().getServer().getPlayerList().getPlayer(targetUuid);
        if (target == null || target == player || player.distanceToSqr(target) > 36.0) {
            player.displayClientMessage(
                Component.translatable("message.pathsrole.josuke.no_target").withStyle(ChatFormatting.RED),
                true
            );
            return;
        }

        boolean cleared = false;
        for (ServerPlayer p : player.serverLevel().getServer().getPlayerList().getPlayers()) {
            var gw = SREGameWorldComponent.KEY.get(p.level());
            if (gw == null || !gw.isRole(p, ModRoles.KIRA)) {
                continue;
            }
            KiraPlayerComponent kiraState = PathsroleComponents.KIRA_PLAYER_KEY.getNullable(p);
            if (kiraState == null || !kiraState.isMarked(targetUuid)) {
                if (target instanceof ServerPlayer serverTarget) {
                    if (serverTarget.hasEffect(ModEffects.VOICE_SILENCE)) {
                        serverTarget.removeEffect(ModEffects.VOICE_SILENCE);
                        cleared = true;
                    }
                }
                WorldModifierComponent wmc = WorldModifierComponent.KEY.get(player.level());
                if (wmc != null && wmc.isModifier(targetUuid, SEModifiers.JEB_)) {
                    wmc.removeModifier(targetUuid, SEModifiers.JEB_);
                    cleared = true;
                }
                if (cleared) {
                    target.displayClientMessage(
                        Component.translatable("message.pathsrole.josuke.clear_kira"), true);
                }
                break;
            }
        }

        if (cleared) {
            player.displayClientMessage(
                Component.translatable("message.pathsrole.josuke.cleared",
                    target.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
        } else {
            player.displayClientMessage(
                Component.translatable("message.pathsrole.josuke.not_marked").withStyle(ChatFormatting.RED),
                true
            );
        }
    }
}