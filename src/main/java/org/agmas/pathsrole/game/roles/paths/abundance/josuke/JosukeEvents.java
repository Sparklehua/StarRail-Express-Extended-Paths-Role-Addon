package org.agmas.pathsrole.game.roles.paths.abundance.josuke;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.event.OnPlayerDeath;
import io.wifi.starrailexpress.event.OnPlayerDeathWithKiller;
import io.wifi.starrailexpress.game.GameUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.noellesroles.component.DeathPenaltyComponent;
import org.agmas.noellesroles.component.ModComponents;
import org.agmas.pathsrole.init.ModRoles;

public class JosukeEvents {
    private static final int PENALTY_DURATION_TICKS = 30 * 20;

    public static void register() {
        OnPlayerDeathWithKiller.EVENT.register((victim, killer, deathReason) -> {
            applyJosukePenalty(victim);
        });

        OnPlayerDeath.EVENT.register((victim, deathReason) -> {
            applyJosukePenalty(victim);
        });

        ModdedRoleAssigned.EVENT.register((player, role) -> {
            if (role == null) return;
            if (role.identifier().equals(ModRoles.JOSUKE_ID)) {
                applyPenaltyToAllSpectators(player);
            }
        });
    }

    private static void applyJosukePenalty(Player victim) {
        if (victim == null || victim.level().isClientSide()) return;

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(victim.level());
        if (gameWorld == null || !gameWorld.isRunning()) return;

        boolean josukeAlive = false;
        for (Player p : victim.level().players()) {
            if (GameUtils.isPlayerAliveAndSurvival(p) && gameWorld.isRole(p, ModRoles.JOSUKE)) {
                josukeAlive = true;
                break;
            }
        }

        if (!josukeAlive) return;

        DeathPenaltyComponent penalty = ModComponents.DEATH_PENALTY.get(victim);
        if (penalty.hasPenalty()) {
            return;
        }

        penalty.setPenalty(PENALTY_DURATION_TICKS, true);

        if (victim instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    Component.translatable("message.pathsrole.josuke.penalty").withStyle(ChatFormatting.RED),
                    true);
        }
    }

    private static void applyPenaltyToAllSpectators(ServerPlayer josuke) {
        if (josuke == null || josuke.level().isClientSide()) return;

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(josuke.level());
        if (gameWorld == null || !gameWorld.isRunning()) return;

        for (Player p : josuke.level().players()) {
            if (p == josuke) continue;
            if (GameUtils.isPlayerSpectator(p)) {
                applyJosukePenalty(p);
            }
        }
    }
}