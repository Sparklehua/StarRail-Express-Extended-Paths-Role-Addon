package org.agmas.pathsrole.client;

import io.wifi.starrailexpress.client.SREClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.agmas.noellesroles.client.event.CommonHudRenderCallback;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.equilibrium.reimu.ReimuPlayerComponent;
import org.agmas.pathsrole.init.ModRoles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReimuStabilizationHud {

    private static final int TOTAL_TICKS = 500;
    private static final int BAR_WIDTH = 200;
    private static final int BAR_HEIGHT = 8;
    private static final int BAR_Y = 35;
    private static final Map<UUID, Float> smoothedProgressMap = new HashMap<>();
    private static final Map<UUID, Integer> lastTimerMap = new HashMap<>();
    private static int cleanupCounter = 0;

    public static void register() {
        CommonHudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) return;
            if (SREClient.gameComponent == null || !SREClient.gameComponent.isRunning()) return;
            if (client.player.isSpectator() || !client.player.isAlive()) return;

            for (Player player : client.level.players()) {
                if (!SREClient.gameComponent.isRole(player, ModRoles.REIMU)) continue;
                ReimuPlayerComponent comp = PathsroleComponents.getReimuComponent(player);
                if (comp == null || comp.getStabilizationTimer() <= 0) continue;

                UUID playerId = player.getUUID();
                float smoothedProgress = smoothedProgressMap.getOrDefault(playerId, 0f);
                int lastTimer = lastTimerMap.getOrDefault(playerId, -1);

                int timer = comp.getStabilizationTimer();

                float targetProgress = (float) (TOTAL_TICKS - timer) / TOTAL_TICKS;
                targetProgress = Math.max(0, Math.min(1, targetProgress));

                if (timer > lastTimer) {
                    smoothedProgress = targetProgress;
                } else {
                    smoothedProgress += (targetProgress - smoothedProgress) * 0.3f;
                }
                lastTimer = timer;

                if (smoothedProgress < 0) smoothedProgress = 0;
                if (smoothedProgress > 1) smoothedProgress = 1;

                smoothedProgressMap.put(playerId, smoothedProgress);
                lastTimerMap.put(playerId, lastTimer);

                int screenWidth = graphics.guiWidth();
                int x = (screenWidth - BAR_WIDTH) / 2;
                int y = BAR_Y;

                Component label = Component.translatable("hud.pathsrole.reimu.stabilizing").withStyle(ChatFormatting.GOLD);
                int labelWidth = client.font.width(label);
                graphics.drawString(client.font, label, (screenWidth - labelWidth) / 2, y - 14, 0xFFFFFF);

                var gfx = graphics.getDefaultGuiGraphics();

                gfx.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x80000000);

                int progressWidth = (int) (BAR_WIDTH * smoothedProgress);
                if (progressWidth > 0) {
                    gfx.fill(x, y, x + progressWidth, y + BAR_HEIGHT, 0xFFFF0000);
                }

                gfx.fill(x, y, x + BAR_WIDTH, y + 1, 0xFFFFFFFF);
                gfx.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFFFFFFFF);
                gfx.fill(x, y, x + 1, y + BAR_HEIGHT, 0xFFFFFFFF);
                gfx.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFFFFFFFF);

                return;
            }

            if (++cleanupCounter > 300) {
                cleanupCounter = 0;
                smoothedProgressMap.keySet().removeIf(id -> {
                    for (Player p : client.level.players()) {
                        if (p.getUUID().equals(id)) return false;
                    }
                    return true;
                });
                lastTimerMap.keySet().removeIf(id -> {
                    for (Player p : client.level.players()) {
                        if (p.getUUID().equals(id)) return false;
                    }
                    return true;
                });
            }
        });
    }
}