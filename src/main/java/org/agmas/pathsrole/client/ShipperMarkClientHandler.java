package org.agmas.pathsrole.client;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.client.SREClient;
import io.wifi.starrailexpress.event.client.CommonInstinctEvents;
import io.wifi.starrailexpress.util.TrueFalseAndCustomResult;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.client.event.CommonHudRenderCallback;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.init.ModModifiers;
import org.agmas.pathsrole.init.ModRoles;

public class ShipperMarkClientHandler {
    public static void register() {
        CommonInstinctEvents.ALIVE_COMMON_MIDDLE_EVENT.register((self, target, isInstinctEnabled) -> {
            if (!(target instanceof Player)) {
                return TrueFalseAndCustomResult.pass();
            }
            WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)self.level());
            if (wmc == null || !wmc.isModifier((Player)self, ModModifiers.SHIPPER_MARK)) {
                return TrueFalseAndCustomResult.pass();
            }
            if (!wmc.isModifier((Player)target, ModModifiers.SHIPPER_MARK)) {
                return TrueFalseAndCustomResult.pass();
            }
            for (Player p : self.level().players()) {
                if (SREClient.gameComponent == null || !SREClient.gameComponent.isRole(p, ModRoles.SHIPPER)) continue;
                ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)p);
                if (shipperComp == null) continue;
                long pairingTime = shipperComp.getPairingTime();
                if (pairingTime > 0L && System.currentTimeMillis() - pairingTime < 10000L) {
                    return TrueFalseAndCustomResult.pass();
                }
                break;
            }
            return TrueFalseAndCustomResult.custom(ModModifiers.SHIPPER_MARK.color());
        });
        CommonHudRenderCallback.EVENT.register((context, deltaTracker) -> {
            var client = Minecraft.getInstance();
            net.minecraft.world.entity.player.Player player = client.player;
            if (player == null) {
                return;
            }
            WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)player.level());
            if (wmc == null) {
                return;
            }
            if (!wmc.isModifier((Player)player, ModModifiers.SHIPPER_MARK)) {
                return;
            }
            if (SREClient.isPlayerSpectatingOrCreative()) {
                return;
            }
            if (client.player.connection == null) {
                return;
            }
            for (Player p : player.level().players()) {
                PlayerInfo loverInfo;
                List<UUID> lovers;
                if (SREClient.gameComponent == null || !SREClient.gameComponent.isRole(p, ModRoles.SHIPPER)) continue;
                ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)p);
                if (shipperComp == null || !(lovers = shipperComp.getPairedLovers()).contains(player.getUUID())) break;
                long pairingTime = shipperComp.getPairingTime();
                if (pairingTime > 0L && System.currentTimeMillis() - pairingTime < 10000L) break;
                UUID otherUUID = null;
                for (UUID uuid : lovers) {
                    if (uuid.equals(player.getUUID())) continue;
                    otherUUID = uuid;
                    break;
                }
                if (otherUUID == null || (loverInfo = client.player.connection.getPlayerInfo(otherUUID)) == null) break;
                if (loverInfo.getSkin() == null) break;
                SRERole role = SREClient.gameComponent.getRole(otherUUID);
                Component displayText = Component.literal(loverInfo.getProfile().getName() + " - ").append(role != null ? role.getName() : Component.literal("?"));
                context.pose().pushPose();
                int textYPos = context.guiHeight() - 12;
                context.blit(loverInfo.getSkin().texture(), 2, textYPos - 2, 12, 12, 8, 8, 8, 8, 64, 64);
                context.drawString(client.font, displayText, 18, textYPos, ModModifiers.SHIPPER_MARK.color());
                context.pose().popPose();
                break;
            }
        });
        CommonHudRenderCallback.EVENT.register((context, deltaTracker) -> {
            var client = Minecraft.getInstance();
            net.minecraft.world.entity.player.Player player = client.player;
            if (player == null) {
                return;
            }
            WorldModifierComponent wmc = (WorldModifierComponent)WorldModifierComponent.KEY.get((Object)player.level());
            if (wmc == null) {
                return;
            }
            if (!wmc.isModifier((Player)player, ModModifiers.SHIPPER_MARK)) {
                return;
            }
            if (SREClient.isPlayerSpectatingOrCreative()) {
                return;
            }
            for (Player p : player.level().players()) {
                if (SREClient.gameComponent == null || !SREClient.gameComponent.isRole(p, ModRoles.SHIPPER)) continue;
                ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)p);
                if (shipperComp == null) break;
                long pairingTime = shipperComp.getPairingTime();
                long elapsed = System.currentTimeMillis() - pairingTime;
                if (pairingTime <= 0L || elapsed < 10000L || elapsed >= 20000L || shipperComp.isShipperMomentActive()) break;
                Component hintText = Component.translatable("message.pathsrole.shipper.became_lover_hud");
                int alpha = -16777216;
                if (elapsed > 17000L) {
                    float fadeProgress = (float)(20000L - elapsed) / 3000.0f;
                    int a = (int)(255.0f * fadeProgress);
                    alpha = a << 24;
                }
                int textWidth = client.font.width(hintText);
                int x = (context.guiWidth() - textWidth) / 2;
                int y = context.guiHeight() - 55;
                context.pose().pushPose();
                context.drawString(client.font, hintText, x, y, ModModifiers.SHIPPER_MARK.color() | alpha);
                context.pose().popPose();
                break;
            }
        });
    }
}