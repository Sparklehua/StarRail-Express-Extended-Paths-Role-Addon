package org.agmas.pathsrole.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.content.entity.ThrownFlowerDoll;
import org.agmas.pathsrole.init.ModItems;

public class ThrownFlowerDollRenderer extends EntityRenderer<ThrownFlowerDoll> {
    private static final ResourceLocation TEXTURE = PathsRoleMod.id("textures/item/flower_doll.png");
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    public ThrownFlowerDollRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(ThrownFlowerDoll entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        poseStack.pushPose();

        float scale = entity.getScale();
        poseStack.translate(0, 0.35 * scale, 0);

        if (entity.isTicking()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    (entity.tickCount + partialTick) * 30f));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        }

        poseStack.scale(scale, scale, scale);

        int renderLight = light;
        if (entity.isTicking()) {
            int tickCount = entity.getTickCount();
            if (tickCount > 0 && tickCount % 6 < 3) {
                renderLight = 15728880;
            }
        }

        this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND,
                renderLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), 0);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownFlowerDoll entity) {
        return TEXTURE;
    }
}