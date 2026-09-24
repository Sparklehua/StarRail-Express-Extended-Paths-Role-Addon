package org.agmas.pathsrole.client;

public class ShrineClientState {

    public static final long GHOST_FALL_MS = 10_000;
    public static final long ACTIVE_DURATION_MS = 60 * 1000;
    public static final long COOLDOWN_DURATION_MS = 200 * 1000;

    private static long ghostStartMs = 0;
    private static boolean ghostActive = false;

    private static double minX, minY, minZ;
    private static double maxX, maxY, maxZ;
    private static double playerStartY;

    public static void onGhostStart(long startMs,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     double playerStartY) {
        ghostStartMs = startMs;
        ghostActive = true;
        ShrineClientState.minX = minX;
        ShrineClientState.minY = minY;
        ShrineClientState.minZ = minZ;
        ShrineClientState.maxX = maxX;
        ShrineClientState.maxY = maxY;
        ShrineClientState.maxZ = maxZ;
        ShrineClientState.playerStartY = playerStartY;
    }

    public static double getShrineCenterX() {
        return (minX + maxX) / 2.0;
    }

    public static double getShrineCenterZ() {
        return (minZ + maxZ) / 2.0;
    }

    public static double getPlayerStartY() {
        return playerStartY;
    }

    public static boolean isShrineActive() {
        if (!ghostActive) return false;
        long now = System.currentTimeMillis();
        long shrineStartMs = ghostStartMs + GHOST_FALL_MS;
        long shrineEndMs = shrineStartMs + ACTIVE_DURATION_MS;
        return now >= shrineStartMs && now < shrineEndMs;
    }

    public static boolean isInShrineBounds(double x, double y, double z) {
        if (!isShrineActive()) return false;
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    public static boolean shouldApplyFog(double x, double y, double z) {
        return isInShrineBounds(x, y, z);
    }

    public static String getStatusText() {
        if (!ghostActive) return null;
        long now = System.currentTimeMillis();

        if (now < ghostStartMs + GHOST_FALL_MS) {
            long remaining = Math.max(0, (ghostStartMs + GHOST_FALL_MS - now) / 1000);
            return "§d神社降临中... §f" + remaining + "§d秒";
        }

        long shrineStartMs = ghostStartMs + GHOST_FALL_MS;
        long shrineEndMs = shrineStartMs + ACTIVE_DURATION_MS;

        if (now < shrineEndMs) {
            long remaining = Math.max(0, (shrineEndMs - now) / 1000);
            return "§d神社开放中... §f" + remaining + "§d秒";
        }

        long cooldownEndMs = shrineEndMs + COOLDOWN_DURATION_MS;
        if (now < cooldownEndMs) {
            long remaining = Math.max(0, (cooldownEndMs - now) / 1000);
            return "§7冷却中... §f" + remaining + "§7秒";
        }

        ghostActive = false;
        return null;
    }

    public static void onShrineEnd() {
        ghostActive = false;
    }
}