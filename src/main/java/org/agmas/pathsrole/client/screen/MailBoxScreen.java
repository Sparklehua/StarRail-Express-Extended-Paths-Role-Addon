package org.agmas.pathsrole.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.agmas.noellesroles.content.item.CourierMailData;
import org.agmas.noellesroles.content.item.CourierMailItem;
import org.agmas.noellesroles.client.screen.CourierMailReceiveScreen;
import org.joml.Matrix4f;

@Environment(value=EnvType.CLIENT)
public class MailBoxScreen extends Screen {

    private static final int VOID = 0xFF080B14;
    private static final int INK = 0xFF0F1428;
    private static final int PANEL = 0xFF0F1828;
    private static final int PANEL_SOFT = 0xFF182440;
    private static final int GOLD = 0xFFD4AF37;
    private static final int GOLD_BRIGHT = 0xFFFFE1A0;
    private static final int IVORY = 0xFFF7EBCF;
    private static final int TEXT = 0xFFFFF4DC;
    private static final int TEXT_MUTED = 0xFF9E8B6E;
    private static final int ACCENT = 0xFF4A9EFF;

    private static final int LIST_WIDTH = 220;
    private static final int ITEM_HEIGHT = 36;
    private static final int MAX_VISIBLE_ITEMS = 10;

    private final List<ItemStack> mailItems = new ArrayList<>();
    private int selectedMail = 0;
    private double listScrollOffset = 0.0;
    private double listScrollTarget = 0.0;

    private final List<Star> stars = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private float animTime = 0f;

    public MailBoxScreen() {
        super(Component.translatable("screen.pathsrole.mailbox"));
        this.collectMailsFromInventory();
        this.initStars();
        this.initParticles();
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(
            Component.literal("← " + Component.translatable("gui.back").getString()),
            b -> this.onClose()
        ).bounds(8, 6, 70, 20).build());
    }

    private void collectMailsFromInventory() {
        this.mailItems.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof CourierMailItem) {
                this.mailItems.add(stack);
            }
        }

        if (this.selectedMail >= this.mailItems.size()) {
            this.selectedMail = Math.max(0, this.mailItems.size() - 1);
        }
    }

    private void initStars() {
        for (int i = 0; i < 100; i++) {
            this.stars.add(new Star(
                this.random.nextFloat(),
                this.random.nextFloat(),
                0.3f + this.random.nextFloat() * 0.7f,
                0.5f + this.random.nextFloat()
            ));
        }
    }

    private void initParticles() {
        for (int i = 0; i < 25; i++) {
            this.particles.add(this.createParticle(true));
        }
    }

    private Particle createParticle(boolean randomY) {
        return new Particle(
            this.random.nextFloat(),
            randomY ? this.random.nextFloat() : -0.05f - this.random.nextFloat() * 0.1f,
            2f + this.random.nextFloat() * 5f,
            (this.random.nextFloat() - 0.5f) * 0.003f,
            0.3f + this.random.nextFloat() * 0.7f,
            (this.random.nextFloat() - 0.5f) * 2f,
            (this.random.nextFloat() - 0.5f) * 3f,
            this.random.nextInt(3)
        );
    }

    @Override
    public void tick() {
        this.animTime += 0.016f;

        double diff = this.listScrollTarget - this.listScrollOffset;
        if (Math.abs(diff) > 0.5) {
            this.listScrollOffset += diff * 0.15;
        } else {
            this.listScrollOffset = this.listScrollTarget;
        }

        for (Particle p : this.particles) {
            p.x += p.speedX;
            p.y += p.speedY;
            p.rotation += p.rotationSpeed;
            if (p.y > 1.15 || p.x < -0.1 || p.x > 1.1) {
                Particle np = this.createParticle(false);
                p.x = np.x; p.y = np.y; p.size = np.size;
                p.speedX = np.speedX; p.speedY = np.speedY;
                p.rotation = np.rotation; p.rotationSpeed = np.rotationSpeed;
                p.colorType = np.colorType;
            }
        }

        for (Star s : this.stars) {
            s.twinkle += s.twinkleSpeed;
            if (s.twinkle > 1f || s.twinkle < 0.3f) s.twinkleSpeed = -s.twinkleSpeed;
        }

        super.tick();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        float elapsed = this.animTime;

        g.fill(0, 0, this.width, this.height, VOID);
        this.renderStars(g);
        this.renderParticles(g);
        this.renderGlowingRings(g);

        int listX = 12;
        int listY = 34;
        int listH = this.height - 50;
        int detailX = listX + LIST_WIDTH + 12;
        int detailW = this.width - detailX - 12;

        this.renderMailList(g, listX, listY, listH, mouseX, mouseY);
        this.renderMailDetail(g, detailX, listY, detailW, listH);

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderStars(GuiGraphics g) {
        for (Star star : this.stars) {
            float alpha = star.brightness * star.twinkle;
            int color = FastColor.ARGB32.color((int)(alpha * 255), 200, 220, 255);
            g.fill((int)(star.x * this.width), (int)(star.y * this.height),
                  (int)(star.x * this.width) + 2, (int)(star.y * this.height) + 2, color);
        }
    }

    private void renderParticles(GuiGraphics g) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Particle p : this.particles) {
            g.pose().pushPose();
            g.pose().translate(p.x * this.width, p.y * this.height, 0);
            g.pose().mulPose(new org.joml.Quaternionf().rotateZ((float)Math.toRadians(p.rotation)));

            int alpha = 160 + (int)(this.random.nextFloat() * 95);
            int color;
            switch (p.colorType) {
                case 0: color = FastColor.ARGB32.color(alpha, 74, 158, 255); break;
                case 1: color = FastColor.ARGB32.color(alpha, 212, 175, 55); break;
                default: color = FastColor.ARGB32.color(alpha, 255, 255, 255); break;
            }

            float half = p.size / 2f;
            Matrix4f matrix = g.pose().last().pose();
            buffer.addVertex(matrix, -half, -half, 0).setColor(color);
            buffer.addVertex(matrix, -half, half, 0).setColor(color);
            buffer.addVertex(matrix, half, half, 0).setColor(color);
            buffer.addVertex(matrix, half, -half, 0).setColor(color);

            g.pose().popPose();
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void renderGlowingRings(GuiGraphics g) {
        float cx = this.width / 2f;
        float cy = this.height / 2f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (int ring = 0; ring < 2; ring++) {
            float radius = 120f + ring * 100f;
            int segments = 48;
            float alpha = 0.06f - ring * 0.02f;
            int baseColor = ((int)(alpha * 255) << 24) | (0x4A9EFF & 0x00FFFFFF);

            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f matrix = g.pose().last().pose();

            for (int i = 0; i <= segments; i++) {
                float angle = (float)i / segments * (float)(Math.PI * 2) + this.animTime * (0.4f + ring * 0.25f);
                float wobble = (float)Math.sin(angle * 3 + this.animTime * 1.5f) * 12f;
                float r = radius + wobble;
                float x = cx + (float)Math.cos(angle) * r;
                float y = cy + (float)Math.sin(angle) * r;
                buffer.addVertex(matrix, x, y, 0).setColor(baseColor);
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        RenderSystem.disableBlend();
    }

    private void renderMailList(GuiGraphics g, int x, int y, int h, int mx, int my) {
        g.fill(x, y, x + LIST_WIDTH, y + h, PANEL);
        g.renderOutline(x, y, LIST_WIDTH, h, withAlpha(GOLD, 100));

        String title = Component.translatable("screen.pathsrole.mailbox.list").getString();
        g.drawString(this.font, title, x + 12, y + 10, IVORY, false);
        g.fill(x + 10, y + 28, x + LIST_WIDTH - 10, y + 29, withAlpha(GOLD, 150));

        int itemStartY = y + 38;
        int visibleH = h - 48;
        int totalH = this.mailItems.size() * ITEM_HEIGHT;
        double maxScroll = Math.max(0, totalH - visibleH);
        this.listScrollTarget = Mth.clamp(this.listScrollTarget, 0, maxScroll);

        g.enableScissor(x, itemStartY, x + LIST_WIDTH, itemStartY + visibleH);

        for (int i = 0; i < this.mailItems.size(); i++) {
            int itemY = itemStartY + i * ITEM_HEIGHT - (int)this.listScrollOffset;
            if (itemY + ITEM_HEIGHT < itemStartY || itemY > itemStartY + visibleH) continue;

            boolean isSelected = (i == this.selectedMail);
            boolean isHovered = (mx >= x && mx <= x + LIST_WIDTH && my >= itemY && my <= itemY + ITEM_HEIGHT);

            if (isSelected) {
                g.fill(x + 4, itemY, x + LIST_WIDTH - 4, itemY + ITEM_HEIGHT, PANEL_SOFT);
                g.fill(x + 4, itemY, x + 5, itemY + ITEM_HEIGHT, ACCENT);
            } else if (isHovered) {
                g.fill(x + 4, itemY, x + LIST_WIDTH - 4, itemY + ITEM_HEIGHT, withAlpha(PANEL_SOFT, 180));
            }

            int iconCx = x + 24;
            int iconCy = itemY + ITEM_HEIGHT / 2;
            this.drawEnvelopeIcon(g, iconCx - 8, iconCy - 6, isSelected ? GOLD_BRIGHT : (isHovered ? GOLD : TEXT_MUTED));

            ItemStack mailStack = this.mailItems.get(i);
            String mailTitle = CourierMailData.getMessage(mailStack);
            if (mailTitle.isEmpty()) {
                mailTitle = Component.translatable("screen.pathsrole.mailbox.no_subject").getString();
            } else if (mailTitle.length() > 20) {
                mailTitle = mailTitle.substring(0, 20) + "...";
            }
            int textX = x + 42;
            int textColor = isSelected ? 0xFFFFFFFF : (isHovered ? TEXT : TEXT_MUTED);
            g.drawString(this.font, mailTitle, textX, itemY + 11, textColor, false);
        }

        g.disableScissor();

        if (totalH > visibleH) {
            int barX = x + LIST_WIDTH - 6;
            int barH = (int)((visibleH / (float)totalH) * visibleH);
            int barY = itemStartY + (int)((this.listScrollOffset / maxScroll) * (visibleH - barH));
            g.fill(barX, barY, barX + 3, barY + barH, withAlpha(GOLD_BRIGHT, 180));
        }
    }

    private void drawEnvelopeIcon(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 16, y + 11, color);
        g.fill(x, y + 11, x + 16, y + 12, withAlpha(color, 180));
        for (int i = 0; i < 8; i++) {
            int alpha = 200 - i * 20;
            if (alpha < 50) alpha = 50;
            g.fill(x + i, y + i, x + 16 - i, y + i + 1, withAlpha(color, alpha));
        }
        g.fill(x + 7, y + 10, x + 9, y + 12, withAlpha(color, 220));
    }

    private void renderMailDetail(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.renderOutline(x, y, w, h, withAlpha(GOLD, 80));

        if (this.mailItems.isEmpty()) {
            String emptyText = Component.translatable("screen.pathsrole.mailbox.empty").getString();
            g.drawCenteredString(this.font, emptyText, x + w / 2, y + h / 2 - 6, TEXT_MUTED);
            return;
        }

        ItemStack mailStack = this.mailItems.get(this.selectedMail);
        String mailMessage = CourierMailData.getMessage(mailStack);
        String mailSender = CourierMailData.getSender(mailStack);

        int headerH = 90;
        g.fill(x + 20, y + 20, x + w - 20, y + headerH, INK);
        g.renderOutline(x + 20, y + 20, w - 40, headerH - 20, withAlpha(ACCENT, 100));

        this.drawEnvelopeIcon(g, x + 36, y + 32, GOLD_BRIGHT);

        String displayTitle = mailMessage.isEmpty() 
            ? Component.translatable("screen.pathsrole.mailbox.no_subject").getString()
            : (mailMessage.length() > 30 ? mailMessage.substring(0, 30) + "..." : mailMessage);
        g.drawString(this.font, displayTitle, x + 64, y + 32, IVORY, false);

        if (!mailSender.isEmpty()) {
            g.drawString(this.font, Component.translatable("screen.pathsrole.mailbox.sender").getString() + "「" + mailSender + "」", x + 64, y + 52, TEXT, false);
        }
        g.drawString(this.font, Component.translatable("screen.pathsrole.mailbox.receiver").getString() + Minecraft.getInstance().player.getName().getString(), x + 64, y + 68, TEXT_MUTED, false);

        g.fill(x + 30, y + headerH + 8, x + w - 30, y + headerH + 9, withAlpha(GOLD, 100));

        int contentY = y + headerH + 18;
        int contentH = h - headerH - 30;

        g.enableScissor(x, contentY, x + w, contentY + contentH);

        if (!mailMessage.isEmpty()) {
            String[] paragraphs = mailMessage.split("\n");
            int lineY = contentY;
            int lineH = 14;

            for (String paragraph : paragraphs) {
                if (lineY > contentY + contentH) break;
                if (paragraph.isEmpty()) {
                    lineY += lineH;
                    continue;
                }
                List<String> wrappedLines = this.wrapText(paragraph, w - 60);
                for (String wl : wrappedLines) {
                    if (lineY > contentY + contentH) break;
                    g.drawString(this.font, wl, x + 34, lineY, TEXT, false);
                    lineY += lineH;
                }
                lineY += 4;
            }
        } else {
            g.drawString(this.font, Component.translatable("screen.pathsrole.mailbox.no_content").getString(), x + 34, contentY, TEXT_MUTED, false);
        }

        g.disableScissor();
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            current.append(text.charAt(i));
            if (this.font.width(current.toString()) > maxWidth) {
                lines.add(current.substring(0, current.length() - 1));
                current = new StringBuilder();
                current.append(text.charAt(i));
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines.isEmpty() ? List.of("") : lines;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int listX = 12;
        int listY = 38;
        int listH = this.height - 64;

        if (mx >= listX && mx <= listX + LIST_WIDTH && my >= listY && my <= listY + listH) {
            int relativeY = (int)my - listY + (int)this.listScrollOffset;
            int clickedIndex = relativeY / ITEM_HEIGHT;
            if (clickedIndex >= 0 && clickedIndex < this.mailItems.size()) {
                if (clickedIndex == this.selectedMail && btn == 0) {
                    this.openMailDetail();
                } else {
                    this.selectedMail = clickedIndex;
                    this.listScrollTarget = Mth.clamp(clickedIndex * ITEM_HEIGHT - listH / 2 + ITEM_HEIGHT / 2,
                        0, Math.max(0, this.mailItems.size() * ITEM_HEIGHT - listH));
                }
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    private void openMailDetail() {
        if (this.mailItems.isEmpty()) return;
        
        ItemStack mailStack = this.mailItems.get(this.selectedMail);
        Minecraft.getInstance().setScreen(new CourierMailReceiveScreen(InteractionHand.MAIN_HAND));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int listX = 12;
        int listY = 34;
        int listH = this.height - 50;

        if (mx >= listX && mx <= listX + LIST_WIDTH && my >= listY && my <= listY + listH) {
            int totalH = this.mailItems.size() * ITEM_HEIGHT;
            double maxScroll = Math.max(0, totalH - listH);
            this.listScrollTarget = Mth.clamp(this.listScrollTarget - dy * 40, 0, maxScroll);
            return true;
        }

        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static class Star {
        float x, y, brightness, twinkle, twinkleSpeed;
        Star(float x, float y, float b, float ts) { this.x=x;this.y=y;this.brightness=b;this.twinkle=0.5f+randomFloat();this.twinkleSpeed=0.01f+randomFloat()*0.03f; }
        private static float randomFloat() { return (float)Math.random(); }
    }

    private static class Particle {
        float x, y, size, speedX, speedY, rotation, rotationSpeed;
        int colorType;
        Particle(float x, float y, float s, float sx, float sy, float r, float rs, int ct) {
            this.x=x;this.y=y;this.size=s;this.speedX=sx;this.speedY=sy;this.rotation=r;this.rotationSpeed=rs;this.colorType=ct;
        }
    }
}