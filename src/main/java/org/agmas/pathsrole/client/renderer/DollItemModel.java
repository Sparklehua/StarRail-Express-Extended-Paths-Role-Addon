package org.agmas.pathsrole.client.renderer;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import org.agmas.pathsrole.content.item.PlayerDollItem;

public class DollItemModel extends GeoModel<PlayerDollItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("pathsrole", "geo/steve.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private ResourceLocation currentTexture = TEXTURE;

    public void setCurrentTexture(ResourceLocation tex) {
        this.currentTexture = tex != null ? tex : TEXTURE;
    }

    @Override
    public ResourceLocation getModelResource(PlayerDollItem obj) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PlayerDollItem obj) {
        return currentTexture;
    }

    @Override
    public ResourceLocation getAnimationResource(PlayerDollItem obj) {
        return null;
    }
}