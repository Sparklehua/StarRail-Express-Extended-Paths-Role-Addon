package org.agmas.pathsrole.client.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import org.agmas.pathsrole.content.item.PlayerDollItem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DollItemRenderer extends GeoItemRenderer<PlayerDollItem>
        implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private static final Map<ResourceLocation, SkinLayers3DIntegration.SkinMeshes> meshCache = new ConcurrentHashMap<>();

    private static final ResourceLocation STEVE = ResourceLocation.withDefaultNamespace("textures/entity/steve.png");

    public DollItemRenderer() {
        super(new DollItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {
        String playerName = getPlayerNameFromStack(stack);
        String skinUrl = PlayerDollItem.getSkinUrl(stack);
        GameProfile profile = PlayerDollItem.getGameProfile(stack);

        if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
            playerName = profile.getName();
        }

        ResourceLocation tex = getTexture(playerName, skinUrl);
        ((DollItemModel) this.getGeoModel()).setCurrentTexture(tex);

        float[] boneData = getBoneDataFromStack(stack);
        if (boneData != null && boneData.length > 0) {
            applyBoneData(boneData);
        }

        if (boneData != null && boneData.length > 15 && boneData[15] > 0) {
            float scale = boneData[15];
            poseStack.pushPose();
            poseStack.scale(1f, scale, 1f);
            poseStack.translate(0.5f, 0f, 0.5f);
            poseStack.scale(scale, 1f, scale);
            poseStack.translate(-0.5f, 0f, -0.5f);
            super.renderByItem(stack, context, poseStack, buffer, light, overlay);
            render3DOverlay(poseStack, buffer, tex, boneData, light, overlay);
            poseStack.popPose();
        } else {
            super.renderByItem(stack, context, poseStack, buffer, light, overlay);
            render3DOverlay(poseStack, buffer, tex, boneData, light, overlay);
        }

        renderEquippedItems(stack, poseStack, buffer, light, overlay);
    }

    private void render3DOverlay(PoseStack ps, MultiBufferSource buffer,
                                  ResourceLocation tex, float[] boneData,
                                  int light, int overlay) {
        if (!SkinLayers3DIntegration.isEnabled()) return;
        if (tex == null) return;

        SkinLayers3DIntegration.SkinMeshes meshes = meshCache.computeIfAbsent(tex, id -> {
            com.mojang.blaze3d.platform.NativeImage img = SkinLayers3DIntegration.getSkinImage(id);
            return SkinLayers3DIntegration.generateMeshes(img);
        });
        if (meshes == null || meshes.isEmpty()) return;

        SkinLayers3DIntegration.renderOverlay(ps, buffer, tex, meshes, boneData, light, overlay);
    }

    private float[] getBoneDataFromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return new float[0];
        CompoundTag tag = data.copyTag();
        if (!tag.contains("Poses")) return new float[0];
        CompoundTag poses = tag.getCompound("Poses");
        int count = poses.getAllKeys().size();
        if (count == 0) return new float[0];
        float[] result = new float[count];
        for (String key : poses.getAllKeys()) {
            try {
                int idx = Integer.parseInt(key.substring(1));
                result[idx] = poses.getFloat(key);
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private void applyBoneData(float[] boneData) {
        var model = getGeoModel().getBakedModel(getGeoModel().getModelResource(null));
        if (model == null) return;
        if (boneData.length > 2) {
            model.getBone("Head").ifPresent(b -> { b.setRotX(boneData[0]); b.setRotY(boneData[1]); b.setRotZ(boneData[2]); });
        }
        if (boneData.length > 5) {
            model.getBone("RightArm").ifPresent(b -> { b.setRotX(boneData[3]); b.setRotY(boneData[4]); b.setRotZ(boneData[5]); });
        }
        if (boneData.length > 8) {
            model.getBone("LeftArm").ifPresent(b -> { b.setRotX(boneData[6]); b.setRotY(boneData[7]); b.setRotZ(boneData[8]); });
        }
        if (boneData.length > 11) {
            model.getBone("RightLeg").ifPresent(b -> { b.setRotX(boneData[9]); b.setRotY(boneData[10]); b.setRotZ(boneData[11]); });
        }
        if (boneData.length > 14) {
            model.getBone("LeftLeg").ifPresent(b -> { b.setRotX(boneData[12]); b.setRotY(boneData[13]); b.setRotZ(boneData[14]); });
        }
    }

    private ResourceLocation getTexture(String playerName, String skinUrl) {
        if (playerName == null || playerName.isEmpty()) return STEVE;
        ResourceLocation cached = SkinTextureManager.getCachedTexture(playerName);
        if (cached != null) return cached;
        if (SkinTextureManager.hasTriedSkin(playerName)) return STEVE;
        SkinTextureManager.loadSkinForPlayer(playerName, skinUrl);
        return STEVE;
    }

    private String getPlayerNameFromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && !data.isEmpty()) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("PlayerName")) return tag.getString("PlayerName");
        }
        return null;
    }

    private void renderEquippedItems(ItemStack dollStack, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int light, int overlay) {
        CustomData data = dollStack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return;
        CompoundTag tag = data.copyTag();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (tag.contains("HatItem")) {
            String hatId = tag.getString("HatItem");
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    ResourceLocation.tryParse(hatId));
            if (item != null && item != Items.AIR) {
                ItemStack stack = new ItemStack(item);
                poseStack.pushPose();
                if (tag.contains("HatTransform")) {
                    CompoundTag ht = tag.getCompound("HatTransform");
                    poseStack.translate(0.5 + ht.getFloat("px"), 1.6 + ht.getFloat("py"), 0.5 + ht.getFloat("pz"));
                    poseStack.scale(0.6f, 0.6f, 0.6f);
                    poseStack.mulPose(new org.joml.Quaternionf().rotateX((float)Math.toRadians(ht.getFloat("rx"))));
                    poseStack.mulPose(new org.joml.Quaternionf().rotateY((float)Math.toRadians(ht.getFloat("ry"))));
                    poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float)Math.toRadians(ht.getFloat("rz"))));
                } else {
                    poseStack.translate(0.5, 1.6, 0.5);
                    poseStack.scale(0.6f, 0.6f, 0.6f);
                }
                mc.getItemRenderer().renderStatic(stack,
                        net.minecraft.world.item.ItemDisplayContext.HEAD,
                        light, overlay, poseStack, bufferSource, mc.level, 0);
                poseStack.popPose();
            }
        }

        if (tag.contains("HandItem")) {
            String handId = tag.getString("HandItem");
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    ResourceLocation.tryParse(handId));
            if (item != null && item != Items.AIR) {
                ItemStack stack = new ItemStack(item);
                poseStack.pushPose();
                if (tag.contains("HandTransform")) {
                    CompoundTag ht = tag.getCompound("HandTransform");
                    poseStack.translate(2.0 + ht.getFloat("px"), 2.2 + ht.getFloat("py"), ht.getFloat("pz"));
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                    poseStack.mulPose(new org.joml.Quaternionf().rotateX((float)Math.toRadians(ht.getFloat("rx"))));
                    poseStack.mulPose(new org.joml.Quaternionf().rotateY((float)Math.toRadians(ht.getFloat("ry"))));
                    poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float)Math.toRadians(ht.getFloat("rz"))));
                } else {
                    poseStack.translate(2.0, 2.2, 0.0);
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                }
                mc.getItemRenderer().renderStatic(stack,
                        net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        light, overlay, poseStack, bufferSource, mc.level, 0);
                poseStack.popPose();
            }
        }
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {
        renderByItem(stack, context, matrices, vertexConsumers, light, overlay);
    }
}