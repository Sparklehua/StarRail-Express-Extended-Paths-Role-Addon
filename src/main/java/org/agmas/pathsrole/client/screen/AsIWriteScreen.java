package org.agmas.pathsrole.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import org.agmas.pathsrole.client.widget.CardButton;
import org.agmas.pathsrole.client.widget.CardButton.IconType;
import org.agmas.pathsrole.network.MimiRequestPacket;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.agmas.pathsrole.client.screen.PathStoryCollectionScreen;

@Environment(value=EnvType.CLIENT)
public class AsIWriteScreen extends Screen {

    private static final ResourceLocation CARD_TEXTURE = ResourceLocation.fromNamespaceAndPath("pathsrole", "textures/gui/as_i_write_card.png");
    private static final ResourceLocation COURIER_MAIL_TEXTURE = ResourceLocation.fromNamespaceAndPath("pathsrole", "textures/gui/mail.png");

    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 91;
    private static final int CARD_GAP_X = 50;
    private static final int CARD_GAP_Y = 40;
    private static final int CARDS_PER_ROW = 3;
    private static final int TOTAL_CARDS = 6;

    private final List<Particle> particles = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();
    private final Random random = new Random();
    private float animTime = 0f;

    private int gridStartX;
    private int gridStartY;

    public AsIWriteScreen() {
        super(Component.translatable("item.pathsrole.as_i_write"));
    }

    protected void init() {
        super.init();
        this.initStars();
        this.initParticles();

        int totalGridWidth = CARDS_PER_ROW * CARD_WIDTH + (CARDS_PER_ROW - 1) * CARD_GAP_X;
        int totalGridHeight = 2 * CARD_HEIGHT + CARD_GAP_Y;

        this.gridStartX = (this.width - totalGridWidth) / 2;
        this.gridStartY = this.height * 25 / 100;

        String[] cardNames = new String[]{
            "text.pathsrole.as_i_write.card1",
            "text.pathsrole.as_i_write.card2",
            "text.pathsrole.as_i_write.card3",
            "text.pathsrole.as_i_write.card4",
            "text.pathsrole.as_i_write.card5",
            "text.pathsrole.as_i_write.card6"
        };

        IconType[] cardIcons = new IconType[]{
            IconType.BOOK,
            IconType.CARDS,
            IconType.WINGED_SPIRIT,
            IconType.DOCUMENTS,
            IconType.MAGNIFIER,
            IconType.ENVELOPE
        };

        for (int i = 0; i < TOTAL_CARDS; i++) {
            int row = i / CARDS_PER_ROW;
            int col = i % CARDS_PER_ROW;
            int cardX = this.gridStartX + col * (CARD_WIDTH + CARD_GAP_X);
            int cardY = this.gridStartY + row * (CARD_HEIGHT + CARD_GAP_Y);
            final int cardIndex = i;
            
            CardButton card;
            if (i == 5) {
                card = new CardButton(cardX, cardY, CARD_WIDTH, CARD_HEIGHT,
                    Component.translatable(cardNames[i]), b -> this.onCardClicked(cardIndex), COURIER_MAIL_TEXTURE);
            } else {
                card = new CardButton(cardX, cardY, CARD_WIDTH, CARD_HEIGHT,
                    Component.translatable(cardNames[i]), b -> this.onCardClicked(cardIndex), cardIcons[i]);
            }
            this.addRenderableWidget(card);
        }

        this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> this.onClose())
            .bounds(this.width - 25, 5, 20, 20)
            .build());
    }

    private void onCardClicked(int index) {
        if (this.minecraft != null && this.minecraft.player != null) {
            if (index == 0) {
                this.minecraft.setScreen(new PathStoryCollectionScreen());
            } else if (index == 1) {
                this.minecraft.setScreen(new GameCollectionScreen());
            } else if (index == 2) {
                ClientPlayNetworking.send(new MimiRequestPacket());
            } else if (index == 3) {
            } else if (index == 5) {
                this.minecraft.setScreen(new MailBoxScreen());
            } else {
                this.minecraft.player.displayClientMessage(
                    Component.translatable("message.pathsrole.as_i_write.clicked", index + 1),
                    true
                );
            }
        }
    }

    private void initStars() {
        this.stars.clear();
        for (int i = 0; i < 150; i++) {
            this.stars.add(new Star(
                this.random.nextFloat() * this.width,
                this.random.nextFloat() * this.height,
                0.3f + this.random.nextFloat() * 0.7f,
                1f + this.random.nextFloat() * 2f
            ));
        }
    }

    private void initParticles() {
        this.particles.clear();
        for (int i = 0; i < 40; i++) {
            this.particles.add(this.createParticle(true));
        }
    }

    private Particle createParticle(boolean randomY) {
        float x = this.random.nextFloat() * this.width;
        float y = randomY ? this.random.nextFloat() * this.height : -10f - this.random.nextFloat() * 50f;
        float size = 3f + this.random.nextFloat() * 8f;
        float speedX = (this.random.nextFloat() - 0.5f) * 1.5f;
        float speedY = 0.5f + this.random.nextFloat() * 1.5f;
        float rotation = this.random.nextFloat() * 360f;
        float rotationSpeed = (this.random.nextFloat() - 0.5f) * 3f;
        int colorType = this.random.nextInt(3);
        return new Particle(x, y, size, speedX, speedY, rotation, rotationSpeed, colorType);
    }

    public void tick() {
        this.animTime += 0.016f;

        for (Particle p : this.particles) {
            p.x += p.speedX;
            p.y += p.speedY;
            p.rotation += p.rotationSpeed;
            if (p.y > this.height + 20 || p.x < -20 || p.x > this.width + 20) {
                Particle newP = this.createParticle(false);
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

        for (Star s : this.stars) {
            s.twinkle += s.twinkleSpeed;
            if (s.twinkle > 1f || s.twinkle < 0.3f) {
                s.twinkleSpeed = -s.twinkleSpeed;
            }
        }

        super.tick();
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackgroundLayer(guiGraphics);
        this.renderStarfield(guiGraphics);
        this.renderParticles(guiGraphics);
        this.renderGlowingRings(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderCardLabels(guiGraphics);
        this.renderTitle(guiGraphics);
    }

    private void renderBackgroundLayer(GuiGraphics guiGraphics) {
        Matrix4f matrix = guiGraphics.pose().last().pose();
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

    private void renderStarfield(GuiGraphics guiGraphics) {
        for (Star star : this.stars) {
            float alpha = star.brightness * star.twinkle;
            int color = FastColor.ARGB32.color((int)(alpha * 255), 255, 255, (int)(alpha * 220));
            guiGraphics.fill((int)star.x, (int)star.y, (int)star.x + 2, (int)star.y + 2, color);
        }
    }

    private void renderParticles(GuiGraphics guiGraphics) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Particle p : this.particles) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(p.x, p.y, 0);
            guiGraphics.pose().mulPose(new Quaternionf().rotateZ((float)Math.toRadians(p.rotation)));

            int alpha = 180 + (int)(this.random.nextFloat() * 75);
            int color;
            switch (p.colorType) {
                case 0:
                    color = FastColor.ARGB32.color(alpha, 200, 255, 255);
                    break;
                case 1:
                    color = FastColor.ARGB32.color(alpha, 255, 180, 220);
                    break;
                default:
                    color = FastColor.ARGB32.color(alpha, 255, 255, 255);
                    break;
            }

            float halfSize = p.size / 2f;
            Matrix4f matrix = guiGraphics.pose().last().pose();

            buffer.addVertex(matrix, -halfSize, -halfSize, 0).setColor(color);
            buffer.addVertex(matrix, -halfSize, halfSize, 0).setColor(color);
            buffer.addVertex(matrix, halfSize, halfSize, 0).setColor(color);
            buffer.addVertex(matrix, halfSize, -halfSize, 0).setColor(color);

            guiGraphics.pose().popPose();
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void renderGlowingRings(GuiGraphics guiGraphics) {
        float centerX = this.width / 2f;
        float centerY = this.height / 2f;
        float time = this.animTime;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (int ring = 0; ring < 3; ring++) {
            float radius = 100f + ring * 80f;
            int segments = 60;
            float alpha = 0.12f - ring * 0.03f;
            int baseColor = withAlpha(0xB4A0FF, (int)(alpha * 255));

            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f matrix = guiGraphics.pose().last().pose();

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

    private void renderCardLabels(GuiGraphics guiGraphics) {
        String[] labels = new String[]{
            "命途角色故事集.exe",
            "休闲游戏.exe",
            "迷迷.exe",
            "命途角色反馈.wav",
            "播放控制遥控.exe",
            "邮箱.exe"
        };

        for (int i = 0; i < TOTAL_CARDS; i++) {
            int row = i / CARDS_PER_ROW;
            int col = i % CARDS_PER_ROW;
            int cardCenterX = this.gridStartX + col * (CARD_WIDTH + CARD_GAP_X) + CARD_WIDTH / 2;
            int cardLabelY = this.gridStartY + row * (CARD_HEIGHT + CARD_GAP_Y) + CARD_HEIGHT + 5;

            String label = labels[i];
            int textWidth = this.font.width(label);
            guiGraphics.drawString(this.font, label, cardCenterX - textWidth / 2, cardLabelY, 0xE8D8F0, false);
        }
    }

    private void renderTitle(GuiGraphics guiGraphics) {
        String title = Component.translatable("item.pathsrole.as_i_write").getString();
        int titleWidth = this.font.width(title);

        guiGraphics.drawCenteredString(this.font, title, this.width / 2, 15, 0xFFFFFF);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
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