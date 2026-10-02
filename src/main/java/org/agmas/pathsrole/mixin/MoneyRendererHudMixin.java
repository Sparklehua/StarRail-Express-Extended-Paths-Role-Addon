package org.agmas.pathsrole.mixin;

import io.wifi.utils.client.betterrender.FakeGuiGraphics;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "io.wifi.starrailexpress.client.gui.HudStoreRenderer$MoneyNumberRenderer")
public class MoneyRendererHudMixin {

    @Unique
    private int pathsrole$balance;

    @Unique
    private boolean pathsrole$isNegative;

    @Inject(method = "setTarget", at = @At("HEAD"))
    private void captureTarget(float target, CallbackInfo ci) {
        this.pathsrole$isNegative = target < 0;
        this.pathsrole$balance = (int) target;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/Font;Lio/wifi/utils/client/betterrender/FakeGuiGraphics;IIIF)V",
            at = @At("HEAD"), cancellable = true)
    private void renderDebt(Font renderer, FakeGuiGraphics context, int x, int y, int colour, float delta, CallbackInfo ci) {
        if (this.pathsrole$isNegative) {
            ci.cancel();
            String debtText = "负债 -" + (-pathsrole$balance);
            int textWidth = renderer.width(debtText);
            context.drawString(renderer, debtText, x - textWidth, y, 0xFF5555);
        }
    }

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    private void getDebtWidth(Font renderer, CallbackInfoReturnable<Integer> cir) {
        if (this.pathsrole$isNegative) {
            String debtText = "负债 -" + (-pathsrole$balance);
            cir.setReturnValue(renderer.width(debtText));
        }
    }
}