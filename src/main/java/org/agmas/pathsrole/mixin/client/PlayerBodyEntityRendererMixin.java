package org.agmas.pathsrole.mixin.client;

import io.wifi.starrailexpress.client.render.entity.PlayerBodyEntityRenderer;
import io.wifi.starrailexpress.content.entity.PlayerBodyEntity;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerBodyEntityRenderer.class)
public class PlayerBodyEntityRendererMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void kiraaddon$skipRemovedBody(PlayerBodyEntity entity, Frustum frustum, double camX, double camY, double camZ,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (entity.isRemoved()) {
            cir.setReturnValue(false);
        }
    }
}