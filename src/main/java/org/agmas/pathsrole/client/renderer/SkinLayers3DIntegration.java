package org.agmas.pathsrole.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.PathsRoleMod;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Optional integration with 3D Skin Layers mod.
 * When skinlayers3d is present, generates 3D voxel meshes
 * from player skin textures and renders them on doll models.
 * When absent, all methods are no-ops.
 */
public class SkinLayers3DIntegration {

    private static final boolean ENABLED;

    private static Object meshHelper;
    private static Method create3DMeshMethod;
    private static Method meshRenderMethod;
    private static Method meshSetPositionMethod;
    private static Method meshSetRotationMethod;
    private static Method meshResetMethod;

    static {
        boolean enabled = false;
        try {
            if (FabricLoader.getInstance().isModLoaded("skinlayers3d")) {
                Class<?> apiClass = Class.forName("dev.tr7zw.skinlayers.api.SkinLayersAPI");
                Object helperInstance = apiClass.getMethod("getMeshHelper").invoke(null);
                meshHelper = helperInstance;

                create3DMeshMethod = helperInstance.getClass().getMethod("create3DMesh",
                        NativeImage.class, int.class, int.class, int.class, int.class, int.class,
                        boolean.class, float.class, boolean.class);

                Class<?> meshClass = Class.forName("dev.tr7zw.skinlayers.api.Mesh");
                meshRenderMethod = meshClass.getMethod("render",
                        PoseStack.class, VertexConsumer.class, int.class, int.class);
                meshSetPositionMethod = meshClass.getMethod("setPosition",
                        float.class, float.class, float.class);
                meshSetRotationMethod = meshClass.getMethod("setRotation",
                        float.class, float.class, float.class);
                meshResetMethod = meshClass.getMethod("reset");

                enabled = true;
                PathsRoleMod.LOGGER.info("[SkinLayers3D] Successfully integrated with 3D Skin Layers mod!");
            }
        } catch (Exception e) {
            PathsRoleMod.LOGGER.warn("[SkinLayers3D] Failed to initialize integration: " + e.getMessage());
        }
        ENABLED = enabled;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Generate 3D meshes for all body parts from a 64x64 skin image.
     */
    public static SkinMeshes generateMeshes(NativeImage skinImage) {
        if (!ENABLED || skinImage == null) return SkinMeshes.EMPTY;
        if (skinImage.getWidth() != 64 || skinImage.getHeight() != 64) return SkinMeshes.EMPTY;

        try {
            Object head = create3DMesh(skinImage, 8, 8, 8, 32, 0, false, 0.6f, false);
            Object body = create3DMesh(skinImage, 8, 12, 4, 16, 32, true, 0f, false);
            Object rightArm = create3DMesh(skinImage, 4, 12, 4, 40, 32, true, -2f, false);
            Object leftArm = create3DMesh(skinImage, 4, 12, 4, 48, 48, true, -2f, false);
            Object rightLeg = create3DMesh(skinImage, 4, 12, 4, 0, 32, true, 0f, false);
            Object leftLeg = create3DMesh(skinImage, 4, 12, 4, 0, 48, true, 0f, false);
            return new SkinMeshes(head, body, rightArm, leftArm, rightLeg, leftLeg);
        } catch (Exception e) {
            PathsRoleMod.LOGGER.error("[SkinLayers3D] Mesh generation failed: ", e);
            return SkinMeshes.EMPTY;
        }
    }

    private static Object create3DMesh(NativeImage skin, int w, int h, int d, int u, int v,
                                       boolean topPivot, float rotationOffset, boolean mirror) throws Exception {
        return create3DMeshMethod.invoke(meshHelper, skin, w, h, d, u, v, topPivot, rotationOffset, mirror);
    }

    /**
     * Try to get NativeImage from a registered texture.
     */
    public static NativeImage getSkinImage(ResourceLocation textureId) {
        if (!ENABLED) return null;
        try {
            Optional<net.minecraft.server.packs.resources.Resource> res =
                    Minecraft.getInstance().getResourceManager().getResource(textureId);
            if (res.isPresent()) {
                return NativeImage.read(res.get().open());
            }
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(textureId);
            if (texture instanceof net.minecraft.client.renderer.texture.DynamicTexture dt) {
                return dt.getPixels();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Render 3D meshes as overlay on a doll model.
     */
    public static void renderOverlay(PoseStack poseStack, MultiBufferSource bufferSource,
                                     ResourceLocation skinTexture, SkinMeshes meshes,
                                     float[] boneData, int light, int overlay) {
        if (!ENABLED || meshes == null || meshes.isEmpty()) return;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(skinTexture, true));

        renderBoneMesh(poseStack, vc, meshes.head, "Head", boneData, 0, light, overlay);
        renderBoneMesh(poseStack, vc, meshes.body, "Body", boneData, -1, light, overlay);
        renderBoneMesh(poseStack, vc, meshes.rightArm, "RightArm", boneData, 3, light, overlay);
        renderBoneMesh(poseStack, vc, meshes.leftArm, "LeftArm", boneData, 6, light, overlay);
        renderBoneMesh(poseStack, vc, meshes.rightLeg, "RightLeg", boneData, 9, light, overlay);
        renderBoneMesh(poseStack, vc, meshes.leftLeg, "LeftLeg", boneData, 12, light, overlay);
    }

    private static void renderBoneMesh(PoseStack ps, VertexConsumer vc, Object mesh,
                                       String boneName, float[] boneData, int offset,
                                       int light, int overlay) {
        if (mesh == null) return;

        try {
            meshResetMethod.invoke(mesh);

            float rx = 0, ry = 0, rz = 0;
            if (boneData != null && offset >= 0 && boneData.length >= offset + 3) {
                rx = boneData[offset];
                ry = boneData[offset + 1];
                rz = boneData[offset + 2];
            }
            meshSetRotationMethod.invoke(mesh, rx, ry, rz);

            float voxelScale = 1.05f;
            ps.pushPose();

            if (boneName != null) {
                ps.translate(0.5, 0, 0.5);
            }
            if ("Body".equals(boneName)) {
                ps.translate(0.5, 0, 0.5);
            }

            ps.scale(voxelScale, voxelScale, voxelScale);
            meshSetPositionMethod.invoke(mesh, 0f, 0f, 0f);
            meshRenderMethod.invoke(mesh, ps, vc, light, overlay);
            ps.popPose();
        } catch (Exception e) {
            PathsRoleMod.LOGGER.error("[SkinLayers3D] Render failed for bone " + boneName + ": ", e);
        }
    }

    /**
     * Holds generated 3D meshes for all body parts.
     */
    public static class SkinMeshes {
        public static final SkinMeshes EMPTY = new SkinMeshes(null, null, null, null, null, null);

        final Object head, body, rightArm, leftArm, rightLeg, leftLeg;

        SkinMeshes(Object head, Object body, Object rightArm, Object leftArm, Object rightLeg, Object leftLeg) {
            this.head = head;
            this.body = body;
            this.rightArm = rightArm;
            this.leftArm = leftArm;
            this.rightLeg = rightLeg;
            this.leftLeg = leftLeg;
        }

        boolean isEmpty() {
            return head == null;
        }
    }
}