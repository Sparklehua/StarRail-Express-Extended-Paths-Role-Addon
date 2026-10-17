package org.agmas.pathsrole.client.widget;

import io.wifi.starrailexpress.client.SREClient;
import io.wifi.starrailexpress.client.gui.screen.ingame.LimitedInventoryScreen;
import io.wifi.starrailexpress.cca.SREPlayerShopComponent;
import io.wifi.starrailexpress.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import org.agmas.pathsrole.PathsRoleMod;
import org.agmas.pathsrole.cca.PathsroleComponents;
import org.agmas.pathsrole.game.roles.paths.elation.kira.KiraPlayerComponent;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.KiraC2SPacket;
import org.jetbrains.annotations.NotNull;

public class KiraPlayerWidget extends Button {
    private static final ResourceLocation KIRA_MARK_OVERLAY = ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "textures/gui/kira_mark_overlay.png");
    private static final ResourceLocation KIRA_MARK_CORNER = ResourceLocation.fromNamespaceAndPath(PathsRoleMod.MOD_ID, "textures/gui/kira_mark_corner.png");
    public final LimitedInventoryScreen screen;
    public final PlayerInfo displayTarget;
    private Component displayText = Component.empty();
    private java.util.List<net.minecraft.util.FormattedCharSequence> cachedLines = new java.util.ArrayList<>();

    public KiraPlayerWidget(LimitedInventoryScreen screen, int x, int y, @NotNull PlayerInfo displayTarget) {
        super(x, y, 16, 16, Component.nullToEmpty(displayTarget.getProfile().getName()), (a) -> {
            AbstractClientPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                KiraPlayerComponent kiraComponent = PathsroleComponents.KIRA_PLAYER_KEY.get(player);
                
                boolean isSelfOrTeammate = false;
                if (displayTarget.getProfile().getId().equals(player.getUUID())) {
                    isSelfOrTeammate = true;
                } else if (SREClient.gameComponent != null && SREClient.gameComponent.isKillerTeam(displayTarget.getProfile().getId())) {
                    var targetRole = SREClient.gameComponent.getRole(displayTarget.getProfile().getId());
                    if (!ModRoles.LOST_KILLER_ID.equals(targetRole.identifier())) {
                        isSelfOrTeammate = true;
                    }
                }
                
                if (isSelfOrTeammate) {
                    ClientPlayNetworking.send(new KiraC2SPacket(displayTarget.getProfile().getId(), KiraC2SPacket.ACTION_TOGGLE_JEB));
                } else {
                    player.displayClientMessage(Component.translatable("message.pathsrole.kira.cannot_toggle_jeb"), true);
                }
            }
        }, DEFAULT_NARRATION);
        this.screen = screen;
        this.displayTarget = displayTarget;
        updateDisplayText();
    }
    
    private void updateDisplayText() {
        if (displayTarget.getGameMode() != GameType.ADVENTURE) {
            setDisplayText(Component.translatable("hud.general.dead").withStyle(ChatFormatting.DARK_RED));
        } else {
            if (SREClient.gameComponent != null
                    && SREClient.gameComponent.getRole(displayTarget.getProfile().getId()) != null
                    && SREClient.gameComponent.isKillerTeam(displayTarget.getProfile().getId())) {
                var targetRole = SREClient.gameComponent.getRole(displayTarget.getProfile().getId());
                if (!ModRoles.LOST_KILLER_ID.equals(targetRole.identifier())) {
                    setDisplayText(Component.translatable("hud.pathsrole.general.killer_friend").withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) {
            return false;
        }
        
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        
        if (button == 1) {
            AbstractClientPlayer player = Minecraft.getInstance().player;
            if (player == null) return false;
            
            KiraPlayerComponent kiraComponent = PathsroleComponents.KIRA_PLAYER_KEY.get(player);
            SREPlayerShopComponent shopComponent = SREPlayerShopComponent.KEY.get(player);
            
            if (kiraComponent.isMarked(displayTarget.getProfile().getId())) {
                if (shopComponent.balance >= 90) {
                    ClientPlayNetworking.send(new KiraC2SPacket(displayTarget.getProfile().getId(), KiraC2SPacket.ACTION_DETONATE));
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                } else {
                    player.displayClientMessage(Component.translatable("message.noellesroles.insufficient_funds"), true);
                }
            } else {
                player.displayClientMessage(Component.translatable("message.pathsrole.kira.backpack_cannot_mark").withStyle(ChatFormatting.RED), true);
            }
            return true;
        }
        
        if (button == 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        KiraPlayerComponent kiraComponent = PathsroleComponents.KIRA_PLAYER_KEY.getNullable(player);
        if (kiraComponent == null) return;
        boolean isMarked = kiraComponent.isMarked(displayTarget.getProfile().getId());

        super.renderWidget(context, mouseX, mouseY, delta);
        
        if (isMarked) {
            context.blitSprite(ShopEntry.Type.WEAPON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        } else {
            context.blitSprite(ShopEntry.Type.POISON.getTexture(), this.getX() - 7, this.getY() - 7, 30, 30);
        }
        
        PlayerFaceRenderer.draw(context, displayTarget.getSkin().texture(), this.getX(), this.getY(), 16);
        
        if (this.isHovered()) {
            this.drawShopSlotHighlight(context, this.getX(), this.getY(), 0);
            
            Component tooltip;
            if (isMarked) {
                tooltip = Component.translatable("hud.pathsrole.kira.marked_player", displayTarget.getProfile().getName())
                    .withStyle(ChatFormatting.RED);
            } else {
                tooltip = Component.nullToEmpty(displayTarget.getProfile().getName());
            }
            
            context.renderTooltip(Minecraft.getInstance().font, tooltip,
                    this.getX() - 4 - Minecraft.getInstance().font.width(tooltip) / 2,
                    this.getY() - 9);
        }

        renderDisplayText(context);
        
        if (isMarked) {
            context.pose().pushPose();
            context.pose().translate(0, 0, 200);
            context.blit(KIRA_MARK_OVERLAY, this.getX(), this.getY(), 0, 0, 20, 20, 20, 20);
            context.blit(KIRA_MARK_CORNER, this.getX() - 6, this.getY() + 10, 0, 0, 12, 12, 12, 12);
            context.pose().popPose();
        }
    }

    private void drawShopSlotHighlight(GuiGraphics context, int x, int y, int z) {
        int color = -1862287543;
        context.fillGradient(RenderType.guiOverlay(), x, y, x + 16, y + 14, color, color, 0);
        context.fillGradient(RenderType.guiOverlay(), x, y + 14, x + 15, y + 15, color, color, 0);
        context.fillGradient(RenderType.guiOverlay(), x, y + 15, x + 14, y + 16, color, color, 0);
    }

    public void setDisplayText(Component text) {
        this.displayText = text;
        this.cachedLines.clear();
    }

    private void renderDisplayText(GuiGraphics context) {
        if (displayText == null || displayText.getString().isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int maxWidth = 50;
        int lineHeight = font.lineHeight + 1;
        int yOffset = 4;

        if (cachedLines.isEmpty()) {
            cachedLines = font.split(displayText, maxWidth);
        }

        int startY = this.getY() + this.getHeight() + yOffset;

        for (int i = 0; i < cachedLines.size(); i++) {
            net.minecraft.util.FormattedCharSequence line = cachedLines.get(i);
            int lineWidth = font.width(line);
            int x = this.getX() + (this.getWidth() - lineWidth) / 2;
            int y = startY + (i * lineHeight);

            context.fill(x - 2, y - 1, x + lineWidth + 2, y + font.lineHeight + 1, 0x80000000);
            context.drawString(font, line, x, y, 0xFFFFFF, true);
        }
    }

    @Override
    public void renderString(GuiGraphics context, Font textRenderer, int color) {
    }
}