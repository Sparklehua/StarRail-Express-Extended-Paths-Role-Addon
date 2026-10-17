package org.agmas.pathsrole.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.content.entity.SheerHeartEntity;

public class SheerHeartEntityRenderer extends MobRenderer<SheerHeartEntity, SheerHeartEntityModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "textures/entity/sheer_heart.png");

    public SheerHeartEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new SheerHeartEntityModel(context.bakeLayer(SheerHeartEntityModel.LAYER_LOCATION)), 0F);
    }

    @Override
    protected void scale(SheerHeartEntity entity, PoseStack poseStack, float partialTickTime) {
        float baseScale = 0.5F;
        poseStack.scale(baseScale, baseScale, baseScale);
        poseStack.translate(0.0F, -0.47F, 0.0F);
        super.scale(entity, poseStack, partialTickTime);
    }

    @Override
    public ResourceLocation getTextureLocation(SheerHeartEntity entity) {
        return TEXTURE;
    }
}