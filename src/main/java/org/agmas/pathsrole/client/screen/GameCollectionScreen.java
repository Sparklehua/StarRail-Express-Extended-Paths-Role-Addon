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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Environment(value = EnvType.CLIENT)
public class GameCollectionScreen extends Screen {

    private static final int CARD_BG = 0xCC0D0D1A;
    private static final int GOLD = 0xFFC8A84E;
    private static final int GOLD_BRIGHT = 0xFFE8C56D;
    private static final int IVORY = 0xFFF5F0E8;
    private static final int TEXT_MUTED = 0xFF9E8B6E;

    private static final int CARD_WIDTH = 160;
    private static final int CARD_HEIGHT = 150;
    private static final int CARD_PADDING = 16;
    private static final int TOP_OFFSET = 50;
    private static final int BOTTOM_PADDING = 20;
    private static final int SIDE_MARGIN = 20;

    private final Random random = new Random();
    private final List<Star> stars = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private float animTime;
    private int tickCounter;
    private int hoveredCard = -1;

    private static final String[][] GAMES = {
        {"dou_dizhu", "text.pathsrole.game.dou_dizhu", "text.pathsrole.game.dou_dizhu_desc", "0xFFE8C56D"},
    };

    public GameCollectionScreen() {
        super(Component.translatable("screen.pathsrole.game_collection"));
    }

    @Override
    protected void init() {
        super.init();
        stars.clear();
        particles.clear();
        animTime = 0f;
        tickCounter = 0;
        hoveredCard = -1;

        for (int i = 0; i < 100; i++) {
            stars.add(new Star(
                random.nextFloat() * this.width,
                random.nextFloat() * this.height,
                0.3f + random.nextFloat() * 0.7f,
                1f + random.nextFloat() * 2f
            ));
        }
        for (int i = 0; i < 25; i++) {
            particles.add(createParticle(true));
        }

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
        renderTitle(g);
        renderCards(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, delta);
    }

    private void renderBackgroundLayer(GuiGraphics g) {
        Matrix4f matrix = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(matrix, 0, this.height, 0).setColor(withAlpha(0x0F0826, 255));
        buffer.addVertex(matrix, this.width, this.height, 0).setColor(withAlpha(0x190C37, 255));
        buffer.addVertex(matrix, this.width, 0, 0).setColor(withAlpha(0x23124B, 255));
        buffer.addVertex(matrix, 0, 0, 0).setColor(withAlpha(0x140A2D, 255));
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
            int alpha = 120 + (int)(random.nextFloat() * 40);
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

    private void renderTitle(GuiGraphics g) {
        Component title = Component.translatable("screen.pathsrole.game_collection");
        int titleWidth = this.font.width(title);
        int titleX = (this.width - titleWidth) / 2;
        float pulse = 1.0f + Mth.sin(tickCounter * 0.04f) * 0.05f;
        g.pose().pushPose();
        g.pose().translate(titleX + titleWidth / 2f, 14f, 0);
        g.pose().scale(pulse, pulse, 1.0f);
        g.drawString(this.font, title, -titleWidth / 2, 0, GOLD_BRIGHT, true);
        g.pose().popPose();

        g.fill(titleX - 20, 26, titleX + titleWidth + 20, 27, 0x66C8A84E);
    }

    private void renderCards(GuiGraphics g, int mouseX, int mouseY) {
        int cols = Math.max(1, (this.width - SIDE_MARGIN * 2 + CARD_PADDING) / (CARD_WIDTH + CARD_PADDING));
        int totalCardsWidth = cols * CARD_WIDTH + (cols - 1) * CARD_PADDING;
        int startX = (this.width - totalCardsWidth) / 2;

        for (int i = 0; i < GAMES.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cardX = startX + col * (CARD_WIDTH + CARD_PADDING);
            int cardY = TOP_OFFSET + row * (CARD_HEIGHT + CARD_PADDING);

            if (cardY + CARD_HEIGHT > this.height - BOTTOM_PADDING) continue;
            if (cardY < TOP_OFFSET - CARD_HEIGHT) continue;

            boolean hovered = (hoveredCard == i);
            renderGameCard(g, cardX, cardY, GAMES[i], hovered);
        }
    }

    private void renderGameCard(GuiGraphics g, int x, int y, String[] game, boolean hovered) {
        int bgColor = hovered ? 0xDD15152E : CARD_BG;
        g.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, bgColor);

        int borderAlpha = hovered ? 0xFF : 0x99;
        int borderColor = (borderAlpha << 24) | (GOLD & 0x00FFFFFF);
        g.fill(x, y, x + CARD_WIDTH, y + 1, borderColor);
        g.fill(x, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, borderColor);
        g.fill(x, y, x + 1, y + CARD_HEIGHT, borderColor);
        g.fill(x + CARD_WIDTH - 1, y, x + CARD_WIDTH, y + CARD_HEIGHT, borderColor);

        g.fill(x, y, x + 6, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + 6, 0xFFFFFFFF);
        g.fill(x + CARD_WIDTH - 6, y, x + CARD_WIDTH, y + 1, 0xFFFFFFFF);
        g.fill(x + CARD_WIDTH - 1, y, x + CARD_WIDTH, y + 6, 0xFFFFFFFF);
        g.fill(x, y + CARD_HEIGHT - 6, x + 1, y + CARD_HEIGHT, 0xFFFFFFFF);
        g.fill(x, y + CARD_HEIGHT - 1, x + 6, y + CARD_HEIGHT, 0xFFFFFFFF);
        g.fill(x + CARD_WIDTH - 6, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFFFFFFFF);
        g.fill(x + CARD_WIDTH - 1, y + CARD_HEIGHT - 6, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFFFFFFFF);

        int iconSize = 36;
        int iconCenterX = x + CARD_WIDTH / 2;
        int iconCenterY = y + 32;
        drawDiamond(g, iconCenterX, iconCenterY, iconSize / 2, 0xE8, 0xC5, 0x6D, hovered ? 1.0f : 0.7f);
        drawDiamond(g, iconCenterX, iconCenterY, iconSize / 3, 0xE8, 0xC5, 0x6D, hovered ? 0.6f : 0.35f);

        Component title = Component.translatable(game[1]);
        int titleWidth = this.font.width(title);
        g.drawString(this.font, title, x + (CARD_WIDTH - titleWidth) / 2, iconCenterY + iconSize / 2 + 10, GOLD_BRIGHT, true);

        Component desc = Component.translatable(game[2]);
        int descWidth = this.font.width(desc);
        g.drawString(this.font, desc, x + (CARD_WIDTH - descWidth) / 2, iconCenterY + iconSize / 2 + 26, TEXT_MUTED, false);

        if (hovered) {
            for (int i = 0; i < 2; i++) {
                int glowAlpha = 40 - i * 15;
                int glowColor = (glowAlpha << 24) | (GOLD & 0x00FFFFFF);
                g.fill(x - 1 - i, y - 1 - i, x + CARD_WIDTH + 1 + i, y - i, glowColor);
                g.fill(x - 1 - i, y + CARD_HEIGHT + i, x + CARD_WIDTH + 1 + i, y + CARD_HEIGHT + 1 + i, glowColor);
                g.fill(x - 1 - i, y - 1 - i, x - i, y + CARD_HEIGHT + 1 + i, glowColor);
                g.fill(x + CARD_WIDTH + i, y - 1 - i, x + CARD_WIDTH + 1 + i, y + CARD_HEIGHT + 1 + i, glowColor);
            }
        }
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int cols = Math.max(1, (this.width - SIDE_MARGIN * 2 + CARD_PADDING) / (CARD_WIDTH + CARD_PADDING));
            int totalCardsWidth = cols * CARD_WIDTH + (cols - 1) * CARD_PADDING;
            int startX = (this.width - totalCardsWidth) / 2;

            for (int i = 0; i < GAMES.length; i++) {
                int col = i % cols;
                int row = i / cols;
                int cardX = startX + col * (CARD_WIDTH + CARD_PADDING);
                int cardY = TOP_OFFSET + row * (CARD_HEIGHT + CARD_PADDING);
                if (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT) {
                    onGameClicked(GAMES[i][0]);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onGameClicked(String gameId) {
        if (this.minecraft == null) return;
        switch (gameId) {
            default:
                break;
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        hoveredCard = -1;
        int cols = Math.max(1, (this.width - SIDE_MARGIN * 2 + CARD_PADDING) / (CARD_WIDTH + CARD_PADDING));
        int totalCardsWidth = cols * CARD_WIDTH + (cols - 1) * CARD_PADDING;
        int startX = (this.width - totalCardsWidth) / 2;

        for (int i = 0; i < GAMES.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int cardX = startX + col * (CARD_WIDTH + CARD_PADDING);
            int cardY = TOP_OFFSET + row * (CARD_HEIGHT + CARD_PADDING);
            if (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT) {
                hoveredCard = i;
                break;
            }
        }
        super.mouseMoved(mouseX, mouseY);
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
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Particle createParticle(boolean randomY) {
        float x = random.nextFloat() * this.width;
        float y = randomY ? random.nextFloat() * this.height : -10f - random.nextFloat() * 50f;
        float size = 3f + random.nextFloat() * 6f;
        float speedX = (random.nextFloat() - 0.5f) * 1.5f;
        float speedY = 0.3f + random.nextFloat() * 1.2f;
        float rotation = random.nextFloat() * 360f;
        float rotationSpeed = (random.nextFloat() - 0.5f) * 3f;
        int colorType = random.nextInt(3);
        return new Particle(x, y, size, speedX, speedY, rotation, rotationSpeed, colorType);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static class Star {
        float x, y, brightness, twinkle, twinkleSpeed, size;
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
        float x, y, size, speedX, speedY, rotation, rotationSpeed;
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