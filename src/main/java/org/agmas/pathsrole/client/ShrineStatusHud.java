package org.agmas.pathsrole.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.agmas.noellesroles.client.event.CommonHudRenderCallback;

public class ShrineStatusHud {

    public static void register() {
        CommonHudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) return;

            String status = ShrineClientState.getStatusText();
            if (status == null) return;

            int screenWidth = graphics.guiWidth();
            Component text = Component.literal(status);
            int textWidth = client.font.width(text);
            int x = (screenWidth - textWidth) / 2;
            int y = 10;

            graphics.drawString(client.font, text, x, y, 0xFFFFFF, true);
        });
    }
}