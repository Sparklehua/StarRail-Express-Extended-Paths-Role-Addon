package org.agmas.pathsrole.client.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.agmas.pathsrole.client.ShrineClientState;
import org.joml.Matrix4f;

public class ShrineGhostRenderer {

    private static long startTimeMs = 0;
    private static final long FALL_DURATION_MS = 10_000;
    private static boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public static void onGhostStart(long startMs,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     double playerStartY) {
        ShrineClientState.onGhostStart(startMs, minX, minY, minZ, maxX, maxY, maxZ, playerStartY);

        GhostMeshData.resetBakingState();
        GhostMeshData.loadStructure();

        startTimeMs = startMs;
        active = true;
    }

    public static void render(WorldRenderContext context) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        long elapsed = System.currentTimeMillis() - startTimeMs;

        if (elapsed > FALL_DURATION_MS + 500) {
            active = false;
            return;
        }

        float progress = Math.min(1.0f, (float) elapsed / (float) FALL_DURATION_MS);

        double playerY = mc.player.getY();
        double halfHeight = GhostMeshData.getStructureHalfHeight();

        if (halfHeight <= 0) {
            return;
        }

        double startY = playerY + 1.62 + 18.0 + halfHeight;
        double endY = playerY - 2.0 + halfHeight;
        double currentY = startY + (endY - startY) * (double) progress;

        double centerX = ShrineClientState.getShrineCenterX();
        double centerZ = ShrineClientState.getShrineCenterZ();

        if (Double.isNaN(centerX) || Double.isNaN(centerZ)
                || Double.isInfinite(centerX) || Double.isInfinite(centerZ)) {
            return;
        }

        Vec3 camPos = context.camera().getPosition();

        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.translate(
                (float)(centerX - camPos.x),
                (float)(currentY - camPos.y),
                (float)(centerZ - camPos.z));
        Matrix4f modelView = new Matrix4f(context.positionMatrix());
        modelView.mul(modelMatrix);

        GhostMeshData.render(modelView, context.projectionMatrix());
    }
}