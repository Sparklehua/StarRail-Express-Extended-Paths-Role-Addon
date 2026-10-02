package org.agmas.pathsrole.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.cache.object.BakedGeoModel;

import java.util.*;

public class VoxelOverlayRenderer {

    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final Map<ResourceLocation, Map<String, List<VoxelOverlayGenerator.VoxelData>>> VOXEL_CACHE = new HashMap<>();

    private static final float[][] VERTICES = {
            {-0.5f, -0.5f, -0.5f}, { 0.5f, -0.5f, -0.5f}, { 0.5f,  0.5f, -0.5f}, {-0.5f,  0.5f, -0.5f},
            {-0.5f, -0.5f,  0.5f}, { 0.5f, -0.5f,  0.5f}, { 0.5f,  0.5f,  0.5f}, {-0.5f,  0.5f,  0.5f},
    };

    private static final int[][] FACES = {
            {0, 1, 2, 3},
            {5, 4, 7, 6},
            {1, 5, 6, 2},
            {4, 0, 3, 7},
            {3, 2, 6, 7},
            {4, 5, 1, 0},
    };

    private static final float[][] FACE_NORMALS = {
            { 0,  0, -1},
            { 0,  0,  1},
            { 1,  0,  0},
            {-1,  0,  0},
            { 0,  1,  0},
            { 0, -1,  0},
    };

    private static final float[][] FACE_UVS = {
            {0, 0, 1, 0, 1, 1, 0, 1},
            {0, 0, 1, 0, 1, 1, 0, 1},
            {0, 0, 1, 0, 1, 1, 0, 1},
            {0, 0, 1, 0, 1, 1, 0, 1},
            {0, 0, 1, 0, 1, 1, 0, 1},
            {0, 0, 1, 0, 1, 1, 0, 1},
    };

    public static Map<String, List<VoxelOverlayGenerator.VoxelData>> getVoxels(ResourceLocation skinTex) {
        return VOXEL_CACHE.computeIfAbsent(skinTex, tex -> {
            NativeImage img = getSkinImage(tex);
            if (img == null) return Collections.emptyMap();
            return VoxelOverlayGenerator.generate(img);
        });
    }

    private static NativeImage getSkinImage(ResourceLocation tex) {
        try {
            var texture = Minecraft.getInstance().getTextureManager().getTexture(tex);
            if (texture instanceof DynamicTexture dt) {
                return dt.getPixels();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void clearCache() {
        VOXEL_CACHE.clear();
    }

    /**
     * Render voxels by applying per-bone transformations from the baked model.
     * Voxels are rendered with entityTranslucent to support alpha blending.
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                              int packedLight, int packedOverlay,
                              Map<String, List<VoxelOverlayGenerator.VoxelData>> voxels,
                              BakedGeoModel model) {
        if (voxels == null || voxels.isEmpty() || model == null) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE));

        for (Map.Entry<String, List<VoxelOverlayGenerator.VoxelData>> entry : voxels.entrySet()) {
            String boneName = entry.getKey();
            List<VoxelOverlayGenerator.VoxelData> boneVoxels = entry.getValue();
            if (boneVoxels.isEmpty()) continue;

            model.getBone(boneName).ifPresent(bone -> {
                float pivotX = bone.getPivotX();
                float pivotY = bone.getPivotY();
                float pivotZ = bone.getPivotZ();

                poseStack.pushPose();

                poseStack.translate(pivotX, pivotY, pivotZ);
                poseStack.mulPose(new Quaternionf().rotateZ(bone.getRotZ()));
                poseStack.mulPose(new Quaternionf().rotateY(bone.getRotY()));
                poseStack.mulPose(new Quaternionf().rotateX(bone.getRotX()));
                poseStack.translate(-pivotX, -pivotY, -pivotZ);

                poseStack.translate(bone.getPosX(), bone.getPosY(), bone.getPosZ());

                PoseStack.Pose pose = poseStack.last();
                Matrix4f mat = pose.pose();

                for (VoxelOverlayGenerator.VoxelData v : boneVoxels) {
                    renderVoxel(mat, pose, consumer, v, packedLight, packedOverlay);
                }

                poseStack.popPose();
            });
        }
    }

    private static void renderVoxel(Matrix4f mat, PoseStack.Pose pose, VertexConsumer consumer,
                                    VoxelOverlayGenerator.VoxelData v, int light, int overlay) {
        int r = (v.color >> 16) & 0xFF;
        int g = (v.color >> 8) & 0xFF;
        int b = v.color & 0xFF;
        int a = v.alpha;

        float sx = v.w;
        float sy = v.h;
        float sz = v.d;

        for (int fi = 0; fi < 6; fi++) {
            float nx = FACE_NORMALS[fi][0];
            float ny = FACE_NORMALS[fi][1];
            float nz = FACE_NORMALS[fi][2];

            float bright = 0.7f + 0.3f * Math.max(0, ny * 0.8f + nz * 0.2f);
            int lr = (int) (r * bright);
            int lg = (int) (g * bright);
            int lb = (int) (b * bright);

            int[] idx = FACES[fi];
            float[] uv = FACE_UVS[fi];

            for (int ci = 0; ci < 4; ci++) {
                int vi = idx[ci];
                float vx = v.x + VERTICES[vi][0] * sx;
                float vy = v.y + VERTICES[vi][1] * sy;
                float vz = v.z + VERTICES[vi][2] * sz;

                consumer.addVertex(mat, vx, vy, vz)
                        .setColor(lr, lg, lb, a)
                        .setUv(uv[ci * 2], uv[ci * 2 + 1])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose, nx, ny, nz);
            }
        }
    }
}