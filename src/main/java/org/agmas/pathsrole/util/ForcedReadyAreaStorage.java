package org.agmas.pathsrole.util;

import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class ForcedReadyAreaStorage {
    @Nullable
    private static AABB forcedReadyArea = null;
    @Nullable
    private static AABB originalReadyArea = null;

    @Nullable
    public static AABB getForcedReadyArea() {
        return forcedReadyArea;
    }

    public static void setForcedReadyArea(@Nullable AABB area) {
        forcedReadyArea = area;
    }

    public static void clear() {
        forcedReadyArea = null;
        originalReadyArea = null;
    }

    public static boolean isForced() {
        return forcedReadyArea != null;
    }

    @Nullable
    public static AABB getOriginalReadyArea() {
        return originalReadyArea;
    }

    public static void setOriginalReadyArea(@Nullable AABB area) {
        originalReadyArea = area;
    }
}

