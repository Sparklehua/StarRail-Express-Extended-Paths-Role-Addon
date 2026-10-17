package org.agmas.pathsrole.client;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import io.wifi.utils.client.betterrender.FakeGuiGraphics;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.event.RoleHudRenderCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.agmas.pathsrole.init.ModRoles;

public class JosukeHudRenderer {
    public static void init() {
        RoleHudRenderCallback.EVENT.register(ModRoles.JOSUKE_ID,
                (context, tracker) -> renderHud(context, tracker));
    }

    private static void renderHud(FakeGuiGraphics context, DeltaTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null) {
            return;
        }

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
        if (gameWorld == null || !gameWorld.isRunning()) {
            return;
        }

        SRERole role = gameWorld.getRole(player);
        if (role == null || !role.identifier().equals(ModRoles.JOSUKE_ID)) {
            return;
        }

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        String keyName = NoellesrolesClient.abilityBind.getTranslatedKeyMessage().getString();

        Component hintText = Component.translatable("hud.pathsrole.josuke.skill_hint", keyName)
                .withStyle(ChatFormatting.AQUA);

        int textWidth = client.font.width(hintText);
        int x = (width - textWidth) / 2;
        int y = height - 68;

        context.fill(x - 4, y - 2, x + textWidth + 4, y + client.font.lineHeight + 2, 0x80000000);
        context.drawString(client.font, hintText, x, y, 0xFFFFFF);
    }
}