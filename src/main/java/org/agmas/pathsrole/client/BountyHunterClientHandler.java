package org.agmas.pathsrole.client;

import io.wifi.starrailexpress.client.SREClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.agmas.noellesroles.client.event.CommonHudRenderCallback;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterPlayerComponent;
import org.agmas.pathsrole.init.ModRoles;

public class BountyHunterClientHandler {
    public static void register() {
        CommonHudRenderCallback.EVENT.register((context, deltaTracker) -> {
            var client = Minecraft.getInstance();
            Player player = client.player;
            if (player == null) {
                return;
            }
            if (SREClient.gameComponent == null || !SREClient.gameComponent.isRole(player, ModRoles.BOUNTY_HUNTER)) {
                return;
            }
            if (SREClient.isPlayerSpectatingOrCreative()) {
                return;
            }
            BountyHunterPlayerComponent comp = BountyHunterPlayerComponent.KEY.get(player);
            if (comp == null) {
                return;
            }

            int yPos = context.guiHeight() - 30;
            int color = 0xCCB08060;
            int forcedColor = 0xCCFF4444;

            if (comp.getCooldownTicks() > 0) {
                Component cooldownText = Component.translatable(
                    "hud.pathsrole.bounty_hunter.cooldown", comp.getCooldownTicks() / 20);
                context.drawString(client.font, cooldownText, 4, yPos, color);
            } else if (comp.isHunting()) {
                if (comp.getTargetUUID() != null) {
                    Component targetText = Component.translatable(
                        "hud.pathsrole.bounty_hunter.hunting_target", comp.getTargetName());
                    context.drawString(client.font, targetText, 4, yPos, color);
                    yPos -= 12;
                }
                if (comp.getForcedTargetUUID() != null) {
                    Component forcedText = Component.translatable(
                        "hud.pathsrole.bounty_hunter.forced_target", comp.getForcedTargetName());
                    context.drawString(client.font, forcedText, 4, yPos, forcedColor);
                }
            } else {
                Component idleText = Component.translatable("hud.pathsrole.bounty_hunter.idle");
                context.drawString(client.font, idleText, 4, yPos, color);
            }
        });
    }
}