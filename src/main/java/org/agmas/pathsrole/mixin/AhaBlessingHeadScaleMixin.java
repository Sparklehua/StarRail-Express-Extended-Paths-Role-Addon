package org.agmas.pathsrole.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.agmas.pathsrole.modifier.aha_blessing.AhaBlessingHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class AhaBlessingHeadScaleMixin<T extends LivingEntity> {

    @Unique
    private T pathsrole$storedEntity = null;

    @Shadow
    protected abstract Iterable<ModelPart> headParts();

    @Shadow
    protected abstract Iterable<ModelPart> bodyParts();

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void pathsrole$storeEntity(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                        float netHeadYaw, float headPitch) {
        this.pathsrole$storedEntity = entity;
    }

    @Inject(method = "renderToBuffer", at = @At("HEAD"), cancellable = true)
    private void pathsrole$adjustHeadScale(PoseStack poseStack, VertexConsumer vertexConsumer,
                                            int packedLight, int packedOverlay, int color, CallbackInfo ci) {
        if (!(this.pathsrole$storedEntity instanceof Player player)) {
            return;
        }

        var scaleAttr = player.getAttribute(Attributes.SCALE);
        if (scaleAttr == null || !scaleAttr.hasModifier(AhaBlessingHandler.SHRINK_MODIFIER_ID)) {
            return;
        }

        ci.cancel();

        float counterScale = 1.0f / AhaBlessingHandler.SHRINK_SCALE;

        poseStack.pushPose();
        poseStack.scale(counterScale, counterScale, counterScale);
        for (ModelPart part : this.headParts()) {
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();

        for (ModelPart part : this.bodyParts()) {
            part.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }
    }
}