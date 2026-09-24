package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.network.original.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.agmas.pathsrole.content.item.TargetRevolverItem;
import org.agmas.pathsrole.init.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "io.wifi.starrailexpress.network.original.GunShootPayload$Receiver", remap = false)
public class TargetRevolverGunShootMixin {

    @Inject(method = "receive", at = @At("HEAD"), cancellable = true, require = 0)
    private void blockTargetRevolverOnNonHunter(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayer player = context.player();
        ItemStack mainHandStack = player.getMainHandItem();

        if (!mainHandStack.is(ModItems.TARGET_REVOLVER)) {
            return;
        }

        UUID hunterUUID = TargetRevolverItem.getHunterUUID(mainHandStack);
        if (hunterUUID == null) {
            ci.cancel();
            return;
        }

        Entity hitEntity = player.serverLevel().getEntity(payload.target());
        if (!(hitEntity instanceof ServerPlayer targetPlayer)) {
            ci.cancel();
            return;
        }

        if (!targetPlayer.getUUID().equals(hunterUUID)) {
            ci.cancel();
        }
    }
}