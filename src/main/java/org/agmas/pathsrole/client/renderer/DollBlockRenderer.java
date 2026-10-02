package org.agmas.pathsrole.client.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import org.agmas.pathsrole.content.block.PlayerDollBlockEntity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DollBlockRenderer extends GeoBlockRenderer<PlayerDollBlockEntity> {

    private static final Map<ResourceLocation, SkinLayers3DIntegration.SkinMeshes> meshCache = new ConcurrentHashMap<>();

    public DollBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new DollBlockModel());
    }

    @Override
    public void render(PlayerDollBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        ensureSkinLoaded(entity);

        float[] boneData = entity.boneData;
        if (boneData != null && boneData.length > 0) {
            applyBoneDataToModel(entity, boneData);
        }

        double squish = entity.squash;
        double lastSquish = squish * 3;
        float squash = (float) Math.pow(1 - 1f / (1f + Mth.lerp(partialTick, (float) lastSquish, (float) squish)), 2);

        float scale = entity.modelScale;
        if (scale > 0 && scale != 1.0f) {
            poseStack.pushPose();
            poseStack.scale(1f, scale, 1f);
            poseStack.translate(0.5f, 0f, 0.5f);
            poseStack.scale(scale, 1f, scale);
            poseStack.translate(-0.5f, 0f, -0.5f);
        }

        poseStack.pushPose();
        poseStack.scale(1.0F, 1.0F - squash, 1.0F);
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.scale(1.0F + squash / 2.0F, 1.0F, 1.0F + squash / 2.0F);
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        super.render(entity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);

        render3DOverlay(entity, poseStack, bufferSource, packedLight, packedOverlay);

        renderEquippedItems(entity, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.popPose();
        if (scale > 0 && scale != 1.0f) {
            poseStack.popPose();
        }
    }

    private void applyBoneDataToModel(PlayerDollBlockEntity entity, float[] boneData) {
        ResourceLocation modelLoc = getGeoModel().getModelResource(entity);
        BakedGeoModel model = getGeoModel().getBakedModel(modelLoc);
        if (model == null) return;

        setBone(model, "Head", boneData, 0);
        setBone(model, "RightArm", boneData, 3);
        setBone(model, "LeftArm", boneData, 6);
        setBone(model, "RightLeg", boneData, 9);
        setBone(model, "LeftLeg", boneData, 12);
    }

    private static void setBone(BakedGeoModel model, String name, float[] data, int offset) {
        if (data == null || data.length < offset + 3) return;
        model.getBone(name).ifPresent(b -> {
            b.setRotX(data[offset]);
            b.setRotY(data[offset + 1]);
            b.setRotZ(data[offset + 2]);
        });
    }

    private void ensureSkinLoaded(PlayerDollBlockEntity entity) {
        String playerName = entity.getPlayerName();
        String skinUrl = entity.getSkinUrl();

        if (playerName == null || playerName.isEmpty()) return;
        if (entity.getSkinTexture() != null) return;
        if (entity.isSkinLoadingAttempted()) return;

        entity.setSkinLoadingAttempted(true);
        loadSkin(entity, playerName, skinUrl);
    }

    private void render3DOverlay(PlayerDollBlockEntity entity, PoseStack ps,
                                  MultiBufferSource bufferSource, int light, int overlay) {
        if (!SkinLayers3DIntegration.isEnabled()) return;
        ResourceLocation tex = entity.getSkinTexture();
        if (tex == null) return;

        SkinLayers3DIntegration.SkinMeshes meshes = meshCache.computeIfAbsent(tex, id -> {
            NativeImage img = SkinLayers3DIntegration.getSkinImage(id);
            return SkinLayers3DIntegration.generateMeshes(img);
        });
        if (meshes == null || meshes.isEmpty()) return;

        float[] boneData = entity.boneData;
        SkinLayers3DIntegration.renderOverlay(ps, bufferSource, tex, meshes, boneData, light, overlay);
    }

    private void loadSkin(PlayerDollBlockEntity entity, String playerName, String skinUrl) {
        if (skinUrl != null && !skinUrl.isEmpty()) {
            loadFromUrl(entity, playerName, skinUrl);
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                UUID uuid = PlayerSkinUtil.getUUIDFromName(playerName).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (uuid != null) {
                    String url = PlayerSkinUtil.getSkinUrl(uuid).get(10, java.util.concurrent.TimeUnit.SECONDS);
                    if (url != null) {
                        NativeImage img = PlayerSkinUtil.downloadSkin(url).get(10, java.util.concurrent.TimeUnit.SECONDS);
                        if (img != null) {
                            ResourceLocation texId = PlayerSkinUtil.getSkinTextureId(uuid);
                            Minecraft.getInstance().execute(() -> SkinTextureManager.registerTexture(texId, img));
                            return texId;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        }).thenAccept(tex -> {
            Minecraft.getInstance().execute(() -> {
                if (tex != null) {
                    entity.setSkinTexture(tex);
                } else {
                    GameProfile profile = entity.getOwnerProfile();
                    if (profile != null && profile.getId() != null) {
                        ResourceLocation fromProfile = loadFromProfile(profile);
                        if (fromProfile != null) entity.setSkinTexture(fromProfile);
                    }
                }
            });
        });
    }

    private void loadFromUrl(PlayerDollBlockEntity entity, String playerName, String skinUrl) {
        CompletableFuture.supplyAsync(() -> {
            try {
                ResourceLocation texId = PlayerSkinUtil.getSkinTextureIdFromUrl(skinUrl);
                if (!SkinTextureManager.isTextureLoaded(texId)) {
                    NativeImage img = PlayerSkinUtil.downloadSkin(skinUrl).get(10, java.util.concurrent.TimeUnit.SECONDS);
                    if (img != null) {
                        Minecraft.getInstance().execute(() -> SkinTextureManager.registerTexture(texId, img));
                        return texId;
                    }
                } else {
                    return texId;
                }
            } catch (Exception ignored) {
            }
            return null;
        }).thenAccept(tex -> {
            Minecraft.getInstance().execute(() -> {
                if (tex != null && entity.getSkinTexture() == null) {
                    entity.setSkinTexture(tex);
                }
            });
        });
    }

    private ResourceLocation loadFromProfile(GameProfile profile) {
        try {
            if (profile.getProperties().containsKey("textures")) {
                Property prop = profile.getProperties().get("textures").iterator().next();
                if (prop != null) {
                    String decoded = new String(java.util.Base64.getDecoder().decode(prop.value()));
                    com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(decoded, com.google.gson.JsonObject.class);
                    if (json.has("textures") && json.get("textures").isJsonObject()) {
                        com.google.gson.JsonObject textures = json.getAsJsonObject("textures");
                        if (textures.has("SKIN") && textures.get("SKIN").isJsonObject()) {
                            String url = textures.getAsJsonObject("SKIN").get("url").getAsString();
                            UUID uuid = profile.getId();
                            ResourceLocation texId = PlayerSkinUtil.getSkinTextureId(uuid);
                            if (!SkinTextureManager.isTextureLoaded(texId)) {
                                NativeImage img = PlayerSkinUtil.downloadSkin(url).get(10, java.util.concurrent.TimeUnit.SECONDS);
                                if (img != null) SkinTextureManager.registerTexture(texId, img);
                            }
                            return texId;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void renderEquippedItems(PlayerDollBlockEntity entity, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (entity.hatItemId != null && !entity.hatItemId.isEmpty()) {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    ResourceLocation.tryParse(entity.hatItemId));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                ItemStack stack = new ItemStack(item);
                poseStack.pushPose();
                float[] t = entity.hatTransform;
                if (t != null && t.length >= 6) {
                    poseStack.translate(0.5 + t[3], 1.6 + t[4], 0.5 + t[5]);
                    poseStack.scale(0.6f, 0.6f, 0.6f);
                    poseStack.mulPose(new org.joml.Quaternionf().rotateX((float)Math.toRadians(t[0])));
                    poseStack.mulPose(new org.joml.Quaternionf().rotateY((float)Math.toRadians(t[1])));
                    poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float)Math.toRadians(t[2])));
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

        if (entity.handItemId != null && !entity.handItemId.isEmpty()) {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    ResourceLocation.tryParse(entity.handItemId));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                ItemStack stack = new ItemStack(item);
                poseStack.pushPose();
                float[] t = entity.handTransform;
                if (t != null && t.length >= 6) {
                    poseStack.translate(2.0 + t[3], 2.2 + t[4], t[5]);
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                    poseStack.mulPose(new org.joml.Quaternionf().rotateX((float)Math.toRadians(t[0])));
                    poseStack.mulPose(new org.joml.Quaternionf().rotateY((float)Math.toRadians(t[1])));
                    poseStack.mulPose(new org.joml.Quaternionf().rotateZ((float)Math.toRadians(t[2])));
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
}