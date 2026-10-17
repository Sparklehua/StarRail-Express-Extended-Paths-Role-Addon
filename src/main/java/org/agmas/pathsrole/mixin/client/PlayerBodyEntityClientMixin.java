package org.agmas.pathsrole.mixin.client;

import io.wifi.starrailexpress.content.entity.PlayerBodyEntity;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerBodyEntity.class)
public abstract class PlayerBodyEntityClientMixin {
    @Inject(method = "getCustomName", at = @At("HEAD"), cancellable = true, require = 0)
    private void kiraaddon$guardClientServer(CallbackInfoReturnable<Component> cir) {
        PlayerBodyEntity self = (PlayerBodyEntity) (Object) this;
        if (self.level().isClientSide && self.getServer() == null) {
            cir.setReturnValue(null);
        }
    }
}