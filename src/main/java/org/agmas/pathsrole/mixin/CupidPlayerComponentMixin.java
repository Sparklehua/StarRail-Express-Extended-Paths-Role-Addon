package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.agmas.noellesroles.game.roles.neutral.cupid.CupidPlayerComponent;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = CupidPlayerComponent.class)
public abstract class CupidPlayerComponentMixin {

    @Shadow
    private UUID markedPlayer;

    @Shadow
    public abstract Player getPlayer();

    @Shadow
    public abstract void clearMark();

    @Inject(method = "tryMarkOrLink", at = @At("HEAD"), cancellable = true)
    private void preventCannotBeLoverRolesFromBeingMarkedOrLinked(ServerPlayer target, CallbackInfo ci) {
        if (ModRoles.REIMU == null) {
            return;
        }
        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(target.serverLevel());
        if (gameWorld == null) {
            return;
        }

        if (isCannotBeLoverRole(target, gameWorld)) {
            if (getPlayer() instanceof ServerPlayer cupid) {
                cupid.displayClientMessage(Component.translatable("message.pathsrole.cannot_be_lover")
                        .withStyle(ChatFormatting.RED), true);
            }
            ci.cancel();
            return;
        }

        if (markedPlayer != null) {
            Player firstTarget = getPlayer().level().getPlayerByUUID(markedPlayer);
            if (firstTarget instanceof ServerPlayer serverFirstTarget
                    && isCannotBeLoverRole(serverFirstTarget, gameWorld)) {
                clearMark();
                if (getPlayer() instanceof ServerPlayer cupid) {
                    cupid.displayClientMessage(Component.translatable("message.pathsrole.cannot_be_lover")
                            .withStyle(ChatFormatting.RED), true);
                }
                ci.cancel();
            }
        }
    }

    private static boolean isCannotBeLoverRole(ServerPlayer player, SREGameWorldComponent gameWorld) {
        return (ModRoles.REIMU != null && gameWorld.isRole(player, ModRoles.REIMU))
                || (ModRoles.SHIPPER != null && gameWorld.isRole(player, ModRoles.SHIPPER));
    }
}