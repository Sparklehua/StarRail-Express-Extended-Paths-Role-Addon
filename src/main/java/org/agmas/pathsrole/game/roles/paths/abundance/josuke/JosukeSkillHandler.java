package org.agmas.pathsrole.game.roles.paths.abundance.josuke;

import io.wifi.starrailexpress.SRE;
import io.wifi.starrailexpress.game.GameUtils;
import net.minecraft.world.level.GameType;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.content.entity.PlayerBodyEntity;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.init.ModSounds;
import org.agmas.pathsrole.cca.PathsroleComponents;
import net.minecraft.world.item.Items;

import java.util.UUID;

public final class JosukeSkillHandler {
    private static final int REVIVE_COOLDOWN = 120 * 20;
    private static final int MAX_DEATH_TIME_TICKS = 10 * 20;

    private JosukeSkillHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer josuke) || !(entity instanceof PlayerBodyEntity body)) {
                return InteractionResult.PASS;
            }
            if (!isJosuke(josuke) || !GameUtils.isPlayerAliveAndSurvival(josuke)) {
                return InteractionResult.PASS;
            }
            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            if (body.getPlayerUuid() == null) {
                return InteractionResult.PASS;
            }
            Player revivedPlayer = serverLevel.getPlayerByUUID(body.getPlayerUuid());
            if (!(revivedPlayer instanceof ServerPlayer revived)) {
                return InteractionResult.PASS;
            }
            if (revived == null || !revived.isSpectator()) {
                return InteractionResult.PASS;
            }
            JosukePlayerComponent josukeState = PathsroleComponents.JOSUKE_PLAYER_KEY.getNullable(josuke);
            if (josukeState == null || !josukeState.isReviveReady()) {
                return InteractionResult.PASS;
            }
            if (body.tickCount > MAX_DEATH_TIME_TICKS) {
                josuke.displayClientMessage(Component.translatable("message.pathsrole.josuke.death_too_old")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.PASS;
            }

            revived.getInventory().clearContent();
            revived.teleportTo(body.getX(), body.getY(), body.getZ());
            revived.setGameMode(GameType.ADVENTURE);
            resetVoiceThroughMainMod(revived.getUUID());
            body.remove(Entity.RemovalReason.DISCARDED);
            josukeState.startReviveCooldown();
            josuke.getCooldowns().addCooldown(Items.CLOCK, REVIVE_COOLDOWN);
            serverLevel.players().forEach(p -> {
                p.playNotifySound(SoundEvents.TOTEM_USE, revived.getSoundSource(), 1.2f, 1.5f);
                p.playNotifySound(ModSounds.JOSUKE_REVIVE, revived.getSoundSource(), 1.0f, 1.0f);
                p.displayClientMessage(Component.translatable("message.pathsrole.josuke.revived", revived.getDisplayName())
                        .withStyle(ChatFormatting.GREEN), true);
            });
            SRE.REPLAY_MANAGER.recordPlayerRevival(revived.getUUID(), SREGameWorldComponent.KEY.get(serverLevel).getRole(revived));
            return InteractionResult.CONSUME;
        });
    }

    private static void resetVoiceThroughMainMod(UUID playerUuid) {
        try {
            Class<?> pluginClass = Class.forName("io.wifi.starrailexpress.compat.TrainVoicePlugin");
            pluginClass.getMethod("resetPlayer", UUID.class).invoke(null, playerUuid);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean isJosuke(ServerPlayer player) {
        var gw = SREGameWorldComponent.KEY.get(player.level());
        return gw != null && gw.isRole(player, ModRoles.JOSUKE);
    }
}