package org.agmas.pathsrole.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.agmas.pathsrole.Paths;

@Environment(value=EnvType.CLIENT)
public class PathStoryCollectionScreen extends Screen {
    private static final int VOID = 0xFF080B14;
    private static final int INK = 0xFF0F1428;
    private static final int PANEL = 0xFF0F1828;
    private static final int PANEL_SOFT = 0xFF182440;
    private static final int BRONZE = 0xFF8B6914;
    private static final int GOLD_DARK = 0xFF6B5A30;
    private static final int GOLD = 0xFFD4AF37;
    private static final int GOLD_BRIGHT = 0xFFFFE1A0;
    private static final int IVORY = 0xFFF7EBCF;
    private static final int TEXT = 0xFFFFF4DC;
    private static final int TEXT_MUTED = 0xFF9E8B6E;

    private static final int CARD_MIN_W = 70;
    private static final int CARD_MAX_W = 90;
    private static final int CARD_MIN_H = 110;
    private static final int CARD_MAX_H = 176;
    private static final int CARD_SPACING = 95;
    private static final int VISIBLE_RANGE = 2;

    private final Paths[] allPaths = Paths.values();
    private final int totalCards;
    
    private double carouselPosition = 0.0;
    private int focusedIndex = 0;
    private float[] cardEmphasis;
    private long openedAtMillis;
    private long lastFrameMillis;
    
    private boolean isDragging = false;
    private double dragStartX = 0.0;
    private double dragStartPosition = 0.0;
    
    private int previousArrowX, previousArrowY, previousArrowW, previousArrowH;
    private int nextArrowX, nextArrowY, nextArrowW, nextArrowH;

    public PathStoryCollectionScreen() {
        super(Component.translatable("screen.pathsrole.path_story_collection"));
        this.totalCards = this.allPaths.length;
        this.cardEmphasis = new float[this.totalCards];
        this.openedAtMillis = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        this.lastFrameMillis = System.currentTimeMillis();
        
        if (this.focusedIndex < 0 || this.focusedIndex >= this.totalCards) {
            this.focusedIndex = 0;
        }
        this.carouselPosition = this.focusedIndex;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float frameSeconds = (now - lastFrameMillis) / 1000.0f;
        float elapsedSeconds = (now - this.openedAtMillis) / 1000.0f;
        this.lastFrameMillis = now;

        updateCarousel(frameSeconds);
        
        renderBackdrop(g, elapsedSeconds);
        renderHeader(g, elapsedSeconds);
        renderCarousel(g, mouseX, mouseY, frameSeconds, elapsedSeconds);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    private void updateCarousel(float frameSeconds) {
        double targetPosition = this.focusedIndex;
        double diff = targetPosition - this.carouselPosition;
        
        if (Math.abs(diff) > this.totalCards / 2.0) {
            if (diff > 0) {
                diff -= this.totalCards;
            } else {
                diff += this.totalCards;
            }
        }
        
        this.carouselPosition += diff * approachFactor(frameSeconds, 11.0f);
        
        if (this.carouselPosition < 0) {
            this.carouselPosition += this.totalCards;
        } else if (this.carouselPosition >= this.totalCards) {
            this.carouselPosition -= this.totalCards;
        }
    }

    private void renderBackdrop(GuiGraphics g, float elapsedSeconds) {
        g.fill(0, 0, this.width, this.height, 0xFF080B14);
        g.fillGradient(0, 0, this.width, this.height, 0xFF080B14, 0xFF0F1428);
    }

    private void renderHeader(GuiGraphics g, float elapsedSeconds) {
        String title = Component.translatable("screen.pathsrole.path_story_collection").getString();
        g.drawString(this.font, title, this.width / 2 - this.font.width(title) / 2, 12, IVORY, false);

        float reveal = easeOutCubic(Mth.clamp(elapsedSeconds / 0.65f, 0.0f, 1.0f));
        int revealHalf = Math.round(this.width * 0.5f * reveal);
        g.hLine(this.width / 2 - revealHalf, this.width / 2 + revealHalf, 32, withAlpha(GOLD, 100));
    }

    private void renderCarousel(GuiGraphics g, int mouseX, int mouseY, 
                                 float frameSeconds, float elapsedSeconds) {
        if (this.totalCards == 0) return;

        int centerX = this.width / 2;
        int centerY = this.height / 2 + 20;
        
        int cardHeight = CARD_MAX_H;
        int cardWidth = CARD_MAX_W;
        
        List<CardRenderInfo> cardsToRender = new ArrayList<>();

        for (int offset = -VISIBLE_RANGE; offset <= VISIBLE_RANGE; offset++) {
            int actualOffset = offset;
            
            int index = getWrappedIndex((int)this.carouselPosition + offset);
            if (index < 0 || index >= this.totalCards) continue;
            
            float distance = ((int)this.carouselPosition + offset) - (float)this.carouselPosition;
            
            if (distance > this.totalCards / 2.0f) {
                distance -= this.totalCards;
            } else if (distance < -this.totalCards / 2.0f) {
                distance += this.totalCards;
            }
            
            float absDistance = Math.abs(distance);
            if (absDistance > VISIBLE_RANGE + 1.0f) continue;
            
            float scale = Mth.lerp(1.0f - Mth.clamp(absDistance / (VISIBLE_RANGE + 1.0f), 0.0f, 1.0f), 
                                   CARD_MIN_W, CARD_MAX_W);
            float heightScale = scale / CARD_MAX_W;
            int currentCardWidth = Math.round(scale);
            int currentCardHeight = Math.round(CARD_MAX_H * heightScale);
            
            int baseX = centerX + Math.round(distance * CARD_SPACING) - currentCardWidth / 2;
            int baseY = centerY - currentCardHeight / 2;
            
            boolean isFocused = (index == this.focusedIndex);
            boolean isHovered = (mouseX >= baseX && mouseX <= baseX + currentCardWidth && 
                               mouseY >= baseY && mouseY <= baseY + currentCardHeight);
            
            float targetEmphasis = isFocused ? 1.0f : (isHovered ? 0.7f : 0.0f);
            cardEmphasis[index] += (targetEmphasis - cardEmphasis[index]) * Mth.clamp(frameSeconds * 10.0f, 0.0f, 1.0f);
            
            float visibility = Mth.clamp(1.0f - Math.max(0.0f, absDistance - VISIBLE_RANGE) * 0.5f, 0.3f, 1.0f);
            
            cardsToRender.add(new CardRenderInfo(index, baseX, baseY, currentCardWidth, currentCardHeight, 
                                                 absDistance, cardEmphasis[index], visibility, isFocused, isHovered));
        }

        cardsToRender.sort((a, b) -> Float.compare(b.distance, a.distance));

        for (CardRenderInfo info : cardsToRender) {
            renderSingleCard(g, info.x, info.y, info.width, info.height,
                           allPaths[info.index], info.index, info.emphasis, 
                           info.isFocused, info.isHovered, info.visibility, elapsedSeconds);
        }

        renderArrows(g, centerX, centerY, cardHeight, mouseX, mouseY);
    }

    private int getWrappedIndex(int index) {
        if (index < 0) {
            index += this.totalCards * ((-index / this.totalCards) + 1);
        }
        return index % this.totalCards;
    }

    private void renderArrows(GuiGraphics g, int centerX, int centerY, int cardHeight, int mouseX, int mouseY) {
        int arrowY = centerY;
        int arrowSize = 24;
        int edgeMargin = 15;
        
        this.previousArrowX = edgeMargin;
        this.previousArrowY = arrowY - arrowSize / 2;
        this.previousArrowW = arrowSize;
        this.previousArrowH = arrowSize;
        
        this.nextArrowX = this.width - arrowSize - edgeMargin;
        this.nextArrowY = arrowY - arrowSize / 2;
        this.nextArrowW = arrowSize;
        this.nextArrowH = arrowSize;
        
        boolean prevHovered = mouseX >= this.previousArrowX && mouseX <= this.previousArrowX + this.previousArrowW &&
                             mouseY >= this.previousArrowY && mouseY <= this.previousArrowY + this.previousArrowH;
        boolean nextHovered = mouseX >= this.nextArrowX && mouseX <= this.nextArrowX + this.nextArrowW &&
                             mouseY >= this.nextArrowY && mouseY <= this.nextArrowY + this.nextArrowH;
        
        int prevColor = prevHovered ? GOLD_BRIGHT : GOLD;
        int nextColor = nextHovered ? GOLD_BRIGHT : GOLD;
        
        renderArrow(g, this.previousArrowX, this.previousArrowY, this.previousArrowW, this.previousArrowH, "<", prevColor);
        renderArrow(g, this.nextArrowX, this.nextArrowY, this.nextArrowW, this.nextArrowH, ">", nextColor);
    }

    private void renderArrow(GuiGraphics g, int x, int y, int w, int h, String symbol, int color) {
        fillChamfered(g, x - 1, y - 1, w + 2, h + 2, 4, withAlpha(0xFF000000, 150));
        fillChamfered(g, x, y, w, h, 4, withAlpha(color, 200));
        
        g.drawString(this.font, symbol, x + w / 2 - this.font.width(symbol) / 2, y + h / 2 - 4, withAlpha(IVORY, 255), false);
    }

    private void renderSingleCard(GuiGraphics g, int x, int y, int w, int h,
                                   Paths path, int index, float emphasis, 
                                   boolean focused, boolean hovered, float visibility, float elapsedSeconds) {
        int alpha = Mth.clamp(Math.round(255.0f * visibility), 50, 255);
        
        int extraScale = Math.round(emphasis * 3);
        int drawX = x - extraScale;
        int drawY = y - extraScale - Math.round(emphasis * 2);
        int drawW = w + extraScale * 2;
        int drawH = h + extraScale * 2;

        int border = focused ? GOLD_BRIGHT : (hovered ? GOLD : BRONZE);
        int cardTop = focused ? 0xFF4C3825 : 0xFF31251F;

        fillChamfered(g, drawX - 2, drawY + 3, drawW + 4, drawH + 4, 6, withAlpha(0xFF000000, alpha * 70 / 255));
        fillChamfered(g, drawX, drawY, drawW, drawH, 6, withAlpha(border, alpha));
        fillChamfered(g, drawX + 1, drawY + 1, drawW - 2, drawH - 2, 5, withAlpha(PANEL_SOFT, alpha));
        g.fillGradient(drawX + 3, drawY + 4, drawX + drawW - 3, drawY + drawH - 4,
                withAlpha(cardTop, alpha), withAlpha(INK, alpha));

        g.hLine(drawX + 6, drawX + drawW - 7, drawY + 4, withAlpha(border, alpha * 120 / 255));
        g.hLine(drawX + 6, drawX + drawW - 7, drawY + drawH - 5, withAlpha(border, alpha * 80 / 255));

        float scanPhase = (elapsedSeconds * 0.38f + index * 0.17f) % 1.0f;
        int scanY = drawY + 6 + Math.round(scanPhase * Math.max(1, drawH - 12));
        g.fill(drawX + 3, scanY, drawX + drawW - 3, scanY + 1,
                withAlpha(GOLD_BRIGHT, Math.round(emphasis * 60.0f * visibility)));

        int iconRadius = Mth.clamp(w / 6, 10, 16);
        int iconCenterX = drawX + drawW / 2;
        int iconCenterY = drawY + Math.max(28, drawH / 3);

        drawDiamond(g, iconCenterX, iconCenterY, iconRadius + 3, withAlpha(GOLD_DARK, alpha * 140 / 255));
        drawDiamond(g, iconCenterX, iconCenterY, iconRadius, withAlpha(focused ? GOLD_BRIGHT : GOLD, alpha));
        drawDiamond(g, iconCenterX, iconCenterY, Math.max(4, iconRadius - 4), withAlpha(INK, alpha));

        renderPathEmblem(g, iconCenterX, iconCenterY, iconRadius, index, elapsedSeconds, alpha, emphasis);

        String chineseName = path.getChineseName();
        String englishName = path.getEnglishName();

        int nameY = iconCenterY + iconRadius + 8;
        g.drawString(this.font, chineseName, drawX + drawW / 2 - this.font.width(chineseName) / 2, nameY, withAlpha(TEXT, alpha), false);

        int englishWidth = this.font.width(englishName);
        if (englishWidth > drawW - 10) {
            String truncated = this.font.plainSubstrByWidth(englishName, drawW - 10);
            g.drawString(this.font, truncated, drawX + drawW / 2 - this.font.width(truncated) / 2, nameY + 11, withAlpha(TEXT_MUTED, alpha), false);
        } else {
            g.drawString(this.font, englishName, drawX + drawW / 2 - englishWidth / 2, nameY + 11, withAlpha(TEXT_MUTED, alpha), false);
        }

        if (focused) {
            drawDiamond(g, drawX + drawW / 2, drawY + drawH + 4, 3, withAlpha(GOLD_BRIGHT, alpha));
        }
    }

    private void renderPathEmblem(GuiGraphics g, int centerX, int centerY, int radius,
                                   int index, float elapsedSeconds, int alpha, float emphasis) {
        int arm = Math.max(4, radius - 3);
        int railAlpha = Math.round(alpha * (0.44f + emphasis * 0.24f));
        g.fill(centerX - arm, centerY, centerX + arm + 1, centerY + 1, withAlpha(GOLD, railAlpha));
        g.fill(centerX, centerY - arm, centerX + 1, centerY + arm + 1, withAlpha(GOLD, railAlpha));
        drawDiamond(g, centerX, centerY, 2, withAlpha(IVORY, alpha));

        float angle = elapsedSeconds * 1.35f + index * 0.83f;
        int orbit = Math.max(5, radius - 2);
        int lightX = centerX + Math.round(Mth.cos(angle) * orbit);
        int lightY = centerY + Math.round(Mth.sin(angle) * orbit);
        drawDiamond(g, lightX, lightY, emphasis > 0.55f ? 2 : 1, withAlpha(GOLD_BRIGHT, alpha));

        float opposite = angle + (float) Math.PI;
        int echoX = centerX + Math.round(Mth.cos(opposite) * orbit);
        int echoY = centerY + (int)(Math.sin(opposite) * orbit);
        g.fill(echoX, echoY, echoX + 1, echoY + 1, withAlpha(GOLD, Math.round(alpha * 0.55f)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isClickInBounds(mouseX, mouseY, this.previousArrowX, this.previousArrowY, this.previousArrowW, this.previousArrowH)) {
                navigatePrevious();
                return true;
            }
            
            if (isClickInBounds(mouseX, mouseY, this.nextArrowX, this.nextArrowY, this.nextArrowW, this.nextArrowH)) {
                navigateNext();
                return true;
            }
            
            int clickedIndex = findClickedCard(mouseX, mouseY);
            if (clickedIndex >= 0) {
                if (clickedIndex == this.focusedIndex) {
                    onPathClicked(allPaths[clickedIndex], clickedIndex);
                } else {
                    this.focusedIndex = clickedIndex;
                }
                
                this.isDragging = true;
                this.dragStartX = mouseX;
                this.dragStartPosition = this.carouselPosition;
                return true;
            }
            
            this.isDragging = true;
            this.dragStartX = mouseX;
            this.dragStartPosition = this.carouselPosition;
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isDragging && button == 0) {
            double dragDelta = mouseX - this.dragStartX;
            double positionDelta = -dragDelta / CARD_SPACING;
            
            this.carouselPosition = this.dragStartPosition + positionDelta;
            
            while (this.carouselPosition < 0) {
                this.carouselPosition += this.totalCards;
            }
            while (this.carouselPosition >= this.totalCards) {
                this.carouselPosition -= this.totalCards;
            }
            
            int nearestIndex = Math.round((float)this.carouselPosition);
            this.focusedIndex = getWrappedIndex(nearestIndex);
            
            return true;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isDragging) {
            this.isDragging = false;
            
            int nearestIndex = Math.round((float)this.carouselPosition);
            this.focusedIndex = getWrappedIndex(nearestIndex);
            this.carouselPosition = this.focusedIndex;
            return true;
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int findClickedCard(double mouseX, double mouseY) {
        int centerX = this.width / 2;
        int centerY = this.height / 2 + 20;
        
        for (int offset = -VISIBLE_RANGE; offset <= VISIBLE_RANGE; offset++) {
            int index = getWrappedIndex((int)this.carouselPosition + offset);
            if (index < 0 || index >= this.totalCards) continue;
            
            float distance = ((int)this.carouselPosition + offset) - (float)this.carouselPosition;
            if (distance > this.totalCards / 2.0f) distance -= this.totalCards;
            if (distance < -this.totalCards / 2.0f) distance += this.totalCards;
            
            float absDistance = Math.abs(distance);
            if (absDistance > VISIBLE_RANGE + 1.0f) continue;
            
            float scale = Mth.lerp(1.0f - Mth.clamp(absDistance / (VISIBLE_RANGE + 1.0f), 0.0f, 1.0f), 
                                   CARD_MIN_W, CARD_MAX_W);
            float heightScale = scale / CARD_MAX_W;
            int currentCardWidth = Math.round(scale);
            int currentCardHeight = Math.round(CARD_MAX_H * heightScale);
            
            int cardX = centerX + Math.round(distance * CARD_SPACING) - currentCardWidth / 2;
            int cardY = centerY - currentCardHeight / 2;
            
            if (mouseX >= cardX && mouseX <= cardX + currentCardWidth &&
                mouseY >= cardY && mouseY <= cardY + currentCardHeight) {
                return index;
            }
        }
        
        return -1;
    }

    private boolean isClickInBounds(double x, double y, int boundsX, int boundsY, int boundsW, int boundsH) {
        return x >= boundsX && x <= boundsX + boundsW && y >= boundsY && y <= boundsY + boundsH;
    }

    private void navigatePrevious() {
        this.focusedIndex = getWrappedIndex(this.focusedIndex - 1);
        this.carouselPosition = this.focusedIndex;
    }

    private void navigateNext() {
        this.focusedIndex = getWrappedIndex(this.focusedIndex + 1);
        this.carouselPosition = this.focusedIndex;
    }

    private void onPathClicked(Paths path, int index) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new PathStoryDetailScreen(path));
        }
    }

    private void fillChamfered(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (r > 0) {
            for (int dy = 0; dy < r && dy < h; dy++) {
                int dx = (int)Math.sqrt(r * r - (r - dy) * (r - dy));
                if (dx > 0) {
                    g.fill(x + r - dx, y + dy, x + r + dx, y + dy + 1, color);
                    g.fill(x + r - dx, y + h - dy - 1, x + r + dx, y + h - dy, color);
                }
            }
            for (int dx = 0; dx < r && dx < w; dx++) {
                int dy = (int)Math.sqrt(r * r - (r - dx) * (r - dx));
                if (dy > 0) {
                    g.fill(x + dx, y + r - dy, x + dx + 1, y + r + dy, color);
                    g.fill(x + w - dx - 1, y + r - dy, x + w - dx, y + r + dy, color);
                }
            }
        }
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);
    }

    private void drawDiamond(GuiGraphics g, int cx, int cy, int halfSize, int color) {
        if (halfSize <= 0) return;
        for (int i = 0; i < halfSize; i++) {
            float scale = (float)(halfSize - i) / halfSize;
            int w = Math.max(1, Math.round(scale * halfSize));
            g.fill(cx - w, cy - i, cx + w, cy - i + 1, color);
            if (i > 0) {
                g.fill(cx - w, cy + i, cx + w, cy + i + 1, color);
            }
        }
        g.fill(cx - halfSize, cy, cx + halfSize, cy + 1, color);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static float easeOutCubic(float t) {
        float t1 = t - 1.0f;
        return t1 * t1 * t1 + 1.0f;
    }

    private static float approachFactor(float frameSeconds, float speed) {
        return 1.0f - (float)Math.pow(0.001, frameSeconds * speed);
    }

    private static class CardRenderInfo {
        final int index;
        final int x, y, width, height;
        final float distance, emphasis, visibility;
        final boolean isFocused, isHovered;

        CardRenderInfo(int index, int x, int y, int width, int height, 
                      float distance, float emphasis, float visibility, 
                      boolean isFocused, boolean isHovered) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.distance = distance;
            this.emphasis = emphasis;
            this.visibility = visibility;
            this.isFocused = isFocused;
            this.isHovered = isHovered;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}