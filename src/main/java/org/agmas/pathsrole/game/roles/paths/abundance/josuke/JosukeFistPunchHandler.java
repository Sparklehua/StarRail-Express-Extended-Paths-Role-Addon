package org.agmas.pathsrole.game.roles.paths.abundance.josuke;

import io.wifi.starrailexpress.SRE;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import io.wifi.starrailexpress.event.AllowPlayerPunching;
import io.wifi.starrailexpress.event.OnShieldBroken;
import io.wifi.starrailexpress.game.GameUtils;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.noellesroles.init.ModEffects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JosukeFistPunchHandler {

    public static final Map<UUID, PunchRecord> PUNCH_RECORDS = new HashMap<>();

    private static final double SHOCKWAVE_RADIUS = 6.0;

    private static final double SHOCKWAVE_STRENGTH = 2.5;

    public static void applyShockwave(Player epicenter) {
        Vec3 origin = epicenter.position();

        AABB searchBox = new AABB(
                origin.x - SHOCKWAVE_RADIUS, origin.y - SHOCKWAVE_RADIUS, origin.z - SHOCKWAVE_RADIUS,
                origin.x + SHOCKWAVE_RADIUS, origin.y + SHOCKWAVE_RADIUS, origin.z + SHOCKWAVE_RADIUS);

        List<Player> nearby = epicenter.level().getEntitiesOfClass(
                Player.class,
                searchBox,
                e -> e != epicenter && GameUtils.isPlayerAliveAndSurvival(e));

        for (Player target : nearby) {
            double dx = target.getX() - origin.x;
            double dz = target.getZ() - origin.z;
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist == 0) {
                double angle = Math.random() * Math.PI * 2;
                dx = Math.cos(angle);
                dz = Math.sin(angle);
                dist = 1.0;
            }

            double falloff = 1.0 - (dist / SHOCKWAVE_RADIUS);
            double strength = SHOCKWAVE_STRENGTH * falloff;

            target.knockback(strength, -dx / dist, -dz / dist);
        }
    }

    public static final int KILL_THRESHOLD = 3;

    public static void register() {
        OnShieldBroken.EVENT.register((player, killer) -> {
            var gwShield = SREGameWorldComponent.KEY.get(player.level());
            if (gwShield != null && gwShield.isRole(player, ModRoles.JOSUKE)) {
                JosukeFistPunchHandler.applyShockwave(player);
            }
        });

        AllowPlayerPunching.EVENT.register((player) -> {
            if (player.hasEffect(ModEffects.SAFE_TIME)) {
                return false;
            }
            var gw2 = SREGameWorldComponent.KEY.get(player.level());
            if (gw2 != null && gw2.isRole(player, ModRoles.JOSUKE)) {
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.isEmpty()) {
                    if (player.getCooldowns().isOnCooldown(Items.BARRIER))
                        return false;
                    return true;
                }
            }
            return false;
        });

        AttackEntityCallback.EVENT.register(JosukeFistPunchHandler::onEntityDamaged);
    }

    public static InteractionResult onEntityDamaged(Player attacker, Level level, InteractionHand hand, Entity entity,
            EntityHitResult hitResult) {
        if (SRE.isLobby)
            return InteractionResult.PASS;
        var gameWorldComponent = SREGameWorldComponent.KEY.get(level);
        if (!GameUtils.isPlayerAliveAndSurvival(attacker))
            return InteractionResult.PASS;
        if (!(entity instanceof Player victim)) {
            return InteractionResult.PASS;
        }
        if (!GameUtils.isPlayerAliveAndSurvival(victim))
            return InteractionResult.PASS;
        if (gameWorldComponent == null || !gameWorldComponent.isRole(attacker, ModRoles.JOSUKE))
            return InteractionResult.PASS;
        if (victim.getCooldowns().isOnCooldown(Items.CLOCK)) {
            return InteractionResult.PASS;
        }
        ItemStack mainHand = attacker.getMainHandItem();
        if (!mainHand.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (attacker.getCooldowns().isOnCooldown(Items.BARRIER))
            return InteractionResult.PASS;
        if (level.isClientSide())
            return InteractionResult.SUCCESS;
        if (attacker.getUUID().equals(entity.getUUID())) {
            return InteractionResult.PASS;
        }
        attacker.getCooldowns().addCooldown(Items.BARRIER, 10);
        UUID attackerUUID = attacker.getUUID();
        UUID victimUUID = victim.getUUID();

        PunchRecord record = PUNCH_RECORDS.computeIfAbsent(attackerUUID, k -> new PunchRecord());

        if (!victimUUID.equals(record.targetUUID)) {
            record.targetUUID = victimUUID;
            record.count = 0;
        }

        record.count++;

        int remaining = KILL_THRESHOLD - record.count;
        victim.hurt(attacker.damageSources().playerAttack(attacker), 1.0F);

        if (record.count >= KILL_THRESHOLD) {
            PUNCH_RECORDS.remove(attackerUUID);

            GameUtils.killPlayer(victim, true, attacker, Noellesroles.id("hoan_meirin_attack"));

            sendActionBar(attacker,
                    Component.translatable("message.pathsrole.josuke.attack.3")
                            .withStyle(ChatFormatting.RED));

            return InteractionResult.SUCCESS_NO_ITEM_USED;
        } else {
            ChatFormatting color = remaining == 1 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
            sendActionBar(attacker,
                    Component.translatable("message.pathsrole.josuke.attack.general", Component
                            .literal("[" + record.count + "/" + KILL_THRESHOLD + "]")
                            .withStyle(ChatFormatting.WHITE))
                            .withStyle(color));
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }
    }

    private static void sendActionBar(Player player, Component message) {
        player.displayClientMessage(
                message,
                true
        );
    }

    private static class PunchRecord {
        UUID targetUUID = null;
        int count = 0;
    }
}