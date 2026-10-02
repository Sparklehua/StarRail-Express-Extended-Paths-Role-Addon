package org.agmas.pathsrole.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.agmas.pathsrole.init.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public class ShionBowlSitArmPoseMixin {
    @Inject(method = "getArmPose", at = @At("TAIL"), cancellable = true)
    private static void pathsrole$shionBowlSitArmPose(AbstractClientPlayer player,
            InteractionHand hand, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (player.getItemInHand(hand).is(ModItems.BOWL) || player.getItemInHand(hand).is(ModItems.BEGGING_BOWL)) {
            cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_CHARGE);
        }
    }
}