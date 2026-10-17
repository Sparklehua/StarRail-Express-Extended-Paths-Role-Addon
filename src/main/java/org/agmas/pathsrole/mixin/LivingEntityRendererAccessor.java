package org.agmas.pathsrole.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    @Invoker("addLayer")
    boolean invokeAddLayer(RenderLayer<?, ?> layer);

    @Invoker("getAttackAnim")
    float invokeGetAttackAnim(LivingEntity entity, float tickDelta);
}