package org.agmas.pathsrole.client;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import io.wifi.utils.client.betterrender.FakeGuiGraphics;
import org.agmas.noellesroles.client.event.RoleHudRenderCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.elation.kira.KiraPlayerComponent;
import org.agmas.pathsrole.init.ModRoles;

public class KiraHudRenderer {
    private static final int[] KILL_THRESHOLDS = {3, 7, 12, 15};

    public static void init() {
        RoleHudRenderCallback.EVENT.register(ModRoles.KIRA_ID,
                (context, tracker) -> renderHud(context, tracker));
    }

    private static void renderHud(FakeGuiGraphics context, DeltaTracker tracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        
        if (player == null) return;

        SREGameWorldComponent gameWorld = SREGameWorldComponent.KEY.get(player.level());
        if (gameWorld == null || !gameWorld.isRunning()) return;

        SRERole role = gameWorld.getRole(player);
        if (role == null || !role.identifier().equals(ModRoles.KIRA_ID)) return;

        KiraPlayerComponent kiraComponent = PathsroleComponents.KIRA_PLAYER_KEY.getNullable(player);
        
        if (kiraComponent == null) return;
        
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        
        int baseX = width - 180;
        int baseY = height - 100;

        int lineHeight = 12;

        int killCount = kiraComponent.getKillCount();
        int charges = kiraComponent.getBitesTheDustCharges();
        int used = kiraComponent.getBitesTheDustUsedCount();
        int remaining = charges - used;

        Component killText = Component.translatable("hud.pathsrole.kira.kill_count", killCount)
                .withStyle(ChatFormatting.GOLD);
        context.drawString(client.font, killText, baseX, baseY, 0xFFFFFF);

        int progressBarWidth = 150;
        int progressBarHeight = 8;
        context.fill(baseX, baseY + 14, baseX + progressBarWidth, baseY + 14 + progressBarHeight, 0x44000000);

        int nextThresholdIndex = 0;
        for (int i = 0; i < KILL_THRESHOLDS.length; i++) {
            if (killCount >= KILL_THRESHOLDS[i]) {
                nextThresholdIndex = i + 1;
            }
        }

        int currentProgress;
        if (nextThresholdIndex >= KILL_THRESHOLDS.length) {
            currentProgress = progressBarWidth;
        } else if (nextThresholdIndex == 0) {
            currentProgress = (killCount * progressBarWidth) / KILL_THRESHOLDS[0];
        } else {
            int prevThreshold = KILL_THRESHOLDS[nextThresholdIndex - 1];
            int nextThreshold = KILL_THRESHOLDS[nextThresholdIndex];
            int segmentProgress = killCount - prevThreshold;
            int segmentTotal = nextThreshold - prevThreshold;
            int completedWidth = (prevThreshold * progressBarWidth) / KILL_THRESHOLDS[KILL_THRESHOLDS.length - 1];
            int segmentWidth = (segmentProgress * progressBarWidth) / (KILL_THRESHOLDS[KILL_THRESHOLDS.length - 1] - prevThreshold);
            currentProgress = completedWidth + segmentWidth;
        }

        int progressColor;
        if (killCount >= 15) {
            progressColor = 0xFFD700;
        } else if (killCount >= 12) {
            progressColor = 0x00FF00;
        } else if (killCount >= 7) {
            progressColor = 0x00FFFF;
        } else if (killCount >= 3) {
            progressColor = 0xFF00FF;
        } else {
            progressColor = 0xFF6600;
        }

        context.fill(baseX, baseY + 14, baseX + Math.min(currentProgress, progressBarWidth), 
                baseY + 14 + progressBarHeight, progressColor);

        int markY = baseY + 14 + progressBarHeight;
        for (int i = 0; i < KILL_THRESHOLDS.length; i++) {
            int markX = baseX + (KILL_THRESHOLDS[i] * progressBarWidth) / KILL_THRESHOLDS[KILL_THRESHOLDS.length - 1];
            context.fill(markX - 1, markY, markX + 1, markY + 4, 0xFFFFFF);
        }

        String bitesDisplay = String.format("败者食尘（剩余 %d 次）", Math.max(0, remaining));
        String usageText = String.format("已使用次数：%d", used);
        
        Component bitesText = Component.literal(bitesDisplay)
                .withStyle(charges > 0 ? ChatFormatting.DARK_RED : ChatFormatting.GRAY);
        context.drawString(client.font, bitesText, baseX, baseY + 28, 0xFFFFFF);
        
        Component usageComponent = Component.literal(usageText)
                .withStyle(charges > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        context.drawString(client.font, usageComponent, baseX, baseY + 40, 0xFFFFFF);

        if (kiraComponent.hasMarkedPosition()) {
            Component posText = Component.translatable("hud.pathsrole.kira.position_marked")
                    .withStyle(ChatFormatting.GREEN);
            context.drawString(client.font, posText, baseX, baseY + 52, 0xFFFFFF);
        }

        int cooldown = kiraComponent.getDetonateCooldown();
        if (cooldown > 0) {
            int seconds = (cooldown + 19) / 20;
            Component cooldownText = Component.translatable("hud.pathsrole.kira.detonate_cooldown", seconds)
                    .withStyle(ChatFormatting.RED);
            context.drawString(client.font, cooldownText, baseX, baseY + 64, 0xFFFFFF);
        }

        int markCount = kiraComponent.getMarkCount();
        Component markText = Component.translatable("hud.pathsrole.kira.mark_count", markCount, KiraPlayerComponent.MAX_MARKS)
                .withStyle(ChatFormatting.BLUE);
        context.drawString(client.font, markText, baseX, baseY + 76, 0xFFFFFF);

        int markCooldown = kiraComponent.getMarkCooldown();
        if (markCooldown > 0) {
            int seconds = (markCooldown + 19) / 20;
            Component cooldownText = Component.translatable("hud.pathsrole.kira.mark_cooldown", seconds)
                    .withStyle(ChatFormatting.RED);
            int cooldownX = (width - client.font.width(cooldownText)) / 2;
            context.drawString(client.font, cooldownText, cooldownX, height - 48, 0xFFFFFF);
        }
    }
}