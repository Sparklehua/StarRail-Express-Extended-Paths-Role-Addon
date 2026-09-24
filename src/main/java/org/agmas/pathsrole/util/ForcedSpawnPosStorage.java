package org.agmas.pathsrole.util;

import io.wifi.starrailexpress.cca.AreasWorldComponent;
import org.jetbrains.annotations.Nullable;

public class ForcedSpawnPosStorage {
    @Nullable
    private static AreasWorldComponent.PosWithOrientation forcedSpawnPos = null;
    @Nullable
    private static AreasWorldComponent.PosWithOrientation originalSpawnPos = null;

    @Nullable
    public static AreasWorldComponent.PosWithOrientation getForcedSpawnPos() {
        return forcedSpawnPos;
    }

    public static void setForcedSpawnPos(@Nullable AreasWorldComponent.PosWithOrientation pos) {
        forcedSpawnPos = pos;
    }

    public static void clear() {
        forcedSpawnPos = null;
        originalSpawnPos = null;
    }

    public static boolean isForced() {
        return forcedSpawnPos != null;
    }

    @Nullable
    public static AreasWorldComponent.PosWithOrientation getOriginalSpawnPos() {
        return originalSpawnPos;
    }

    public static void setOriginalSpawnPos(@Nullable AreasWorldComponent.PosWithOrientation pos) {
        originalSpawnPos = pos;
    }
}

