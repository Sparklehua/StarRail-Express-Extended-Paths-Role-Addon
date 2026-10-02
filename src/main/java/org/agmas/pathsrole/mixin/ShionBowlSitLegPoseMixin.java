package org.agmas.pathsrole.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.nihility.shion.ShionPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class ShionBowlSitLegPoseMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void pathsrole$$shionBowlSitLegPose(T entity, float limbSwing,
            float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        ShionPlayerComponent comp = PathsroleComponents.getShionComponent(player);
        if (comp == null || !comp.isSitting()) return;

        HumanoidModel<?> self = (HumanoidModel<?>)(Object)this;
        self.leftLeg.xRot = -1.57f;
        self.leftLeg.yRot = -0.3f;
        self.rightLeg.xRot = -1.57f;
        self.rightLeg.yRot = 0.3f;
        self.body.xRot = 0.2f;
    }
}