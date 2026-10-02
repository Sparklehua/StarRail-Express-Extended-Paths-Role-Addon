package org.agmas.pathsrole.client.screen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record PreviewState(
        ResourceLocation texture,
        float[] boneRotations,
        float modelRotY,
        float modelRotX,
        float scale
) {
    public static final ResourceLocation STEVE =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public static PreviewState createDefault() {
        return new PreviewState(STEVE, new float[15], 150f, 10f, 1.0f);
    }

    public PreviewState withTexture(ResourceLocation tex) {
        return new PreviewState(tex != null ? tex : STEVE, boneRotations, modelRotY, modelRotX, scale);
    }

    public PreviewState withBones(float[] bones) {
        return new PreviewState(texture, bones, modelRotY, modelRotX, scale);
    }

    public PreviewState withModelRotation(float y, float x) {
        return new PreviewState(texture, boneRotations, y, x, scale);
    }

    public PreviewState withScale(float s) {
        return new PreviewState(texture, boneRotations, modelRotY, modelRotX, s);
    }
}