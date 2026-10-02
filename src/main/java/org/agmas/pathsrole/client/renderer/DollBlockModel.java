package org.agmas.pathsrole.client.renderer;

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
        if (skin != null) return skin;
        String name = obj.getPlayerName();
        if (name != null && !name.isEmpty()) {
            ResourceLocation cached = SkinTextureManager.getCachedTexture(name);
            if (cached != null) {
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