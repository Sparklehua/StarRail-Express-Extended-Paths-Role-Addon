package org.agmas.pathsrole.game.roles.paths.abundance.pig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for pig form transform expiry management.
 * Used by PigEntityMixin internally and by non-mixin code (PigPlayerComponent, ModRoles).
 */
public class PigFormHelper {

    private static final Map<UUID, Long> TRANSFORM_EXPIRY = new HashMap<>();

    public static void markTransformExpiry(UUID uuid, long expiryTick) {
        if (uuid == null) return;
        TRANSFORM_EXPIRY.put(uuid, expiryTick);
    }

    public static void clearTransformExpiry(UUID uuid) {
        if (uuid == null) return;
        TRANSFORM_EXPIRY.remove(uuid);
    }

    public static Long getTransformExpiry(UUID uuid) {
        if (uuid == null) return null;
        return TRANSFORM_EXPIRY.get(uuid);
    }

    /**
     * 清除所有已过期的条目，防止内存泄漏。
     * 在 PigEntityMixin.tickPigFormExpiry 中每次 tick 调用。
     */
    public static void cleanExpired(long currentTick) {
        TRANSFORM_EXPIRY.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() < currentTick);
    }
}