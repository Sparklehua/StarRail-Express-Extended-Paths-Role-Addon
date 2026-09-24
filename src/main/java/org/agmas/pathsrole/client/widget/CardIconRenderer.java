package org.agmas.pathsrole.client.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import org.agmas.pathsrole.client.widget.CardButton.IconType;

@Environment(value=EnvType.CLIENT)
public class CardIconRenderer {

    private static final int ICON_COLOR = 0xE8E0F5FF;

    public static void renderIcon(GuiGraphics guiGraphics, IconType iconType, int centerX, int centerY, int size) {
        switch (iconType) {
            case BOOK:
                renderBookIcon(guiGraphics, centerX, centerY, size);
                break;
            case MAGIC_CIRCLE:
                renderMagicCircleIcon(guiGraphics, centerX, centerY, size);
                break;
            case WINGED_SPIRIT:
                renderWingedSpiritIcon(guiGraphics, centerX, centerY, size);
                break;
            case DOCUMENTS:
                renderDocumentsIcon(guiGraphics, centerX, centerY, size);
                break;
            case MAGNIFIER:
                renderMagnifierIcon(guiGraphics, centerX, centerY, size);
                break;
            case ENVELOPE:
                renderEnvelopeIcon(guiGraphics, centerX, centerY, size);
                break;
            case CARDS:
                renderCardsIcon(guiGraphics, centerX, centerY, size);
                break;
            default:
                break;
        }
    }

    private static void renderBookIcon(GuiGraphics g, int cx, int cy, int size) {
        int s = size / 2;
        
        int spineX = cx;
        int topY = cy - s + 4;
        int bottomY = cy + s - 6;
        int pageWidth = s - 2;
        int openAngle = 15;

        int leftTopX = cx - pageWidth;
        int leftBottomX = cx - 4;
        int rightTopX = cx + 4;
        int rightBottomX = cx + pageWidth;

        g.fill(spineX - 1, topY + 2, spineX + 1, bottomY - 1, ICON_COLOR);

        for (int y = topY; y < bottomY; y++) {
            int progress = (y - topY);
            int total = (bottomY - topY);
            int leftInset = progress * (pageWidth - 4) / total;
            int rightInset = progress * (pageWidth - 4) / total;

            int leftX1 = spineX - 3 - leftInset;
            int leftX2 = spineX - 1;
            int rightX1 = spineX + 1;
            int rightX2 = spineX + 3 + rightInset;

            g.fill(leftX1, y, leftX2, y + 1, ICON_COLOR);
            g.fill(rightX1, y, rightX2, y + 1, ICON_COLOR);
        }

        g.fill(leftTopX, topY, spineX - 2, topY + 2, ICON_COLOR);
        g.fill(spineX + 2, topY, rightTopX, topY + 2, ICON_COLOR);
        g.fill(cx - 5, bottomY - 2, spineX - 1, bottomY, ICON_COLOR);
        g.fill(spineX + 1, bottomY - 2, cx + 5, bottomY, ICON_COLOR);

        for (int i = 0; i < 5; i++) {
            int lineY = topY + 8 + i * 7;
            if (lineY < bottomY - 6) {
                int progress = (lineY - topY);
                int total = (bottomY - topY);
                int inset = progress * (pageWidth - 8) / total;
                
                g.fill(spineX - pageWidth + 4 + inset, lineY, spineX - 3 - inset / 2, lineY + 1, ICON_COLOR);
                g.fill(spineX + 3 + inset / 2, lineY, spineX + pageWidth - 4 - inset, lineY + 1, ICON_COLOR);
            }
        }

        g.fill(spineX - pageWidth / 2, topY + 4, spineX - pageWidth / 2 + 8, topY + 5, ICON_COLOR);
        g.fill(spineX + pageWidth / 2 - 8, topY + 4, spineX + pageWidth / 2, topY + 5, ICON_COLOR);

        int coverLeft = cx - pageWidth - 3;
        int coverRight = cx + pageWidth + 3;
        g.fill(coverLeft, bottomY, coverLeft + 3, bottomY + 3, ICON_COLOR);
        g.fill(coverRight - 3, bottomY, coverRight, bottomY + 3, ICON_COLOR);
        g.fill(coverLeft, bottomY + 2, coverRight, bottomY + 3, ICON_COLOR);
    }

    private static void renderMagicCircleIcon(GuiGraphics g, int cx, int cy, int size) {
        int r = size / 2 - 4;
        
        drawCircle(g, cx, cy, r, ICON_COLOR);
        drawCircle(g, cx, cy, r - 6, ICON_COLOR);
        drawCircle(g, cx, cy, r / 2, ICON_COLOR);

        for (int i = 0; i < 6; i++) {
            double angle = (i * Math.PI * 2) / 6;
            int x1 = cx + (int)(Math.cos(angle) * (r - 6));
            int y1 = cy + (int)(Math.sin(angle) * (r - 6));
            int x2 = cx + (int)(Math.cos(angle) * (r / 2));
            int y2 = cy + (int)(Math.sin(angle) * (r / 2));
            drawLine(g, x1, y1, x2, y2, ICON_COLOR);
        }

        for (int i = 0; i < 8; i++) {
            double angle = (i * Math.PI * 2) / 8 + Math.PI / 8;
            int x = cx + (int)(Math.cos(angle) * (r - 3));
            int y = cy + (int)(Math.sin(angle) * (r - 3));
            g.fill(x - 1, y - 1, x + 1, y + 1, ICON_COLOR);
        }

        g.fill(cx - 3, cy - 3, cx + 3, cy + 3, ICON_COLOR);
    }

    private static void renderWingedSpiritIcon(GuiGraphics g, int cx, int cy, int size) {
        int s = size / 2;

        g.fill(cx - 3, cy - s / 2, cx + 3, cy + s / 3, ICON_COLOR);
        g.fill(cx - 8, cy - s / 4, cx + 8, cy + s / 6, ICON_COLOR);

        g.fill(cx - 12, cy - s / 2, cx - 4, cy - s / 4, ICON_COLOR);
        g.fill(cx - 14, cy - s / 2 - 4, cx - 10, cy - s / 2, ICON_COLOR);
        g.fill(cx - 15, cy - s / 2 - 2, cx - 11, cy - s / 2, ICON_COLOR);
        g.fill(cx - 13, cy - s / 2 + 2, cx - 9, cy - s / 2 + 4, ICON_COLOR);

        g.fill(cx + 4, cy - s / 2, cx + 12, cy - s / 4, ICON_COLOR);
        g.fill(cx + 10, cy - s / 2 - 4, cx + 14, cy - s / 2, ICON_COLOR);
        g.fill(cx + 11, cy - s / 2 - 2, cx + 15, cy - s / 2, ICON_COLOR);
        g.fill(cx + 9, cy - s / 2 + 2, cx + 13, cy - s / 2 + 4, ICON_COLOR);

        g.fill(cx - 2, cy + s / 3, cx, cy + s / 2 - 2, ICON_COLOR);
        g.fill(cx, cy + s / 3, cx + 2, cy + s / 2, ICON_COLOR);

        g.fill(cx - 4, cy - 2, cx + 4, cy + 2, ICON_COLOR);
    }

    private static void renderDocumentsIcon(GuiGraphics g, int cx, int cy, int size) {
        int w = size / 2 - 4;
        int h = size / 2 + 4;
        int docW = w - 6;
        int docH = h - 4;

        int x1 = cx - w / 2;
        int y1 = cy - docH / 2 - 3;
        g.fill(x1, y1, x1 + docW, y1 + docH, ICON_COLOR);
        g.fill(x1 + docW - 1, y1 + 4, x1 + docW + 4, y1 + 9, ICON_COLOR);
        g.fill(x1 + docW, y1 + 8, x1 + docW + 3, y1 + 9, ICON_COLOR);
        for (int i = 0; i < 3; i++) {
            int ly = y1 + 8 + i * 6;
            if (ly < y1 + docH - 3) {
                g.fill(x1 + 3, ly, x1 + docW - 3, ly + 1, ICON_COLOR);
            }
        }

        int x2 = cx - w / 2 + 6;
        int y2 = cy - docH / 2 + 5;
        g.fill(x2, y2, x2 + docW, y2 + docH, ICON_COLOR);
        g.fill(x2 + docW - 1, y2 + 4, x2 + docW + 4, y2 + 9, ICON_COLOR);
        g.fill(x2 + docW, y2 + 8, x2 + docW + 3, y2 + 9, ICON_COLOR);
        for (int i = 0; i < 3; i++) {
            int ly = y2 + 8 + i * 6;
            if (ly < y2 + docH - 3) {
                g.fill(x2 + 3, ly, x2 + docW - 3, ly + 1, ICON_COLOR);
            }
        }

        g.fill(x2 + docW - 2, y2 + docH - 6, x2 + docW + 2, y2 + docH - 4, ICON_COLOR);
        g.fill(x2 + docW - 4, y2 + docH - 4, x2 + docW, y2 + docH - 2, ICON_COLOR);
    }

    private static void renderMagnifierIcon(GuiGraphics g, int cx, int cy, int size) {
        int r = size / 3;
        int handleLen = size / 4;
        double angle = Math.PI / 4;

        drawCircle(g, cx - 4, cy - 4, r, ICON_COLOR);
        drawCircle(g, cx - 4, cy - 4, r - 4, 0x40E0E0FF);

        int handleX1 = cx + (int)(Math.cos(angle) * r) - 4;
        int handleY1 = cy + (int)(Math.sin(angle) * r) - 4;
        int handleX2 = handleX1 + (int)(Math.cos(angle) * handleLen);
        int handleY2 = handleY1 + (int)(Math.sin(angle) * handleLen);

        drawThickLine(g, handleX1, handleY1, handleX2, handleY2, 3, ICON_COLOR);

        g.fill(handleX2 - 2, handleY2 - 4, handleX2 + 4, handleY2 + 2, ICON_COLOR);
    }

    private static void renderEnvelopeIcon(GuiGraphics g, int cx, int cy, int size) {
        int w = size / 2 + 6;
        int h = size / 2 + 2;
        int left = cx - w / 2;
        int right = cx + w / 2;
        int top = cy - h / 2 - 2;
        int bottom = cy + h / 2;

        g.fill(left, top, right, top + 2, ICON_COLOR);
        g.fill(left, bottom - 2, right, bottom, ICON_COLOR);
        g.fill(left, top, left + 2, bottom, ICON_COLOR);
        g.fill(right - 2, top, right, bottom, ICON_COLOR);

        drawThickLine(g, left, top + 3, cx, cy - 1, 2, ICON_COLOR);
        drawThickLine(g, cx, cy - 1, right - 3, top + 3, 2, ICON_COLOR);

        g.fill(cx - 4, cy + 4, cx + 5, cy + 6, ICON_COLOR);

        int stampX = right - 9;
        int stampY = top + 4;
        g.fill(stampX, stampY, stampX + 6, stampY + 7, ICON_COLOR);
        g.fill(stampX + 1, stampY + 1, stampX + 5, stampY + 6, 0x00FFFFFF);
    }

    private static void drawCircle(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y >= (radius - 1) * (radius - 1) && 
                    x * x + y * y <= radius * radius) {
                    g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
                }
            }
        }
    }

    private static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    private static void drawThickLine(GuiGraphics g, int x1, int y1, int x2, int y2, int thickness, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            for (int i = -thickness / 2; i <= thickness / 2; i++) {
                for (int j = -thickness / 2; j <= thickness / 2; j++) {
                    if (i * i + j * j <= (thickness / 2) * (thickness / 2)) {
                        g.fill(x1 + i, y1 + j, x1 + i + 1, y1 + j + 1, color);
                    }
                }
            }
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    private static void renderCardsIcon(GuiGraphics g, int cx, int cy, int size) {
        int cardW = size / 3;
        int cardH = size / 2 + 4;
        int offsetX = 6;
        int offsetY = 4;

        int left1 = cx - cardW / 2 - offsetX;
        int top1 = cy - cardH / 2 - offsetY;
        drawCardShape(g, left1, top1, cardW, cardH);

        int left2 = cx - cardW / 2 + offsetX / 2;
        int top2 = cy - cardH / 2 + offsetY / 2;
        drawCardShape(g, left2, top2, cardW, cardH);

        int left3 = cx - cardW / 2 + offsetX;
        int top3 = cy - cardH / 2 + offsetY;
        drawCardShape(g, left3, top3, cardW, cardH);

        int suitX = cx + offsetX;
        int suitY = cy - 2;
        drawHeart(g, suitX - 4, suitY - 4, 3, 0xFFFF5555);
        drawSpade(g, suitX + 4, suitY - 4, 3, 0xFF5555FF);
        drawDiamond(g, suitX - 4, suitY + 4, 3, 0xFFFF5555);
        drawClub(g, suitX + 4, suitY + 4, 3, 0xFF5555FF);
    }

    private static void drawCardShape(GuiGraphics g, int left, int top, int w, int h) {
        g.fill(left, top, left + w, top + h, 0xCCE8E0F5);
        g.fill(left, top, left + w, top + 1, ICON_COLOR);
        g.fill(left, top + h - 1, left + w, top + h, ICON_COLOR);
        g.fill(left, top, left + 1, top + h, ICON_COLOR);
        g.fill(left + w - 1, top, left + w, top + h, ICON_COLOR);
    }

    private static void drawHeart(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            for (int x = -r * 2; x <= r * 2; x++) {
                double dx = x / (double)(r * 2);
                double dy = y / (double)r;
                if (dx * dx + dy * dy <= 0.6 || (dx > 0 && dx * dx + dy * dy <= 1.2)) {
                    if (Math.abs(dx) < 0.8 && dy < 0.5 && dy > -0.7) {
                        g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
                    }
                }
            }
        }
    }

    private static void drawSpade(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r + 1; y++) {
            for (int x = -r; x <= r; x++) {
                double dx = x / (double)r;
                double dy = y / (double)(r + 1);
                if (dx * dx + dy * dy <= 0.7) {
                    g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
                }
            }
        }
        g.fill(cx - 1, cy + 1, cx + 1, cy + r + 1, color);
    }

    private static void drawDiamond(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            int halfW = r - Math.abs(y);
            g.fill(cx - halfW, cy + y, cx + halfW + 1, cy + y + 1, color);
        }
    }

    private static void drawClub(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                double dx = x / (double)r;
                double dy = y / (double)r;
                if (dx * dx + dy * dy <= 0.5) {
                    g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
                }
            }
        }
        g.fill(cx - 1, cy + 1, cx + 1, cy + r + 1, color);
    }
}