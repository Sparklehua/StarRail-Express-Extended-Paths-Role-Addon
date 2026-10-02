package org.agmas.pathsrole.client.renderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class CelestialGrimoireRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("pathsrole", "textures/item/celestial_grimoire.png");
    private static final ResourceLocation TEXTURE_COVER =
            ResourceLocation.fromNamespaceAndPath("pathsrole", "textures/item/book_cover.png");

    private static final float DEG2RAD = (float) Math.PI / 180.0F;
    private static final float TOTAL_DURATION = 3.75F;
    private static final float OPEN_DURATION = 2.0F;

    private static CelestialGrimoireRenderer INSTANCE;

    public CelestialGrimoireRenderer() {
        INSTANCE = this;
    }

    private boolean playingOpen;
    private long openStartMs;
    private float animTime;
    private long lastFrameMs;

    public static void triggerOpenStatic() {
        if (INSTANCE != null) {
            INSTANCE.playingOpen = true;
            INSTANCE.openStartMs = System.currentTimeMillis();
        }
    }

    public static void resetToIdle() {
        if (INSTANCE != null) {
            INSTANCE.playingOpen = false;
        }
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {
        matrices.pushPose();

        matrices.mulPose(Axis.YP.rotationDegrees(180));
        matrices.translate(-1.6F, -2.2F, 0);

        VertexConsumer v = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        VertexConsumer vCover = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE_COVER));
        PoseStack.Pose last = matrices.last();

        float animT = getAnimationTime();

        float[] r = computeAnimation(animT);

        // --- Render hierarchy ---
        // book_root: rotation Z + position Y
        Matrix4f bookMatrix = new Matrix4f(last.pose());
        bookMatrix.translate(0, r[1], 0);
        applyRotation(bookMatrix, 0, 0, r[0]);

        for (int ei : BONE_ELEMENTS[0]) {
            renderElement(bookMatrix, v, light, ei);
        }

        // cover_front: from book_root pivot, offset to its own pivot, rotate Y
        Matrix4f cfMatrix = new Matrix4f(bookMatrix);
        cfMatrix.translate(CF_OFFSET_X, CF_OFFSET_Y, CF_OFFSET_Z);
        applyRotation(cfMatrix, 0, r[2], 0);
        for (int ei : BONE_ELEMENTS[1]) {
            if (ei == 5) {
                renderFrontCover(cfMatrix, v, vCover, light);
            } else {
                renderElement(cfMatrix, v, light, ei);
            }
        }

        // page1-4: from book_root pivot, offset to their pivots, rotate Y
        for (int pi = 0; pi < 4; pi++) {
            Matrix4f pageMatrix = new Matrix4f(bookMatrix);
            pageMatrix.translate(PAGE_OFFSET_X, PAGE_OFFSET_Y, PAGE_OFFSET_Z);
            applyRotation(pageMatrix, 0, r[3 + pi], 0);
            for (int ei : BONE_ELEMENTS[2 + pi]) {
                renderElement(pageMatrix, v, light, ei);
            }
        }

        matrices.popPose();
    }

    /* ---------- animation time (delta-based, auto-reset open trigger) ---------- */

    private float getAnimationTime() {
        long now = System.currentTimeMillis();

        if (playingOpen) {
            float elapsed = (now - openStartMs) / 1000.0F;
            if (elapsed < TOTAL_DURATION) {
                animTime = elapsed;
            } else {
                playingOpen = false;
                animTime = 0;
            }
        } else {
            animTime = 0;
        }
        return animTime;
    }

    /* ---------- keyframe evaluation ---------- */

    private float[] computeAnimation(float t) {
        float bRz = 0, bPy = 0;
        if (KF_BOOK_ROOT_ROT != null) bRz = lerpKf(KF_BOOK_ROOT_ROT, t, 0);
        if (KF_BOOK_ROOT_POS != null) bPy = lerpKf(KF_BOOK_ROOT_POS, t, 1);

        float cfRy = 0;
        if (KF_COVER_FRONT != null) cfRy = lerpKf(KF_COVER_FRONT, t, 1);

        float p1Ry = 0, p2Ry = 0, p3Ry = 0, p4Ry = 0;
        if (KF_PAGE1 != null) p1Ry = lerpKf(KF_PAGE1, t, 1);
        if (KF_PAGE2 != null) p2Ry = lerpKf(KF_PAGE2, t, 1);
        if (KF_PAGE3 != null) p3Ry = lerpKf(KF_PAGE3, t, 1);
        if (KF_PAGE4 != null) p4Ry = lerpKf(KF_PAGE4, t, 1);

        return new float[]{bRz, bPy, cfRy, p1Ry, p2Ry, p3Ry, p4Ry};
    }

    private static float lerpKf(float[] kf, float t, int offset) {
        if (kf == null) return 0;
        int n = kf.length / 4;
        if (n == 0) return 0;
        float totalTime = kf[(n - 1) * 4];
        t = t % totalTime;
        if (t < 0) t += totalTime;
        for (int i = 0; i < n - 1; i++) {
            float t0 = kf[i * 4];
            float t1 = kf[(i + 1) * 4];
            if (t >= t0 && t <= t1) {
                float v0 = kf[i * 4 + 1 + offset];
                float v1 = kf[(i + 1) * 4 + 1 + offset];
                float alpha = (t - t0) / (t1 - t0);
                return v0 + (v1 - v0) * alpha;
            }
        }
        return kf[1 + offset];
    }

    /* ---------- matrix helpers ---------- */

    private static void applyRotation(Matrix4f m, float rx, float ry, float rz) {
        if (rx == 0 && ry == 0 && rz == 0) return;
        Quaternionf q = new Quaternionf()
                .rotateZ(rz * DEG2RAD)
                .rotateY(ry * DEG2RAD)
                .rotateX(rx * DEG2RAD);
        m.rotate(q);
    }

    /* ---------- element rendering ---------- */

    private void renderElement(Matrix4f matrix, VertexConsumer v, int light, int ei) {
        float[] e = ELEMENTS[ei];
        float x1 = e[0], y1 = e[1], z1 = e[2];
        float x2 = e[3], y2 = e[4], z2 = e[5];
        float ox = e[6], oy = e[7], oz = e[8];
        float rZ = e[9];
        float u0 = e[10] / 128.0F;
        float v0 = e[11] / 128.0F;
        float u1 = e[12] / 128.0F;
        float v1 = e[13] / 128.0F;

        Matrix4f m = new Matrix4f(matrix);
        if (rZ != 0) {
            m.translate(ox, oy, oz);
            m.rotateZ(rZ * DEG2RAD);
            m.translate(-ox, -oy, -oz);
        }

        // +Z (south)
        quad(m, v, light, x2, y2, z2, x2, y1, z2, x1, y1, z2, x1, y2, z2, u0, v0, u1, v1, 0, 0, 1);
        // -Z (north)
        quad(m, v, light, x1, y2, z1, x1, y1, z1, x2, y1, z1, x2, y2, z1, u0, v0, u1, v1, 0, 0, -1);
        // +X (east)
        quad(m, v, light, x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1, u0, v0, u1, v1, 1, 0, 0);
        // -X (west)
        quad(m, v, light, x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2, u0, v0, u1, v1, -1, 0, 0);
        // +Y (up)
        quad(m, v, light, x2, y2, z2, x2, y2, z1, x1, y2, z1, x1, y2, z2, u0, v0, u1, v1, 0, 1, 0);
        // -Y (down)
        quad(m, v, light, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, u0, v0, u1, v1, 0, -1, 0);
    }

    /* ---------- front_cover element: north face uses book_cover.png, others use celestial_grimoire.png ---------- */

    private void renderFrontCover(Matrix4f matrix, VertexConsumer v, VertexConsumer vCover, int light) {
        float[] e = ELEMENTS[5];
        float x1 = e[0], y1 = e[1], z1 = e[2];
        float x2 = e[3], y2 = e[4], z2 = e[5];

        // +Z (south) celestial_grimoire.png UV [16,0,48,32] / 128
        quad(matrix, v, light, x2, y2, z2, x2, y1, z2, x1, y1, z2, x1, y2, z2,
                0.125f, 0f, 0.375f, 0.25f, 0, 0, 1);
        // -Z (north) book_cover.png full texture [0,0,64,64] / 64
        quad(matrix, vCover, light, x1, y2, z1, x1, y1, z1, x2, y1, z1, x2, y2, z1,
                0f, 0f, 1f, 1f, 0, 0, -1);
        // +X (east) celestial_grimoire.png UV [0,0,8,8] / 128
        quad(matrix, v, light, x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1,
                0f, 0f, 0.0625f, 0.0625f, 1, 0, 0);
        // -X (west) celestial_grimoire.png UV [0,0,8,8] / 128
        quad(matrix, v, light, x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2,
                0f, 0f, 0.0625f, 0.0625f, -1, 0, 0);
        // +Y (up) celestial_grimoire.png UV [0,0,8,8] / 128
        quad(matrix, v, light, x2, y2, z2, x2, y2, z1, x1, y2, z1, x1, y2, z2,
                0f, 0f, 0.0625f, 0.0625f, 0, 1, 0);
        // -Y (down) celestial_grimoire.png UV [0,0,8,8] / 128
        quad(matrix, v, light, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2,
                0f, 0f, 0.0625f, 0.0625f, 0, -1, 0);
    }

    private static void quad(Matrix4f m, VertexConsumer v, int light,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4,
            float u0, float v0, float u1, float v1,
            float nx, float ny, float nz) {
        v.addVertex(m, x1, y1, z1).setUv(u1, v1).setColor(-1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        v.addVertex(m, x2, y2, z2).setUv(u1, v0).setColor(-1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        v.addVertex(m, x3, y3, z3).setUv(u0, v0).setColor(-1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
        v.addVertex(m, x4, y4, z4).setUv(u0, v1).setColor(-1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
    }

    /* ========== book_root → cover_front / page offsets ========== */

    // cover_front pivot [-7,8,-1.3]  minus  book_root pivot [0,8,0]  →  [-0.4375, 0, -0.08125]
    private static final float CF_OFFSET_X = -0.4375F;
    private static final float CF_OFFSET_Y =  0.0F;
    private static final float CF_OFFSET_Z = -0.08125F;

    // page1-4 pivot [-7,8,-0.6]  minus  book_root pivot [0,8,0]  →  [-0.4375, 0, -0.0375]
    private static final float PAGE_OFFSET_X = -0.4375F;
    private static final float PAGE_OFFSET_Y =  0.0F;
    private static final float PAGE_OFFSET_Z = -0.0375F;

    /* ========== elements (local bone coords in MC units, /16) ========== */
    // [x1, y1, z1, x2, y2, z2, rot_origin_x, rot_origin_y, rot_origin_z, rotZ_deg, u0, v0, u1, v1]

    private static final float[][] ELEMENTS = {
        //  0  back_cover     book_root    [-7,0,0.8] → [7,16,1.8]
        { -0.437500f, -0.500000f, 0.050000f, 0.437500f, 0.500000f, 0.112500f, 0.500000f, 0.000000f, 0.500000f, 0f, 0f, 32f, 64f, 96f },
        //  1  spine          book_root    [-8.1,0,-1.8] → [-7.1,16,1.55]
        { -0.506250f, -0.500000f, -0.112500f, -0.443750f, 0.500000f, 0.096875f, 0.500000f, 0.000000f, 0.500000f, 0f, 96f, 0f, 112f, 32f },
        //  2  pages_block    book_root    [-7.1,0.7,-0.4] → [6.8,15.3,0.8]
        { -0.443750f, -0.456250f, -0.025000f, 0.425000f, 0.456250f, 0.050000f, 0.500000f, 0.000000f, 0.500000f, 0f, 64f, 0f, 96f, 32f },
        //  3  bc_tr          book_root    [5.4,14.4,0.65] → [7.1,16.1,1.95]
        { 0.337500f, 0.400000f, 0.040625f, 0.443750f, 0.506250f, 0.121875f, 0.500000f, 0.000000f, 0.500000f, 0f, 8f, 0f, 16f, 8f },
        //  4  bc_br          book_root    [5.4,-0.1,0.65] → [7.1,1.6,1.95]
        { 0.337500f, -0.506250f, 0.040625f, 0.443750f, -0.400000f, 0.121875f, 0.500000f, 0.000000f, 0.500000f, 0f, 8f, 0f, 16f, 8f },

        //  5  front_cover    cover_front  [-7,0,-1.8] → [7,16,-0.8]
        { 0.000000f, -0.500000f, -0.031250f, 0.875000f, 0.500000f, 0.031250f, 0.937500f, 0.000000f, 0.581250f, 0f, 16f, 0f, 48f, 32f },
        //  6  fc_tl          cover_front  [-0.1,14.4,-0.65] → [1.6,16.1,0.65]
        { -0.006250f, 0.400000f, -0.040625f, 0.100000f, 0.506250f, 0.040625f, 0.937500f, 0.000000f, 0.581250f, 0f, 8f, 0f, 16f, 8f },
        //  7  fc_tr          cover_front  [12.4,14.4,-0.65] → [14.1,16.1,0.65]
        { 0.775000f, 0.400000f, -0.040625f, 0.881250f, 0.506250f, 0.040625f, 0.937500f, 0.000000f, 0.581250f, 0f, 8f, 0f, 16f, 8f },
        //  8  fc_bl          cover_front  [-0.1,-0.1,-0.65] → [1.6,1.6,0.65]
        { -0.006250f, -0.506250f, -0.040625f, 0.100000f, -0.400000f, 0.040625f, 0.937500f, 0.000000f, 0.581250f, 0f, 8f, 0f, 16f, 8f },
        //  9  fc_br          cover_front  [12.4,-0.1,-0.65] → [14.1,1.6,0.65]
        { 0.775000f, -0.506250f, -0.040625f, 0.881250f, -0.400000f, 0.040625f, 0.937500f, 0.000000f, 0.581250f, 0f, 8f, 0f, 16f, 8f },
        // 10  clasp          cover_front  [13.9,6.9,-0.75] → [14.35,9.1,0.75]
        { 0.868750f, -0.068750f, -0.046875f, 0.896875f, 0.068750f, 0.046875f, 0.937500f, 0.000000f, 0.581250f, 0f, 8f, 0f, 16f, 8f },

        // 11  page1          page1        [-6.8,0.9,-0.2] → [6.7,15.1,-0.1]
        { 0.012500f, -0.443750f, -0.012500f, 0.856250f, 0.443750f, -0.006250f, 0.937500f, 0.000000f, 0.537500f, 0f, 64f, 0f, 96f, 32f },
        // 12  page2          page2        [-6.8,0.9,-0.1] → [6.7,15.1,0]
        { 0.012500f, -0.443750f, -0.006250f, 0.856250f, 0.443750f, 0.000000f, 0.937500f, 0.000000f, 0.537500f, 0f, 64f, 0f, 96f, 32f },
        // 13  page3          page3        [-6.8,0.9,0] → [6.7,15.1,0.1]
        { 0.012500f, -0.443750f, 0.000000f, 0.856250f, 0.443750f, 0.006250f, 0.937500f, 0.000000f, 0.537500f, 0f, 64f, 0f, 96f, 32f },
        // 14  page4          page4        [-6.8,0.9,0.1] → [6.7,15.1,0.2]
        { 0.012500f, -0.443750f, 0.006250f, 0.856250f, 0.443750f, 0.012500f, 0.937500f, 0.000000f, 0.537500f, 0f, 64f, 0f, 96f, 32f },
    };

    /* ========== bone → element index map ========== */

    private static final int[][] BONE_ELEMENTS = {
        {0, 1, 2, 3, 4},      // book_root
        {5, 6, 7, 8, 9, 10},  // cover_front
        {11},                  // page1
        {12},                  // page2
        {13},                  // page3
        {14},                  // page4
    };

    /* ========== keyframes [time, rx, ry, rz] or [time, px, py, pz] ========== */

    private static final float[] KF_BOOK_ROOT_ROT = {
        0f, 0f, 0f, 0f,
        0.4688f, 0f, 0f, 1.0f,
        0.9375f, 0f, 0f, 0f,
        1.4062f, 0f, 0f, -1.0f,
        1.875f, 0f, 0f, 0f,
        2.3438f, 0f, 0f, 1.0f,
        2.8125f, 0f, 0f, 0f,
        3.2812f, 0f, 0f, -1.0f,
        3.75f, 0f, 0f, 0f,
    };

    private static final float[] KF_BOOK_ROOT_POS = {
        0f, 0f, 0f, 0f,
        0.4688f, 0f, 0.25f, 0f,
        0.9375f, 0f, 0f, 0f,
        1.4062f, 0f, -0.25f, 0f,
        1.875f, 0f, 0f, 0f,
        2.3438f, 0f, 0.25f, 0f,
        2.8125f, 0f, 0f, 0f,
        3.2812f, 0f, -0.25f, 0f,
        3.75f, 0f, 0f, 0f,
    };

    private static final float[] KF_COVER_FRONT = {
        0f, 0f, 0f, 0f,
        1.6f, 0f, 150f, 0f,
        2.5f, 0f, 150f, 0f,
        3.75f, 0f, 0f, 0f,
    };

    private static final float[] KF_PAGE1 = {
        0f, 0f, 0f, 0f,
        1.7f, 0f, 130f, 0f,
        2.5f, 0f, 130f, 0f,
        3.55f, 0f, 0f, 0f,
    };

    private static final float[] KF_PAGE2 = {
        0f, 0f, 0f, 0f,
        1.8f, 0f, 100f, 0f,
        2.5f, 0f, 100f, 0f,
        3.35f, 0f, 0f, 0f,
    };

    private static final float[] KF_PAGE3 = {
        0f, 0f, 0f, 0f,
        1.9f, 0f, 70f, 0f,
        2.5f, 0f, 70f, 0f,
        3.1f, 0f, 0f, 0f,
    };

    private static final float[] KF_PAGE4 = {
        0f, 0f, 0f, 0f,
        2.0f, 0f, 40f, 0f,
        2.5f, 0f, 40f, 0f,
        2.9f, 0f, 0f, 0f,
    };
}