package org.agmas.pathsrole;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.client.SREClient;
import io.wifi.starrailexpress.event.AllowItemShowInHand;
import io.wifi.starrailexpress.event.client.CommonInstinctEvents;
import io.wifi.starrailexpress.util.TrueFalseAndCustomResult;
import java.awt.Color;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;


import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.client.gui.components.StringWidget;
import org.agmas.noellesroles.client.event.RoleHudRenderCallback;
import org.agmas.pathsrole.client.BarrierRenderer;
import org.agmas.pathsrole.client.BountyHunterClientHandler;
import org.agmas.pathsrole.game.roles.paths.the_hunt.bountyhunter.BountyHunterPlayerComponent;
import org.agmas.pathsrole.client.DonationBoxRenderer;
import org.agmas.pathsrole.client.ReimuStabilizationHud;
import org.agmas.pathsrole.client.ReimuShieldBreakNotifier;
import org.agmas.pathsrole.client.ShipperMarkClientHandler;
import org.agmas.pathsrole.client.ShrineClientState;

import org.agmas.pathsrole.client.renderer.FlowerDollBlockEntityRenderer;
import org.agmas.pathsrole.client.renderer.YinYangOrbRenderer;
import org.agmas.pathsrole.client.renderer.ShrineGhostRenderer;
import org.agmas.pathsrole.client.renderer.ThrownFlowerDollRenderer;
import org.agmas.pathsrole.client.screen.WantedPosterScreen;
import org.agmas.pathsrole.content.item.WantedPosterItem;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.init.ModBlockEntities;
import org.agmas.pathsrole.init.ModBlocks;
import org.agmas.pathsrole.init.ModEntities;

import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.ShrineGhostStartPayload;
import org.agmas.pathsrole.network.ReimuShieldBreakPacket;
import org.agmas.pathsrole.network.ShrinePurchaseCountSyncPayload;
import org.agmas.pathsrole.network.MimiResponsePacket;
import org.agmas.pathsrole.network.BanListResponsePayload;
import org.agmas.pathsrole.network.PlayerVisibilityStatePayload;
import org.agmas.pathsrole.client.screen.MimiScreen;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PathsRoleModClient
implements ClientModInitializer {

    private static final Set<UUID> hiddenPlayerUuids = new HashSet<>();

    /**
     * 检查指定玩家是否被当前客户端隐藏（受 {@link org.agmas.pathsrole.api.PlayerVisibilityAPI} 控制）。
     */
    public static boolean isPlayerHidden(UUID playerUuid) {
        return hiddenPlayerUuids.contains(playerUuid);
    }

    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ReimuShieldBreakPacket.ID, (payload, context) -> {
            context.client().execute(() -> ReimuShieldBreakNotifier.markShouldSkip());
        });
        ClientPlayNetworking.registerGlobalReceiver(ShrineGhostStartPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                ShrineGhostRenderer.onGhostStart(
                        payload.startTimeMs(),
                        payload.minX(), payload.minY(), payload.minZ(),
                        payload.maxX(), payload.maxY(), payload.maxZ(),
                        payload.playerStartY());
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(MimiResponsePacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.allowed()) {
                    context.client().setScreen(new MimiScreen());
                } else {
                    if (context.client().player != null) {
                        context.client().player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.pathsrole.mimi.denied").withStyle(net.minecraft.ChatFormatting.RED),
                            true
                        );
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(BanListResponsePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                BanListResponsePayload.updateCache(payload.playerNames(), payload.bannedPlayers());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ShrinePurchaseCountSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                org.agmas.pathsrole.client.screen.ShrineShopScreen.KILLER_REMAINING = payload.killerRemaining();
                org.agmas.pathsrole.client.screen.ShrineShopScreen.NEUTRAL_KILLER_REMAINING = payload.neutralKillerRemaining();
                org.agmas.pathsrole.client.screen.ShrineShopScreen.SPECIAL_NEUTRAL_REMAINING = payload.specialNeutralRemaining();
                org.agmas.pathsrole.client.screen.ShrineShopScreen.INNOCENT_REMAINING = payload.innocentRemaining();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(PlayerVisibilityStatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                hiddenPlayerUuids.clear();
                hiddenPlayerUuids.addAll(payload.hiddenPlayerUuids());
            });
        });

        AllowItemShowInHand.EVENT.register((player, itemStack, mainHand) -> {
            if (itemStack.is((Item)ModItems.SHIPPER_BOOK)) {
                return ItemStack.EMPTY;
            }
            return null;
        });
        AllowItemShowInHand.EVENT.register((player, itemStack, mainHand) -> {
            if (itemStack.is(ModItems.PEEPING_EYE)) {
                return ItemStack.EMPTY;
            }
            return null;
        });
        
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BarrierRenderer::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(DonationBoxRenderer::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ShrineGhostRenderer::render);
        
        WorldRenderEvents.AFTER_SETUP.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (ShrineClientState.shouldApplyFog(mc.player.getX(), mc.player.getY(), mc.player.getZ())) {
                RenderSystem.setShaderFogStart(2.0f);
                RenderSystem.setShaderFogEnd(20.0f);
            }
        });
        ShipperMarkClientHandler.register();
        ReimuStabilizationHud.register();
        BountyHunterClientHandler.register();
        WantedPosterItem.openGuiRunner = () -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null)
                return;
            client.execute(() -> {
                client.setScreen(new WantedPosterScreen());
            });
        };
        EntityRendererRegistry.register(ModEntities.YIN_YANG_ORB, YinYangOrbRenderer::new);
        EntityRendererRegistry.register(ModEntities.THROWN_FLOWER_DOLL, ThrownFlowerDollRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLOWER_DOLL, RenderType.cutout());
        BlockEntityRendererRegistry.register(ModBlockEntities.FLOWER_DOLL, FlowerDollBlockEntityRenderer::new);
        CommonInstinctEvents.ALIVE_COMMON_BEFORE_EVENT.register((self, target, hasInstinct) -> {
            if (target == null) {
                return TrueFalseAndCustomResult.pass();
            }
            if (target instanceof Player) {
                Player targetPlayer = (Player)target;
                if (isPlayerHidden(targetPlayer.getUUID())) {
                    return TrueFalseAndCustomResult.disallow();
                }
                if (SREClient.gameComponent != null && SREClient.gameComponent.isRole(targetPlayer, ModRoles.SHIPPER)) {
                    ShipperPlayerComponent shipperComp = ShipperPlayerComponent.KEY.get((Object)targetPlayer);
                    if (shipperComp != null && shipperComp.isObservationActive() && shipperComp.getInvisibilityAlpha() <= 0.01f) {
                        return TrueFalseAndCustomResult.disallow();
                    }
                    return TrueFalseAndCustomResult.pass();
                }
                if (SREClient.gameComponent != null && SREClient.gameComponent.isRole(targetPlayer, ModRoles.REIMU)) {
                    SRERole selfRole = SREClient.gameComponent.getRole(self);
                    if (selfRole != null && selfRole.isKillerTeam()) {
                        return TrueFalseAndCustomResult.pass();
                    }
                    return TrueFalseAndCustomResult.disallow();
                }
            }
            return TrueFalseAndCustomResult.pass();
        });
        CommonInstinctEvents.ALIVE_COMMON_AFTER_EVENT.register((self, target, hasInstinct) -> {
            if (!(target instanceof Player)) {
                return TrueFalseAndCustomResult.pass();
            }
            Player targetPlayer = (Player)target;
            if (SREClient.gameComponent == null) {
                return TrueFalseAndCustomResult.pass();
            }
            if (!SREClient.gameComponent.isRole((Player)self, ModRoles.SHIPPER)) {
                return TrueFalseAndCustomResult.pass();
            }
            ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)self);
            if (shipperComp == null) {
                return TrueFalseAndCustomResult.pass();
            }
            if (shipperComp.isRageActive() && shipperComp.getRageKiller() != null && shipperComp.getRageKiller().equals(targetPlayer.getUUID())) {
                return TrueFalseAndCustomResult.custom(Integer.valueOf(new Color(178, 34, 34).getRGB()));
            }
            if (!hasInstinct) {
                return TrueFalseAndCustomResult.pass();
            }
            if (targetPlayer.distanceToSqr((Entity)self) > 1600.0) {
                return TrueFalseAndCustomResult.disallow();
            }
            if (shipperComp.getPairedLovers().contains(targetPlayer.getUUID())) {
                return TrueFalseAndCustomResult.custom(Integer.valueOf(new Color(255, 105, 180).getRGB()));
            }
            return TrueFalseAndCustomResult.custom(Integer.valueOf(Color.GRAY.getRGB()));
        });
        CommonInstinctEvents.ALIVE_COMMON_AFTER_EVENT.register((self, target, hasInstinct) -> {
            if (!(target instanceof Player targetPlayer)) {
                return TrueFalseAndCustomResult.pass();
            }
            if (!(self instanceof Player selfPlayer)) {
                return TrueFalseAndCustomResult.pass();
            }
            if (SREClient.gameComponent == null) {
                return TrueFalseAndCustomResult.pass();
            }
            if (!SREClient.gameComponent.isRole(selfPlayer, ModRoles.BOUNTY_HUNTER)) {
                return TrueFalseAndCustomResult.pass();
            }
            if (!hasInstinct) {
                return TrueFalseAndCustomResult.pass();
            }
            BountyHunterPlayerComponent bhComp = BountyHunterPlayerComponent.KEY.get(self);
            if (bhComp != null && bhComp.isHunting() && bhComp.getTargetUUID() != null
                    && bhComp.getTargetUUID().equals(targetPlayer.getUUID())) {
                return TrueFalseAndCustomResult.custom(new Color(178, 34, 34).getRGB());
            }
            SRERole targetRole = SREClient.gameComponent.getRole(targetPlayer);
            if (targetRole != null && targetRole.isKillerTeam()) {
                return TrueFalseAndCustomResult.custom(new Color(139, 90, 43).getRGB());
            }
            return TrueFalseAndCustomResult.disallow();
        });
        RoleHudRenderCallback.EVENT.register(ModRoles.REIMU.identifier(), (guiGraphics, deltaTracker) -> {
            var client = Minecraft.getInstance();
            if (client == null || client.player == null || client.level == null) {
                return;
            }
            if (SREClient.gameComponent == null || !SREClient.gameComponent.isRole((Player)client.player, ModRoles.REIMU)) {
                return;
            }
            int civilianCount = 0;
            int killerCount = 0;
            for (Player player : client.level.players()) {
                SRERole role;
                if (!player.isAlive() || player.isSpectator() || player.isCreative() || (role = SREClient.gameComponent.getRole(player)) == null || role == ModRoles.REIMU) continue;
                if (role.isInnocent()) {
                    ++civilianCount;
                    continue;
                }
                if (!role.canUseKiller() || role.isNeutralForKiller()) continue;
                ++killerCount;
            }
            Font font = Minecraft.getInstance().font;
            Component text = Component.empty().append(Component.translatable("message.pathsrole.reimu.ratio.civilian").withStyle(ChatFormatting.GREEN)).append(Component.literal(" " + civilianCount + "  ").withStyle(ChatFormatting.GREEN)).append(Component.literal("|  ").withStyle(ChatFormatting.GRAY)).append(Component.translatable("message.pathsrole.reimu.ratio.killer").withStyle(ChatFormatting.RED)).append(Component.literal(" " + killerCount).withStyle(ChatFormatting.RED));
            int x = 10;
            int y = guiGraphics.guiHeight() - 20;
            guiGraphics.drawString(font, text, x, y, 0xFFFFFF, true);
        });
        RoleHudRenderCallback.EVENT.register(ModRoles.SHIPPER.identifier(), (guiGraphics, deltaTracker) -> {
            long remaining;
            var client = Minecraft.getInstance();
            if (client == null || client.player == null) {
                return;
            }
            if (SREClient.gameComponent == null || !SREClient.gameComponent.isRole((Player)client.player, ModRoles.SHIPPER)) {
                return;
            }
            ShipperPlayerComponent shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)client.player);
            if (shipperComp == null) {
                return;
            }
            if (shipperComp.isObservationActive() && (remaining = shipperComp.getObservationRemainingSeconds()) > 0L) {
                int screenWidth = guiGraphics.guiWidth();
                int screenHeight = guiGraphics.guiHeight();
                Font font = Minecraft.getInstance().font;
                Component text = Component.translatable("message.pathsrole.shipper.observation_cooldown", new Object[]{remaining}).withStyle(ChatFormatting.GOLD);
                int textWidth = font.width(text);
                int x = (screenWidth - textWidth) / 2;
                int y = screenHeight - 68;
                guiGraphics.drawString(font, text, x, y, 16766720, true);
            }
        });
    }
}