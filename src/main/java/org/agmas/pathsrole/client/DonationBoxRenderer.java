package org.agmas.pathsrole.client;

import io.wifi.starrailexpress.client.SREClient;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import org.agmas.noellesroles.client.TaskBlockOverlayRenderer;
import org.agmas.pathsrole.content.block.DonationBoxDataManager;
import org.agmas.pathsrole.init.ModRoles;

@Environment(value=EnvType.CLIENT)
public final class DonationBoxRenderer {
    private static final Color REIMU_COLOR = new Color(255, 50, 50);

    public static void render(WorldRenderContext context) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        if (SREClient.gameComponent == null || !SREClient.gameComponent.isRunning()) {
            return;
        }
        if (!SREClient.gameComponent.isRole((Player)client.player, ModRoles.REIMU)) {
            return;
        }
        if (!SREClient.isInstinctEnabled()) {
            return;
        }
        for (BlockPos pos : DonationBoxDataManager.getClientOwnedBoxes()) {
            TaskBlockOverlayRenderer.renderBlockOverlay((WorldRenderContext)context, (BlockPos)pos, (Color)REIMU_COLOR, (float)1.0f, (boolean)true, (float)0.0f);
        }
    }
}

