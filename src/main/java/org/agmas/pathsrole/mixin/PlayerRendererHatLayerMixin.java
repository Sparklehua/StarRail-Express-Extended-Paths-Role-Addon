package org.agmas.pathsrole.mixin;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.agmas.pathsrole.client.render.ShionButterflyHatLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererHatLayerMixin {

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Z)V",
            at = @At("TAIL"))
    private void pathsrole$addButterflyHatLayer(EntityRendererProvider.Context context, boolean slim,
            CallbackInfo ci) {
        ((LivingEntityRendererAccessor) this).invokeAddLayer(
                new ShionButterflyHatLayer((PlayerRenderer) (Object) this));
    }
}