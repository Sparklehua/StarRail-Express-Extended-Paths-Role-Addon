package org.agmas.pathsrole.client.renderer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import org.agmas.noellesroles.content.effects.TimeStopEffect;
import org.agmas.noellesroles.init.ModEffects;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.content.entity.YinYangOrbEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class YinYangOrbRenderer
extends EntityRenderer<YinYangOrbEntity> {
    private static final ResourceLocation TEXTURE = PathsRoleMod.id("textures/entity/yin_yang_orb.png");

    public YinYangOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void render(YinYangOrbEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(ModEffects.TIME_STOP) && !TimeStopEffect.canMovePlayers.contains(player.getUUID())) {
            return;
        }
        float radius = entity.getRadius();
        if (radius <= 0.01f) {
            return;
        }
        poseStack.pushPose();
        Quaternionf cameraRotation = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        poseStack.mulPose(cameraRotation);
        float scale = radius * 2.0f * 1.4f;
        poseStack.scale(scale, scale, scale);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent((ResourceLocation)TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        float half = 0.5f;
        vertexConsumer.addVertex(matrix, -half, -half, 0.0f).setColor(255, 255, 255, 255).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
        vertexConsumer.addVertex(matrix, -half, half, 0.0f).setColor(255, 255, 255, 255).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
        vertexConsumer.addVertex(matrix, half, half, 0.0f).setColor(255, 255, 255, 255).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
        vertexConsumer.addVertex(matrix, half, -half, 0.0f).setColor(255, 255, 255, 255).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public ResourceLocation getTextureLocation(YinYangOrbEntity entity) {
        return TEXTURE;
    }
}