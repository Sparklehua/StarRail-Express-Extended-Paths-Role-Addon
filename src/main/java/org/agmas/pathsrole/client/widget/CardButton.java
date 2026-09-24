package org.agmas.pathsrole.client.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@Environment(value=EnvType.CLIENT)
public class CardButton extends Button {

    private static final int CARD_BG_COLOR = 0xC01E1B3D;
    private static final int CARD_HOVER_COLOR = 0xD02A2555;
    private static final int CARD_BORDER_COLOR = 0x604A4580;
    private static final int CORNER_RADIUS = 12;
    private static final int BORDER_THICKNESS = 2;
    private static final int ICON_SIZE = 48;

    private ResourceLocation iconTexture;
    private IconType iconType;

    public enum IconType {
        BOOK,
        MAGIC_CIRCLE,
        WINGED_SPIRIT,
        DOCUMENTS,
        MAGNIFIER,
        ENVELOPE,
        CARDS,
        NONE
    }

    public CardButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, null, IconType.NONE);
    }

    public CardButton(int x, int y, int width, int height, Component message, 
                      OnPress onPress, ResourceLocation iconTexture) {
        this(x, y, width, height, message, onPress, iconTexture, IconType.NONE);
    }

    public CardButton(int x, int y, int width, int height, Component message,
                      OnPress onPress, IconType iconType) {
        this(x, y, width, height, message, onPress, null, iconType);
    }

    private CardButton(int x, int y, int width, int height, Component message,
                       OnPress onPress, ResourceLocation iconTexture, IconType iconType) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        this.iconTexture = iconTexture;
        this.iconType = iconType;
    }

    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        boolean hovered = mouseX >= this.getX() && mouseX < this.getX() + this.getWidth()
            && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight();

        int bgColor = hovered ? CARD_HOVER_COLOR : CARD_BG_COLOR;
        this.drawRoundRect(guiGraphics, this.getX(), this.getY(), 
                           this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                           CORNER_RADIUS, bgColor);
        
        this.drawRoundBorder(guiGraphics, this.getX(), this.getY(),
                             this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                             CORNER_RADIUS, BORDER_THICKNESS, CARD_BORDER_COLOR);

        if (hovered) {
            this.drawGlowEffect(guiGraphics, this.getX(), this.getY(),
                                this.getX() + this.getWidth(), this.getY() + this.getHeight());
        }

        this.renderIcon(guiGraphics);

        if (this.iconTexture == null && this.iconType == IconType.NONE) {
            Minecraft mc = Minecraft.getInstance();
            guiGraphics.drawCenteredString(mc.font, this.getMessage(),
                                           this.getX() + this.getWidth() / 2,
                                           this.getY() + (this.getHeight() - 8) / 2,
                                           0xE8D8F0);
        }
    }

    private void renderIcon(GuiGraphics guiGraphics) {
        if (this.iconTexture != null) {
            int iconX = this.getX() + (this.getWidth() - ICON_SIZE) / 2;
            int iconY = this.getY() + (this.getHeight() - ICON_SIZE) / 2 - 8;
            guiGraphics.blit(this.iconTexture, iconX, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        } else if (this.iconType != IconType.NONE) {
            int centerX = this.getX() + this.getWidth() / 2;
            int centerY = this.getY() + this.getHeight() / 2 - 8;
            CardIconRenderer.renderIcon(guiGraphics, this.iconType, centerX, centerY, 40);
        }
    }

    private void drawRoundRect(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color) {
        int h = y2 - y1;
        if (r <= 0 || h <= 0) {
            g.fill(x1, y1, x2, y2, color);
            return;
        }
        r = Math.min(r, h / 2);
        for (int row = 0; row < h; row++) {
            int edge = Math.min(row, h - 1 - row);
            int inset = 0;
            if (edge < r) {
                int d = r - edge;
                inset = r - (int)Math.floor(Math.sqrt((double)r * r - (double)d * d));
            }
            g.fill(x1 + inset, y1 + row, x2 - inset, y1 + row + 1, color);
        }
    }

    private void drawRoundBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int thickness, int color) {
        int h = y2 - y1;
        if (h <= 0) return;
        r = Math.min(r, h / 2);
        for (int i = 0; i < thickness; i++) {
            for (int row = 0; row < h; row++) {
                int edge = Math.min(row, h - 1 - row);
                int inset = 0;
                if (edge < r) {
                    int d = r - edge;
                    inset = r - (int)Math.floor(Math.sqrt((double)r * r - (double)d * d));
                }
                int yy = y1 + row;
                boolean isTopOrBottomEdge = (row < r || row >= h - r);
                
                if (i == 0) {
                    if (inset == 0 || inset == 1) {
                        g.fill(x1 + inset, yy, x1 + inset + 1, yy + 1, color);
                        g.fill(x2 - inset - 1, yy, x2 - inset, yy + 1, color);
                    }
                    if (isTopOrBottomEdge) {
                        g.fill(x1 + inset, yy, x2 - inset, yy + 1, color);
                    }
                } else {
                    if (inset <= i) {
                        g.fill(x1 + i, yy, x1 + i + 1, yy + 1, color);
                        g.fill(x2 - i - 1, yy, x2 - i, yy + 1, color);
                    }
                    if (isTopOrBottomEdge && inset <= i) {
                        g.fill(x1 + i, yy, x2 - i, yy + 1, color);
                    }
                }
            }
        }
    }

    private void drawGlowEffect(GuiGraphics g, int x1, int y1, int x2, int y2) {
        int glowColor = 0x30FFFFFF;
        int glowRadius = 3;
        for (int i = 0; i < glowRadius; i++) {
            int alpha = 30 - i * 10;
            if (alpha > 0) {
                int color = (alpha << 24) | 0xFFFFFF;
                this.drawRoundRect(g, x1 - i, y1 - i, x2 + i, y2 + i, CORNER_RADIUS + i, color);
            }
        }
    }
}