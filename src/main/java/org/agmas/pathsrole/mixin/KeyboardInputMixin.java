package org.agmas.pathsrole.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void pathsrole$restoreReimuJumping(boolean slowDown, float f, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;
        ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent(client.player);
        if (comp == null || comp.getJumpAllowedTicks() <= 0) return;

        Input self = (Input)(Object)this;
        long window = client.getWindow().getWindow();
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            if (!self.jumping) {
                self.jumping = true;
            }
            client.player.getAbilities().mayfly = true;
            client.player.getAbilities().flying = true;
        }
    }
}