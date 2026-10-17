package org.agmas.pathsrole.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraPigHeightMixin {

    @Shadow
    private Entity entity;

    @Shadow
    private Vec3 position;

    @Shadow
    private float eyeHeight;

    @Shadow
    private float eyeHeightOld;

    @Inject(method = "setup", at = @At("TAIL"))
    private void adjustPigCamera(BlockGetter area, Entity focusedEntity, boolean thirdPerson,
                                  boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!(focusedEntity instanceof Player))
            return;
        PigPlayerComponent comp = PathsroleComponents.PIG_PLAYER_KEY.getNullable(focusedEntity);
        if (comp != null && comp.isInPigForm()) {
            float interpY = Mth.lerp(tickDelta, (float) focusedEntity.yOld, (float) focusedEntity.getY());
            float targetY = interpY + 0.7F;
            this.position = new Vec3(this.position.x, targetY, this.position.z);
            this.eyeHeight = 0.7F;
            this.eyeHeightOld = 0.7F;
        }
    }
}