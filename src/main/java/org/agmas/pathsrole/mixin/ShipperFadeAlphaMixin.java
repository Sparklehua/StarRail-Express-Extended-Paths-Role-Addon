package org.agmas.pathsrole.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class ShipperFadeAlphaMixin<T extends LivingEntity> {

    protected ShipperFadeAlphaMixin(EntityRendererProvider.Context ctx) {}

    @Unique
    private boolean pathsrole$fading = false;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void pathsrole$fadeRender(T entity, float entityYaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffer,
                                       int packedLight, CallbackInfo ci) {
        if (pathsrole$fading) return;
        if (!(entity instanceof Player player)) return;
        ShipperPlayerComponent comp = ShipperPlayerComponent.KEY.get(player);
        if (comp == null) return;
        float alpha = comp.getInvisibilityAlpha();
        if (alpha >= 0.99f || alpha <= 0.01f) return;

        pathsrole$fading = true;
        ci.cancel();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            MultiBufferSource wrapped = new AlphaBufferSource(buffer, alpha);
            LivingEntityRenderer renderer = (LivingEntityRenderer)(Object)this;
            renderer.render(entity, entityYaw, partialTick, poseStack, wrapped, packedLight);
            RenderSystem.disableBlend();
        } finally {
            pathsrole$fading = false;
        }
    }

    @Unique
    private static class AlphaBufferSource implements MultiBufferSource {
        private final MultiBufferSource parent;
        private final float alpha;

        AlphaBufferSource(MultiBufferSource parent, float alpha) {
            this.parent = parent;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new AlphaVertexConsumer(parent.getBuffer(renderType), alpha);
        }
    }

    @Unique
    private static class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer parent;
        private final float alpha;

        AlphaVertexConsumer(VertexConsumer parent, float alpha) {
            this.parent = parent;
            this.alpha = alpha;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            return parent.addVertex(x, y, z);
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            return parent.setColor(r, g, b, Math.max(0, Math.min(255, (int)(a * alpha))));
        }
        @Override public VertexConsumer setUv(float u, float v) {
            return parent.setUv(u, v);
        }
        @Override public VertexConsumer setUv1(int u, int v) {
            return parent.setUv1(u, v);
        }
        @Override public VertexConsumer setUv2(int u, int v) {
            return parent.setUv2(u, v);
        }
        @Override public VertexConsumer setNormal(float nx, float ny, float nz) {
            return parent.setNormal(nx, ny, nz);
        }
        @Override public VertexConsumer setLight(int packedLight) {
            return parent.setLight(packedLight);
        }
        @Override public VertexConsumer setOverlay(int packedOverlay) {
            return parent.setOverlay(packedOverlay);
        }
    }
}