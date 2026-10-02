package org.agmas.pathsrole.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.client.SREClient;
import org.agmas.pathsrole.init.ModRoles;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class ShionButterflyHatLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation HAT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("pathsrole", "textures/entity/shion_butterfly_hat.png");
    private static final float PX = 1.0F / 16.0F;
    private static final float DEG2RAD = (float) Math.PI / 180.0F;
    private static final float IDLE_DURATION = 3.2F;

    private final PlayerModel<AbstractClientPlayer> model;

    public ShionButterflyHatLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        this.model = parent.getModel();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
            AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (player == null || poseStack == null || bufferSource == null) return;
        if (this.model == null || this.model.head == null) return;
        if (SREClient.gameComponent == null) return;

        SRERole role = SREClient.gameComponent.getRole(player.getUUID());
        if (role == null || !role.identifier().equals(ModRoles.SHION_ID)) return;

        try {
            poseStack.pushPose();

            this.model.head.translateAndRotate(poseStack);

            poseStack.scale(1, -1, 1);
            poseStack.translate(0, 0.3F, 0.44F);
            poseStack.scale(1.3F, 1.3F, 1.3F);

            VertexConsumer v = bufferSource.getBuffer(RenderType.entityCutoutNoCull(HAT_TEXTURE));
            if (v == null) {
                poseStack.popPose();
                return;
            }

            PoseStack.Pose last = poseStack.last();
            if (last == null) {
                poseStack.popPose();
                return;
            }

            float animTime = ((ageInTicks + partialTick) * 0.05F) % IDLE_DURATION;

            renderAll(last.pose(), v, packedLight, animTime);

            poseStack.popPose();
        } catch (Exception e) {
            // silently ignore rendering errors to prevent crashes
        }
    }

    private void renderAll(Matrix4f matrix, VertexConsumer v, int light, float t) {
        float[] r = computeAnimation(t);

        // bow_root (Y rotation only, from r[0], r[1], r[2])
        Matrix4f bow = new Matrix4f(matrix);
        applyRotation(bow, r[0], r[1], r[2]);

        // knot: element 0, inherits bow_root rotation
        for (int ei : BONE_ELEMENTS[7]) {
            renderElement(bow, v, light, ei);
        }

        // loop_L: rotation r[3,4,5], scale r[6,7,8]
        Matrix4f loopL = new Matrix4f(bow);
        applyScale(loopL, r[6], r[7], r[8]);
        applyRotation(loopL, r[3], r[4], r[5]);
        for (int ei : BONE_ELEMENTS[1]) {
            renderElement(loopL, v, light, ei);
        }

        // loop_R: rotation r[9,10,11], scale r[12,13,14]
        Matrix4f loopR = new Matrix4f(bow);
        applyScale(loopR, r[12], r[13], r[14]);
        applyRotation(loopR, r[9], r[10], r[11]);
        for (int ei : BONE_ELEMENTS[2]) {
            renderElement(loopR, v, light, ei);
        }

        // tail_L: rotation r[15,16,17], element 11 (tailL_up)
        Matrix4f tailL = new Matrix4f(bow);
        applyRotation(tailL, r[15], r[16], r[17]);
        for (int ei : BONE_ELEMENTS[3]) {
            renderElement(tailL, v, light, ei);
        }

        // tailL_low: rotation r[21,22,23], elements 12,13,14 under tail_L
        Matrix4f tailLLow = new Matrix4f(tailL);
        applyRotation(tailLLow, r[21], r[22], r[23]);
        for (int ei : BONE_ELEMENTS[5]) {
            renderElement(tailLLow, v, light, ei);
        }

        // tail_R: rotation r[18,19,20], element 15 (tailR_up)
        Matrix4f tailR = new Matrix4f(bow);
        applyRotation(tailR, r[18], r[19], r[20]);
        for (int ei : BONE_ELEMENTS[4]) {
            renderElement(tailR, v, light, ei);
        }

        // tailR_low: rotation r[24,25,26], elements 16,17,18 under tail_R
        Matrix4f tailRLow = new Matrix4f(tailR);
        applyRotation(tailRLow, r[24], r[25], r[26]);
        for (int ei : BONE_ELEMENTS[6]) {
            renderElement(tailRLow, v, light, ei);
        }
    }

    private static void applyRotation(Matrix4f m, float rx, float ry, float rz) {
        if (rx == 0 && ry == 0 && rz == 0) return;
        Quaternionf q = new Quaternionf()
                .rotateZ(rz * DEG2RAD)
                .rotateY(ry * DEG2RAD)
                .rotateX(rx * DEG2RAD);
        m.rotate(q);
    }

    private static void applyScale(Matrix4f m, float sx, float sy, float sz) {
        if (sx == 1 && sy == 1 && sz == 1) return;
        m.scale(sx, sy, sz);
    }

    private void renderElement(Matrix4f matrix, VertexConsumer v, int light, int ei) {
        float[] e = ELEMENTS[ei];
        float x1 = e[0], y1 = e[1], z1 = e[2];
        float x2 = e[3], y2 = e[4], z2 = e[5];
        float ox = e[6], oy = e[7], oz = e[8];
        float rZ = e[9];
        float su0 = e[10], sv0 = e[11], su1 = e[12], sv1 = e[13];

        Matrix4f m = new Matrix4f(matrix);

        if (rZ != 0) {
            m.translate(ox, oy, oz);
            m.rotateZ(rZ * DEG2RAD);
            m.translate(-ox, -oy, -oz);
        }

        float texW = 64.0F;
        float texH = 64.0F;

        float u0 = su0 / texW;
        float v0 = sv0 / texH;
        float u1 = su1 / texW;
        float v1 = sv1 / texH;

        // +Z face (south = front)
        vertex(m, v, light, x2, y2, z2, u1, v1, 0, 0, 1);
        vertex(m, v, light, x2, y1, z2, u1, v0, 0, 0, 1);
        vertex(m, v, light, x1, y1, z2, u0, v0, 0, 0, 1);
        vertex(m, v, light, x1, y2, z2, u0, v1, 0, 0, 1);

        // -Z face (north = back)
        vertex(m, v, light, x1, y2, z1, u0, v1, 0, 0, -1);
        vertex(m, v, light, x1, y1, z1, u0, v0, 0, 0, -1);
        vertex(m, v, light, x2, y1, z1, u1, v0, 0, 0, -1);
        vertex(m, v, light, x2, y2, z1, u1, v1, 0, 0, -1);

        // +X face (east)
        vertex(m, v, light, x2, y2, z2, u0, v1, 1, 0, 0);
        vertex(m, v, light, x2, y1, z2, u0, v0, 1, 0, 0);
        vertex(m, v, light, x2, y1, z1, u1, v0, 1, 0, 0);
        vertex(m, v, light, x2, y2, z1, u1, v1, 1, 0, 0);

        // -X face (west)
        vertex(m, v, light, x1, y2, z1, u0, v1, -1, 0, 0);
        vertex(m, v, light, x1, y1, z1, u0, v0, -1, 0, 0);
        vertex(m, v, light, x1, y1, z2, u1, v0, -1, 0, 0);
        vertex(m, v, light, x1, y2, z2, u1, v1, -1, 0, 0);

        // +Y face (up)
        vertex(m, v, light, x2, y2, z2, u0, v1, 0, 1, 0);
        vertex(m, v, light, x2, y2, z1, u0, v0, 0, 1, 0);
        vertex(m, v, light, x1, y2, z1, u1, v0, 0, 1, 0);
        vertex(m, v, light, x1, y2, z2, u1, v1, 0, 1, 0);

        // -Y face (down)
        vertex(m, v, light, x1, y1, z2, u0, v1, 0, -1, 0);
        vertex(m, v, light, x1, y1, z1, u0, v0, 0, -1, 0);
        vertex(m, v, light, x2, y1, z1, u1, v0, 0, -1, 0);
        vertex(m, v, light, x2, y1, z2, u1, v1, 0, -1, 0);
    }

    private static void vertex(Matrix4f m, VertexConsumer v, int light,
            float x, float y, float z, float u, float vv, float nx, float ny, float nz) {
        v.addVertex(m, x, y, z)
                .setUv(u, vv)
                .setColor(-1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    private float[] computeAnimation(float t) {
        float bRx, bRy, bRz;
        bRx = bRy = bRz = 0;
        if (KF_BOW_ROOT != null) {
            bRx = lerpKf(KF_BOW_ROOT, t, 0);
            bRy = lerpKf(KF_BOW_ROOT, t, 1);
            bRz = lerpKf(KF_BOW_ROOT, t, 2);
        }

        float lRx, lRy, lRz;
        lRx = lRy = lRz = 0;
        if (KF_LOOP_L != null) {
            lRx = lerpKf(KF_LOOP_L, t, 0);
            lRy = lerpKf(KF_LOOP_L, t, 1);
            lRz = lerpKf(KF_LOOP_L, t, 2);
        }
        float lSx = 1, lSy = 1, lSz = 1;
        if (KF_SCALE_LOOP_L != null) {
            lSx = lerpKf(KF_SCALE_LOOP_L, t, 0);
            lSy = lerpKf(KF_SCALE_LOOP_L, t, 1);
            lSz = lerpKf(KF_SCALE_LOOP_L, t, 2);
        }

        float rRx, rRy, rRz;
        rRx = rRy = rRz = 0;
        if (KF_LOOP_R != null) {
            rRx = lerpKf(KF_LOOP_R, t, 0);
            rRy = lerpKf(KF_LOOP_R, t, 1);
            rRz = lerpKf(KF_LOOP_R, t, 2);
        }
        float rSx = 1, rSy = 1, rSz = 1;
        if (KF_SCALE_LOOP_R != null) {
            rSx = lerpKf(KF_SCALE_LOOP_R, t, 0);
            rSy = lerpKf(KF_SCALE_LOOP_R, t, 1);
            rSz = lerpKf(KF_SCALE_LOOP_R, t, 2);
        }

        float tlRx, tlRy, tlRz;
        tlRx = tlRy = tlRz = 0;
        if (KF_TAIL_L != null) {
            tlRx = lerpKf(KF_TAIL_L, t, 0);
            tlRy = lerpKf(KF_TAIL_L, t, 1);
            tlRz = lerpKf(KF_TAIL_L, t, 2);
        }

        float trRx, trRy, trRz;
        trRx = trRy = trRz = 0;
        if (KF_TAIL_R != null) {
            trRx = lerpKf(KF_TAIL_R, t, 0);
            trRy = lerpKf(KF_TAIL_R, t, 1);
            trRz = lerpKf(KF_TAIL_R, t, 2);
        }

        float tllRx, tllRy, tllRz;
        tllRx = tllRy = tllRz = 0;
        if (KF_TAILL_LOW != null) {
            tllRx = lerpKf(KF_TAILL_LOW, t, 0);
            tllRy = lerpKf(KF_TAILL_LOW, t, 1);
            tllRz = lerpKf(KF_TAILL_LOW, t, 2);
        }

        float trlRx, trlRy, trlRz;
        trlRx = trlRy = trlRz = 0;
        if (KF_TAILR_LOW != null) {
            trlRx = lerpKf(KF_TAILR_LOW, t, 0);
            trlRy = lerpKf(KF_TAILR_LOW, t, 1);
            trlRz = lerpKf(KF_TAILR_LOW, t, 2);
        }

        return new float[]{
            bRx, bRy, bRz,
            lRx, lRy, lRz, lSx, lSy, lSz,
            rRx, rRy, rRz, rSx, rSy, rSz,
            tlRx, tlRy, tlRz,
            trRx, trRy, trRz,
            tllRx, tllRy, tllRz,
            trlRx, trlRy, trlRz,
        };
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

    // Element data: [x1, y1, z1, x2, y2, z2, ox, oy, oz, rotZ, southU0, southV0, southU1, southV1]
    // Generated from BBModel: coordinates converted to Minecraft head space
    // mcX = bbX/16, mcY = (bbY-28)/16, mcZ = -(bbZ-4)/16
    private static final float[][] ELEMENTS = {
        {-0.081250f, 0.000000f, 0.031250f, 0.081250f, 0.187500f, -0.106250f, 0.000000f, 0.093750f, -0.031250f, 0f, 32f, 16f, 40f, 24f},
        {-0.450000f, -0.043750f, 0.006250f, -0.100000f, 0.268750f, -0.081250f, -0.068750f, 0.093750f, -0.031250f, -16f, 0f, 0f, 16f, 16f},
        {-0.387500f, 0.268750f, 0.000000f, -0.150000f, 0.337500f, -0.075000f, -0.068750f, 0.093750f, -0.031250f, -16f, 0f, 34f, 16f, 39f},
        {-0.393750f, -0.112500f, 0.000000f, -0.156250f, -0.043750f, -0.075000f, -0.068750f, 0.093750f, -0.031250f, -16f, 0f, 41f, 16f, 46f},
        {-0.493750f, 0.012500f, 0.000000f, -0.450000f, 0.225000f, -0.075000f, -0.068750f, 0.093750f, -0.031250f, -16f, 18f, 32f, 22f, 48f},
        {-0.125000f, 0.037500f, 0.025000f, -0.050000f, 0.162500f, -0.093750f, -0.068750f, 0.093750f, -0.031250f, -16f, 16f, 32f, 32f, 48f},
        {0.100000f, -0.043750f, 0.006250f, 0.450000f, 0.268750f, -0.081250f, 0.068750f, 0.093750f, -0.031250f, 16f, 32f, 0f, 48f, 16f},
        {0.150000f, 0.268750f, 0.000000f, 0.387500f, 0.337500f, -0.075000f, 0.068750f, 0.093750f, -0.031250f, 16f, 0f, 34f, 16f, 39f},
        {0.156250f, -0.112500f, 0.000000f, 0.393750f, -0.043750f, -0.075000f, 0.068750f, 0.093750f, -0.031250f, 16f, 0f, 41f, 16f, 46f},
        {0.450000f, 0.012500f, 0.000000f, 0.493750f, 0.225000f, -0.075000f, 0.068750f, 0.093750f, -0.031250f, 16f, 26f, 32f, 30f, 48f},
        {0.050000f, 0.037500f, 0.025000f, 0.125000f, 0.162500f, -0.093750f, 0.068750f, 0.093750f, -0.031250f, 16f, 16f, 32f, 32f, 48f},
        {-0.150000f, -0.237500f, 0.006250f, -0.018750f, 0.012500f, -0.068750f, -0.081250f, 0.012500f, -0.031250f, -10f, 48f, 0f, 64f, 16f},
        {-0.193364f, -0.470659f, 0.006250f, -0.062114f, -0.233159f, -0.068750f, -0.127739f, -0.233159f, -0.031250f, -12f, 0f, 16f, 16f, 32f},
        {-0.242744f, -0.652969f, 0.006250f, -0.180244f, -0.465469f, -0.068750f, -0.177119f, -0.465469f, -0.031250f, -22f, 16f, 16f, 21f, 32f},
        {-0.180244f, -0.565469f, 0.006250f, -0.111494f, -0.465469f, -0.068750f, -0.177119f, -0.465469f, -0.031250f, -22f, 27f, 16f, 32f, 32f},
        {0.018750f, -0.237500f, 0.006250f, 0.150000f, 0.012500f, -0.068750f, 0.081250f, 0.012500f, -0.031250f, 10f, 0f, 16f, 16f, 32f},
        {0.062114f, -0.470659f, 0.006250f, 0.193364f, -0.233159f, -0.068750f, 0.127739f, -0.233159f, -0.031250f, 12f, 48f, 0f, 64f, 16f},
        {0.111494f, -0.565469f, 0.006250f, 0.173994f, -0.465469f, -0.068750f, 0.177119f, -0.465469f, -0.031250f, 22f, 27f, 16f, 32f, 32f},
        {0.173994f, -0.652969f, 0.006250f, 0.242744f, -0.465469f, -0.068750f, 0.177119f, -0.465469f, -0.031250f, 22f, 16f, 16f, 21f, 32f},
    };

    // Bone element indices
    private static final int[][] BONE_ELEMENTS = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18},
        {1, 2, 3, 4, 5},
        {6, 7, 8, 9, 10},
        {11},
        {15},
        {12, 13, 14},
        {16, 17, 18},
        {0},
    };

    // idle_sway animation keyframes (3.2s loop)
    // Format: [time0, rx0, ry0, rz0, time1, rx1, ry1, rz1, ...]

    private static final float[] KF_BOW_ROOT = {
        0f, 0f, 0f, 0f, 0.4f, 0f, -0.849f, 0f, 0.8f, 0f, -1.2f, 0f,
        1.2f, 0f, -0.849f, 0f, 1.6f, 0f, 0f, 0f, 2.0f, 0f, 0.849f, 0f,
        2.4f, 0f, 1.2f, 0f, 2.8f, 0f, 0.849f, 0f, 3.2f, 0f, 0f, 0f,
    };

    private static final float[] KF_LOOP_L = {
        0f, -0.847f, 0f, 1.819f, 0.4f, -1.474f, 0f, 0.697f,
        0.8f, -1.238f, 0f, -0.832f, 1.2f, -0.277f, 0f, -1.874f,
        1.6f, 0.847f, 0f, -1.819f, 2.0f, 1.474f, 0f, -0.697f,
        2.4f, 1.238f, 0f, 0.832f, 2.8f, 0.277f, 0f, 1.874f,
        3.2f, -0.847f, 0f, 1.819f,
    };

    private static final float[] KF_LOOP_R = {
        0f, 0.847f, 0f, 1.819f, 0.4f, 1.474f, 0f, 0.697f,
        0.8f, 1.238f, 0f, -0.832f, 1.2f, 0.277f, 0f, -1.874f,
        1.6f, -0.847f, 0f, -1.819f, 2.0f, -1.474f, 0f, -0.697f,
        2.4f, -1.238f, 0f, 0.832f, 2.8f, -0.277f, 0f, 1.874f,
        3.2f, 0.847f, 0f, 1.819f,
    };

    private static final float[] KF_TAIL_L = {
        0f, 0f, 0f, 3.372f, 0.4f, -3.536f, 0f, 3.047f,
        0.8f, -5f, 0f, 0.936f, 1.2f, -3.536f, 0f, -1.723f,
        1.6f, 0f, 0f, -3.372f, 2.0f, 3.536f, 0f, -3.047f,
        2.4f, 5f, 0f, -0.936f, 2.8f, 3.536f, 0f, 1.723f,
        3.2f, 0f, 0f, 3.372f,
    };

    private static final float[] KF_TAIL_R = {
        0f, 0f, 0f, 3.372f, 0.4f, 3.536f, 0f, 3.047f,
        0.8f, 5f, 0f, 0.936f, 1.2f, 3.536f, 0f, -1.723f,
        1.6f, 0f, 0f, -3.372f, 2.0f, -3.536f, 0f, -3.047f,
        2.4f, -5f, 0f, -0.936f, 2.8f, -3.536f, 0f, 1.723f,
        3.2f, 0f, 0f, 3.372f,
    };

    private static final float[] KF_TAILL_LOW = {
        0f, -6.468f, 0f, -1.15f, 0.4f, -1.244f, 0f, -3.889f,
        0.8f, 4.708f, 0f, -4.351f, 1.2f, 7.903f, 0f, -2.263f,
        1.6f, 6.468f, 0f, 1.15f, 2.0f, 1.244f, 0f, 3.889f,
        2.4f, -4.708f, 0f, 4.351f, 2.8f, -7.903f, 0f, 2.263f,
        3.2f, -6.468f, 0f, -1.15f,
    };

    private static final float[] KF_TAILR_LOW = {
        0f, 6.468f, 0f, -1.15f, 0.4f, 1.244f, 0f, -3.889f,
        0.8f, -4.708f, 0f, -4.351f, 1.2f, -7.903f, 0f, -2.263f,
        1.6f, -6.468f, 0f, 1.15f, 2.0f, -1.244f, 0f, 3.889f,
        2.4f, 4.708f, 0f, 4.351f, 2.8f, 7.903f, 0f, 2.263f,
        3.2f, 6.468f, 0f, -1.15f,
    };

    private static final float[] KF_SCALE_LOOP_L = {
        0f, 1.225f, 1f, 1f, 3.2f, 1.225f, 1f, 1f,
    };

    private static final float[] KF_SCALE_LOOP_R = {
        0f, 1.325f, 1f, 1f, 3.2f, 1.325f, 1f, 1f,
    };
}