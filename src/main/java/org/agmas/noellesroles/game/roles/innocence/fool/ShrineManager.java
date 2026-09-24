package org.agmas.noellesroles.game.roles.innocence.fool;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.agmas.noellesroles.init.ModEffects;
import org.agmas.pathsrole.PathsRoleMod;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ShrineManager {

    private static final Logger LOGGER = PathsRoleMod.LOGGER;

    public static final double SHRINE_X = 100.0;
    public static final double SHRINE_Y = 200;
    public static final double SHRINE_Z = 21000.0;

    static final Map<UUID, double[]> preShrinePositions = new HashMap<>();

    public static Map<UUID, double[]> getPreShrinePositions() {
        return preShrinePositions;
    }

    private static AABB shrineBounds = null;
    private static String shrineLastError = null;

    public static AABB getShrineBounds() {
        return shrineBounds;
    }

    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            ShrineSequence.tick();
            if (!preShrinePositions.isEmpty()) {
                TarotAssemblyManager.havingMeeting = true;
            }
        });
    }

    public static boolean isInShrine(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel sl)) return false;
        ensureShrineScene(sl);
        return isPlayerInShrineBounds(player);
    }

    static boolean isPlayerInShrineBounds(ServerPlayer player) {
        if (shrineBounds != null) {
            return shrineBounds.contains(player.position());
        }
        return false;
    }

    static void ensureShrineSceneBuilt(ServerLevel level) {
        ensureShrineScene(level);
    }

    public static void enterShrine(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        ensureShrineScene(serverLevel);

        if (shrineBounds == null) {
            String err = shrineLastError != null ? shrineLastError : "未知错误";
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c神社建造失败: " + err), false);
            return;
        }

        TarotAssemblyManager.havingMeeting = true;

        preShrinePositions.put(player.getUUID(), new double[] {
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        });

        player.addEffect(new MobEffectInstance(ModEffects.BLACK_MONITOR, 20 * 1, 0, false, false, false));

        player.stopSleeping();
        player.stopRiding();

        Vec3 spawnPos = ShrineSceneBuilder.getShrineSpawnPos();
        if (spawnPos != null) {
            player.teleportTo(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z,
                    Set.of(), ShrineSceneBuilder.getShrineSpawnYaw(), ShrineSceneBuilder.getShrineSpawnPitch());
        } else {
            player.teleportTo(serverLevel, SHRINE_X, SHRINE_Y + 1, SHRINE_Z,
                    Set.of(), player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
    }

    public static void leaveShrine(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.BLACK_MONITOR, 20 * 1, 0, false, false, false));
        leaveShrineInternal(player);
    }

    static void leaveShrineInternal(ServerPlayer player) {
        if (player == null) return;

        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        double[] pos = preShrinePositions.remove(player.getUUID());

        if (pos == null) {
            Vec3 spawnPos = Vec3.atCenterOf(serverLevel.getSharedSpawnPos());
            player.stopSleeping();
            player.stopRiding();
            player.teleportTo(serverLevel, spawnPos.x, spawnPos.y, spawnPos.z,
                    Set.of(), player.getYRot(), player.getXRot());
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            player.fallDistance = 0.0F;
            return;
        }

        player.stopSleeping();
        player.stopRiding();
        player.teleportTo(serverLevel, pos[0], pos[1], pos[2],
                Set.of(), (float) pos[3], (float) pos[4]);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
    }

    private static void ensureShrineScene(ServerLevel level) {
        if (shrineBounds != null) {
            boolean intact = isShrineIntact(level, shrineBounds);
            if (!intact) {
                LOGGER.info("[ShrineManager] Shrine was destroyed, rebuilding...");
                shrineBounds = null;
                shrineLastError = null;
            } else {
                return;
            }
        }

        LOGGER.info("[ShrineManager] Building shrine scene at ({}, {}, {})", SHRINE_X, SHRINE_Y, SHRINE_Z);

        try {
            BlockPos center = new BlockPos((int) SHRINE_X, (int) SHRINE_Y, (int) SHRINE_Z);
            ShrineSceneBuilder builder = new ShrineSceneBuilder(level);
            shrineBounds = builder.build(center);
            shrineLastError = null;
            LOGGER.info("[ShrineManager] Shrine scene built successfully. Bounds: {}", shrineBounds);
        } catch (Exception e) {
            shrineLastError = e.getMessage();
            LOGGER.error("[ShrineManager] FAILED to build shrine scene!", e);
        }
    }

    private static boolean isShrineIntact(ServerLevel level, AABB bounds) {
        int minX = (int) bounds.minX;
        int minY = (int) bounds.minY;
        int minZ = (int) bounds.minZ;
        int maxX = (int) bounds.maxX;
        int maxY = (int) bounds.maxY;
        int maxZ = (int) bounds.maxZ;

        int midX = (minX + maxX) / 2;
        int midZ = (minZ + maxZ) / 2;

        int solidCount = 0;
        int totalChecked = 0;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; y += 10) {
            checkPos.set(midX, y, midZ);
            if (!level.getBlockState(checkPos).isAir()) {
                solidCount++;
            }
            totalChecked++;
        }

        LOGGER.info("[ShrineManager] Shrine integrity check: {}/{} non-air at vertical center line", solidCount, totalChecked);

        return solidCount >= 2;
    }
}