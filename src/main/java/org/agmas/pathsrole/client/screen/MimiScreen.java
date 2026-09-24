package org.agmas.pathsrole.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class MimiScreen extends Screen {

    private static final int IVORY = 0xFFF5F0E8;
    private static final int GOLD = 0xFFC8A84E;
    private static final int GOLD_BRIGHT = 0xFFE8C56D;
    private static final int CARD_BG = 0xCC0D0D1A;
    private static final int CARD_BORDER = 0x99C8A84E;

    private static final int CARD_WIDTH = 160;
    private static final int CARD_HEIGHT = 180;
    private static final int CARD_MARGIN = 30;

    private final Random random = new Random();
    private final List<Star> stars = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private float animTime;
    private int tickCounter;

    private CornerCard topLeftCard;
    private CornerCard topRightCard;
    private CornerCard bottomLeftCard;
    private CornerCard bottomRightCard;

    private int hoveredCard = -1;

    public MimiScreen() {
        super(Component.translatable("screen.pathsrole.mimi.title"));
    }

    @Override
    protected void init() {
        super.init();
        stars.clear();
        particles.clear();
        animTime = 0f;
        tickCounter = 0;

        for (int i = 0; i < 150; i++) {
            stars.add(new Star(
                random.nextFloat() * this.width,
                random.nextFloat() * this.height,
                0.3f + random.nextFloat() * 0.7f,
                1f + random.nextFloat() * 2f
            ));
        }
        for (int i = 0; i < 40; i++) {
            particles.add(createParticle(true));
        }

        int tlX = CARD_MARGIN;
        int tlY = CARD_MARGIN + 10;
        int trX = this.width - CARD_MARGIN - CARD_WIDTH;
        int trY = CARD_MARGIN + 10;
        int blX = CARD_MARGIN;
        int blY = this.height - CARD_MARGIN - CARD_HEIGHT;
        int brX = this.width - CARD_MARGIN - CARD_WIDTH;
        int brY = this.height - CARD_MARGIN - CARD_HEIGHT;

        topLeftCard = new CornerCard(tlX, tlY, CARD_WIDTH, CARD_HEIGHT,
            Component.translatable("screen.pathsrole.mimi.card1"),
            Component.translatable("screen.pathsrole.mimi.card1_desc"),
            0xFF4A90D9
        );
        topRightCard = new CornerCard(trX, trY, CARD_WIDTH, CARD_HEIGHT,
            Component.translatable("screen.pathsrole.mimi.card2"),
            Component.translatable("screen.pathsrole.mimi.card2_desc"),
            0xFFD94A8A
        );
        bottomLeftCard = new CornerCard(blX, blY, CARD_WIDTH, CARD_HEIGHT,
            Component.translatable("screen.pathsrole.mimi.card3"),
            Component.translatable("screen.pathsrole.mimi.card3_desc"),
            0xFF4AD9A0
        );
        bottomRightCard = new CornerCard(brX, brY, CARD_WIDTH, CARD_HEIGHT,
            Component.translatable("screen.pathsrole.mimi.card4"),
            Component.translatable("screen.pathsrole.mimi.card4_desc"),
            0xFFD9A04A
        );

        this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> this.onClose())
            .bounds(this.width - 25, 5, 20, 20)
            .build());
    }

    @Override
    public void tick() {
        animTime += 0.016f;
        tickCounter++;

        for (Particle p : particles) {
            p.x += p.speedX;
            p.y += p.speedY;
            p.rotation += p.rotationSpeed;
            if (p.y > this.height + 20 || p.x < -20 || p.x > this.width + 20) {
                Particle newP = createParticle(false);
                p.x = newP.x;
                p.y = newP.y;
                p.size = newP.size;
                p.speedX = newP.speedX;
                p.speedY = newP.speedY;
                p.rotation = newP.rotation;
                p.rotationSpeed = newP.rotationSpeed;
                p.colorType = newP.colorType;
            }
        }
        for (Star s : stars) {
            s.twinkle += s.twinkleSpeed;
            if (s.twinkle > 1f || s.twinkle < 0.3f) {
                s.twinkleSpeed = -s.twinkleSpeed;
            }
        }
        super.tick();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackgroundLayer(g);
        renderStarfield(g);
        renderParticles(g);
        renderGlowingRings(g);
        renderCenterOrnament(g);

        hoveredCard = -1;
        if (topLeftCard.contains(mouseX, mouseY)) hoveredCard = 0;
        if (topRightCard.contains(mouseX, mouseY)) hoveredCard = 1;
        if (bottomLeftCard.contains(mouseX, mouseY)) hoveredCard = 2;
        if (bottomRightCard.contains(mouseX, mouseY)) hoveredCard = 3;

        renderCornerCard(g, topLeftCard, 0);
        renderCornerCard(g, topRightCard, 1);
        renderCornerCard(g, bottomLeftCard, 2);
        renderCornerCard(g, bottomRightCard, 3);

        renderTitle(g);
        renderCrosshair(g);

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderBackgroundLayer(GuiGraphics g) {
        Matrix4f matrix = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int colorTopLeft = withAlpha(0x0F0826, 255);
        int colorTopRight = withAlpha(0x190C37, 255);
        int colorBottomRight = withAlpha(0x23124B, 255);
        int colorBottomLeft = withAlpha(0x140A2D, 255);

        buffer.addVertex(matrix, 0, this.height, 0).setColor(colorTopLeft);
        buffer.addVertex(matrix, this.width, this.height, 0).setColor(colorTopRight);
        buffer.addVertex(matrix, this.width, 0, 0).setColor(colorBottomRight);
        buffer.addVertex(matrix, 0, 0, 0).setColor(colorBottomLeft);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void renderStarfield(GuiGraphics g) {
        for (Star star : stars) {
            float alpha = star.brightness * star.twinkle;
            int color = FastColor.ARGB32.color((int)(alpha * 255), 255, 255, (int)(alpha * 220));
            g.fill((int)star.x, (int)star.y, (int)star.x + 2, (int)star.y + 2, color);
        }
    }

    private void renderParticles(GuiGraphics g) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Particle p : particles) {
            g.pose().pushPose();
            g.pose().translate(p.x, p.y, 0);
            g.pose().mulPose(new Quaternionf().rotateZ((float)Math.toRadians(p.rotation)));

            int alpha = 180 + (int)(random.nextFloat() * 75);
            int color;
            switch (p.colorType) {
                case 0: color = FastColor.ARGB32.color(alpha, 200, 255, 255); break;
                case 1: color = FastColor.ARGB32.color(alpha, 255, 180, 220); break;
                default: color = FastColor.ARGB32.color(alpha, 255, 255, 255); break;
            }

            float halfSize = p.size / 2f;
            Matrix4f matrix = g.pose().last().pose();
            buffer.addVertex(matrix, -halfSize, -halfSize, 0).setColor(color);
            buffer.addVertex(matrix, -halfSize, halfSize, 0).setColor(color);
            buffer.addVertex(matrix, halfSize, halfSize, 0).setColor(color);
            buffer.addVertex(matrix, halfSize, -halfSize, 0).setColor(color);

            g.pose().popPose();
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void renderGlowingRings(GuiGraphics g) {
        float centerX = this.width / 2f;
        float centerY = this.height / 2f;
        float time = animTime;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (int ring = 0; ring < 3; ring++) {
            float radius = 100f + ring * 80f;
            int segments = 60;
            float alpha = 0.12f - ring * 0.03f;
            int baseColor = withAlpha(0xB4A0FF, (int)(alpha * 255));

            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f matrix = g.pose().last().pose();

            for (int i = 0; i <= segments; i++) {
                float angle = (float)i / segments * (float)(Math.PI * 2) + time * (0.5f + ring * 0.3f);
                float wobble = (float)Math.sin(angle * 3 + time * 2) * 10f;
                float r = radius + wobble;
                float x = centerX + (float)Math.cos(angle) * r;
                float y = centerY + (float)Math.sin(angle) * r;
                buffer.addVertex(matrix, x, y, 0).setColor(baseColor);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
        RenderSystem.disableBlend();
    }

    private void renderCenterOrnament(GuiGraphics g) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        float pulse = 1.0f + Mth.sin(tickCounter * 0.03f) * 0.08f;

        int ringRadius = 55;
        for (int i = 0; i < 360; i += 2) {
            float angle = (float)Math.toRadians(i + tickCounter * 0.3f);
            float alpha = 0.1f + Mth.sin((i + tickCounter) * 0.04f) * 0.05f;
            int color = (int)(alpha * 255) << 24 | 0xC8A84E;
            int x = centerX + (int)(Math.cos(angle) * ringRadius * pulse);
            int y = centerY + (int)(Math.sin(angle) * ringRadius * pulse);
            g.fill(x, y, x + 2, y + 2, color);
        }

        int innerRadius = 30;
        for (int i = 0; i < 360; i += 4) {
            float angle = (float)Math.toRadians(i - tickCounter * 0.5f);
            float alpha = 0.15f;
            int color = (int)(alpha * 255) << 24 | 0xB4A0FF;
            int x = centerX + (int)(Math.cos(angle) * innerRadius);
            int y = centerY + (int)(Math.sin(angle) * innerRadius);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void renderCornerCard(GuiGraphics g, CornerCard card, int index) {
        boolean hovered = hoveredCard == index;
        int x = card.x;
        int y = card.y;
        int w = card.width;
        int h = card.height;

        int bgColor = hovered ? 0xDD15152E : CARD_BG;
        g.fill(x, y, x + w, y + h, bgColor);

        int borderAlpha = hovered ? 0xFF : 0x99;
        int borderColor = (borderAlpha << 24) | (card.accentColor & 0x00FFFFFF);
        g.fill(x, y, x + w, y + 1, borderColor);
        g.fill(x, y + h - 1, x + w, y + h, borderColor);
        g.fill(x, y, x + 1, y + h, borderColor);
        g.fill(x + w - 1, y, x + w, y + h, borderColor);

        int cornerLen = 12;
        int cornerAlpha = hovered ? 0xFF : 0xCC;
        int cornerColor = (cornerAlpha << 24) | (card.accentColor & 0x00FFFFFF);

        g.fill(x, y, x + cornerLen, y + 2, cornerColor);
        g.fill(x, y, x + 2, y + cornerLen, cornerColor);
        g.fill(x + w - cornerLen, y, x + w, y + 2, cornerColor);
        g.fill(x + w - 2, y, x + w, y + cornerLen, cornerColor);
        g.fill(x, y + h - 2, x + cornerLen, y + h, cornerColor);
        g.fill(x, y + h - cornerLen, x + 2, y + h, cornerColor);
        g.fill(x + w - cornerLen, y + h - 2, x + w, y + h, cornerColor);
        g.fill(x + w - 2, y + h - cornerLen, x + w, y + h, cornerColor);

        int iconSize = 48;
        int iconCenterX = x + w / 2;
        int iconCenterY = y + 20 + iconSize / 2;

        RenderSystem.enableBlend();
        float alpha = hovered ? 1.0f : 0.7f;
        int red = (card.accentColor >> 16) & 0xFF;
        int green = (card.accentColor >> 8) & 0xFF;
        int blue = card.accentColor & 0xFF;

        drawDiamond(g, iconCenterX, iconCenterY, iconSize / 2, red, green, blue, alpha);

        int innerAlpha = hovered ? 200 : 140;
        drawDiamond(g, iconCenterX, iconCenterY, iconSize / 3, red, green, blue, innerAlpha / 255f);

        int titleWidth = this.font.width(card.title);
        g.drawString(this.font, card.title, x + (w - titleWidth) / 2, iconCenterY + iconSize / 2 + 12, GOLD_BRIGHT, true);

        int descWidth = this.font.width(card.description);
        g.drawString(this.font, card.description, x + (w - descWidth) / 2, iconCenterY + iconSize / 2 + 28, IVORY, true);

        if (hovered) {
            float glowAlpha = 0.3f + Mth.sin(tickCounter * 0.08f) * 0.1f;
            int glowColor = (int)(glowAlpha * 255) << 24 | (card.accentColor & 0x00FFFFFF);
            for (int i = 0; i < 3; i++) {
                g.fill(x - 1 - i, y - 1 - i, x + w + 1 + i, y - i, glowColor);
                g.fill(x - 1 - i, y + h + i, x + w + 1 + i, y + h + 1 + i, glowColor);
                g.fill(x - 1 - i, y - 1 - i, x - i, y + h + 1 + i, glowColor);
                g.fill(x + w + i, y - 1 - i, x + w + 1 + i, y + h + 1 + i, glowColor);
            }
        }
    }

    private void renderTitle(GuiGraphics g) {
        Component title = Component.translatable("screen.pathsrole.mimi.title");
        int titleWidth = this.font.width(title);
        int titleX = (this.width - titleWidth) / 2;
        int titleY = 25;

        float pulse = 1.0f + Mth.sin(tickCounter * 0.04f) * 0.05f;
        g.pose().pushPose();
        g.pose().translate(titleX + titleWidth / 2f, titleY + 6, 0);
        g.pose().scale(pulse, pulse, 1.0f);
        g.drawString(this.font, title, -titleWidth / 2, -6, GOLD_BRIGHT, true);
        g.pose().popPose();

        g.fill(titleX - 20, titleY + 20, titleX + titleWidth + 20, titleY + 21, 0x66C8A84E);
    }

    private void renderCrosshair(GuiGraphics g) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int gap = 65;
        int len = 15;
        int color = 0x44C8A84E;

        g.fill(cx - gap - len, cy - 1, cx - gap, cy + 1, color);
        g.fill(cx + gap, cy - 1, cx + gap + len, cy + 1, color);
        g.fill(cx - 1, cy - gap - len, cx + 1, cy - gap, color);
        g.fill(cx - 1, cy + gap, cx + 1, cy + gap + len, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (topLeftCard.contains((int)mouseX, (int)mouseY)) {
                onCardClicked(0);
                return true;
            }
            if (topRightCard.contains((int)mouseX, (int)mouseY)) {
                onCardClicked(1);
                return true;
            }
            if (bottomLeftCard.contains((int)mouseX, (int)mouseY)) {
                onCardClicked(2);
                return true;
            }
            if (bottomRightCard.contains((int)mouseX, (int)mouseY)) {
                onCardClicked(3);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onCardClicked(int index) {
        if (this.minecraft != null && this.minecraft.player != null) {
            if (index == 0) {
            } else {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("message.pathsrole.mimi.card_clicked", index + 1),
                    true
                );
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new AsIWriteScreen());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Particle createParticle(boolean randomY) {
        float x = random.nextFloat() * this.width;
        float y = randomY ? random.nextFloat() * this.height : -10f - random.nextFloat() * 50f;
        float size = 3f + random.nextFloat() * 8f;
        float speedX = (random.nextFloat() - 0.5f) * 1.5f;
        float speedY = 0.5f + random.nextFloat() * 1.5f;
        float rotation = random.nextFloat() * 360f;
        float rotationSpeed = (random.nextFloat() - 0.5f) * 3f;
        int colorType = random.nextInt(3);
        return new Particle(x, y, size, speedX, speedY, rotation, rotationSpeed, colorType);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private void drawDiamond(GuiGraphics g, int cx, int cy, int radius, int r, int gVal, int bVal, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = g.pose().last().pose();
        int color = ((int)(alpha * 255) << 24) | (r << 16) | (gVal << 8) | bVal;

        buffer.addVertex(matrix, cx, cy - radius, 0).setColor(color);
        buffer.addVertex(matrix, cx - radius, cy, 0).setColor(color);
        buffer.addVertex(matrix, cx, cy + radius, 0).setColor(color);
        buffer.addVertex(matrix, cx + radius, cy, 0).setColor(color);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static class CornerCard {
        int x, y, width, height;
        Component title;
        Component description;
        int accentColor;

        CornerCard(int x, int y, int width, int height, Component title, Component description,
                   int accentColor) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.title = title;
            this.description = description;
            this.accentColor = accentColor;
        }

        boolean contains(int mx, int my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }
    }

    private static class Star {
        float x, y;
        float brightness;
        float twinkle;
        float twinkleSpeed;
        float size;

        Star(float x, float y, float brightness, float size) {
            this.x = x;
            this.y = y;
            this.brightness = brightness;
            this.size = size;
            this.twinkle = 0.5f + (float)Math.random() * 0.5f;
            this.twinkleSpeed = 0.01f + (float)Math.random() * 0.03f;
        }
    }

    private static class Particle {
        float x, y;
        float size;
        float speedX, speedY;
        float rotation, rotationSpeed;
        int colorType;

        Particle(float x, float y, float size, float speedX, float speedY, float rotation, float rotationSpeed, int colorType) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speedX = speedX;
            this.speedY = speedY;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
            this.colorType = colorType;
        }
    }
}