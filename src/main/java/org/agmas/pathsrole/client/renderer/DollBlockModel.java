package org.agmas.pathsrole.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import org.agmas.pathsrole.content.block.PlayerDollBlockEntity;

public class DollBlockModel extends GeoModel<PlayerDollBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("pathsrole", "geo/steve.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    @Override
    public ResourceLocation getModelResource(PlayerDollBlockEntity obj) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PlayerDollBlockEntity obj) {
        ResourceLocation skin = obj.getSkinTexture();
        if (skin != null && SkinTextureManager.isTextureLoaded(skin)) return skin;
        if (skin != null) {
            obj.setSkinTexture(null);
            obj.setSkinLoadingAttempted(false);
        }

        String cachedData = obj.getSkinData();
        if (cachedData != null && !cachedData.isEmpty()) {
            ResourceLocation texId = ResourceLocation.fromNamespaceAndPath("pathsrole",
                    "skins/cached_" + Integer.toHexString(cachedData.hashCode()));
            if (SkinTextureManager.isTextureLoaded(texId)) {
                obj.setSkinTexture(texId);
                return texId;
            }
            try {
                byte[] skinBytes = java.util.Base64.getDecoder().decode(cachedData);
                NativeImage img = NativeImage.read(new java.io.ByteArrayInputStream(skinBytes));
                if (img != null) {
                    SkinTextureManager.registerTexture(texId, img);
                    obj.setSkinTexture(texId);
                    return texId;
                }
            } catch (Exception ignored) {
            }
            return TEXTURE;
        }

        String name = obj.getPlayerName();
        if (name != null && !name.isEmpty()) {
            ResourceLocation cached = SkinTextureManager.getCachedTexture(name);
            if (cached != null && SkinTextureManager.isTextureLoaded(cached)) {
                obj.setSkinTexture(cached);
                return cached;
            }
        }
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PlayerDollBlockEntity obj) {
        return null;
    }
}