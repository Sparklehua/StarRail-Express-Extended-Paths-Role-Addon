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
    private static final ResourceLocation MOOD_KIRA = ResourceLocation.fromNamespaceAndPath("pathsrole", "hud/mood_kira");
    private static final ResourceLocation MOOD_JOSUKE = ResourceLocation.fromNamespaceAndPath("pathsrole", "hud/mood_josuke");

    @Redirect(method = "renderCivilian",
              at = @At(value = "INVOKE",
                       target = "Lio/wifi/utils/client/betterrender/FakeGuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"),
              require = 0)
    private static void redirectCivilianMoodSprite(FakeGuiGraphics instance, ResourceLocation sprite, int x, int y, int width, int height,
                                                    Font textRenderer, FakeGuiGraphics context, float prevMood, int color, SRERole role) {
        if (instance == null || sprite == null) {
            return;
        }
        ResourceLocation newSprite = getCustomMoodSprite(role, sprite);
        if (newSprite == null) {
            return;
        }
        instance.blitSprite(newSprite, x, y, width, height);
    }

    @Redirect(method = "renderKiller",
              at = @At(value = "INVOKE",
                       target = "Lio/wifi/utils/client/betterrender/FakeGuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"),
              require = 0)
    private static void redirectKillerMoodSprite(FakeGuiGraphics instance, ResourceLocation sprite, int x, int y, int width, int height,
                                                  Font textRenderer, FakeGuiGraphics context, int color, SRERole role) {
        if (instance == null) {
            return;
        }
        ResourceLocation newSprite = getCustomMoodSprite(role, sprite);
        if (newSprite == null || sprite == null || newSprite.equals(sprite)) {
            if (sprite != null) {
                instance.blitSprite(sprite, x, y, width, height);
            }
            return;
        }
        int scaledWidth = width * 2;
        int scaledHeight = height * 2;
        if (role != null && role.identifier() != null && ModRoles.KIRA != null && role.identifier().equals(ModRoles.KIRA.identifier())) {
            scaledWidth = (int)(scaledWidth * 0.85);
        }
        int cx = x - (scaledWidth - width) / 2;
        int cy = y - (scaledHeight - height) / 2;
        instance.blitSprite(newSprite, cx, cy, scaledWidth, scaledHeight);
    }

    private static ResourceLocation getCustomMoodSprite(SRERole role, ResourceLocation defaultSprite) {
        if (role == null || defaultSprite == null) {
            return defaultSprite;
        }
        ResourceLocation roleId = role.identifier();
        if (roleId == null) {
            return defaultSprite;
        }
        if (ModRoles.REIMU != null && roleId.equals(ModRoles.REIMU.identifier())) {
            return MOOD_REIMU;
        }
        if (ModRoles.SHIPPER != null && roleId.equals(ModRoles.SHIPPER.identifier())) {
            return MOOD_SHIPPER;
        }
        if (ModRoles.KIRA != null && roleId.equals(ModRoles.KIRA.identifier())) {
            return MOOD_KIRA;
        }
        if (ModRoles.JOSUKE != null && roleId.equals(ModRoles.JOSUKE.identifier())) {
            return MOOD_JOSUKE;
        }
        return defaultSprite;
    }
}