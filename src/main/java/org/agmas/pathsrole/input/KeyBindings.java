package org.agmas.pathsrole.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyMapping RECALL_SHEER_HEART;

    public static void init() {
        RECALL_SHEER_HEART = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.pathsrole.recall_sheer_heart",
                GLFW.GLFW_KEY_V,
                "key.categories.pathsrole"
        ));
    }
}