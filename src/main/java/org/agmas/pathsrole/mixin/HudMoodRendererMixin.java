package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.client.gui.HudMoodRenderer;
import io.wifi.utils.client.betterrender.FakeGuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {HudMoodRenderer.class})
public class HudMoodRendererMixin {

    private static final ResourceLocation MOOD_REIMU = ResourceLocation.fromNamespaceAndPath("pathsrole", "hud/mood_reimu");
    private static final ResourceLocation MOOD_SHIPPER = ResourceLocation.fromNamespaceAndPath("pathsrole", "hud/mood_shipper");

    @Redirect(method = "renderCivilian",
              at = @At(value = "INVOKE",
                       target = "Lio/wifi/utils/client/betterrender/FakeGuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"),
              require = 0)
    private static void redirectCivilianMoodSprite(FakeGuiGraphics instance, ResourceLocation sprite, int x, int y, int width, int height,
                                                    Font textRenderer, FakeGuiGraphics context, float prevMood, int color, SRERole role) {
        ResourceLocation newSprite = getCustomMoodSprite(role, sprite);
        instance.blitSprite(newSprite, x, y, width, height);
    }

    @Redirect(method = "renderKiller",
              at = @At(value = "INVOKE",
                       target = "Lio/wifi/utils/client/betterrender/FakeGuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"),
              require = 0)
    private static void redirectKillerMoodSprite(FakeGuiGraphics instance, ResourceLocation sprite, int x, int y, int width, int height,
                                                  Font textRenderer, FakeGuiGraphics context, int color, SRERole role) {
        ResourceLocation newSprite = getCustomMoodSprite(role, sprite);
        instance.blitSprite(newSprite, x, y, width, height);
    }

    private static ResourceLocation getCustomMoodSprite(SRERole role, ResourceLocation defaultSprite) {
        if (role == null) {
            return defaultSprite;
        }
        if (role.identifier().equals(ModRoles.REIMU.identifier())) {
            return MOOD_REIMU;
        }
        if (role.identifier().equals(ModRoles.SHIPPER.identifier())) {
            return MOOD_SHIPPER;
        }
        return defaultSprite;
    }
}