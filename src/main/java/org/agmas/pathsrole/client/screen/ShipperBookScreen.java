package org.agmas.pathsrole.client.screen;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.client.SREClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import org.agmas.pathsrole.game.roles.paths.elation.shipper.ShipperPlayerComponent;
import org.agmas.pathsrole.init.ModItems;

@Environment(value=EnvType.CLIENT)
public class ShipperBookScreen
extends Screen {
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath("pathsrole", "textures/gui/book.png");
    private int bookX;
    private int bookY;
    private final List<String> templates = new ArrayList<String>();
    private String currentText = "";
    private List<String> markedPlayerNames = new ArrayList<String>();
    private List<FormattedCharSequence> allLines = new ArrayList<FormattedCharSequence>();
    private int currentPage = 0;
    private int totalPages = 1;
    private int linesPerPage;
    private boolean isTouristMode = false;

    public ShipperBookScreen() {
        super(Component.literal("gui.pathsrole.shipper_book.title"));
    }

    protected void init() {
        super.init();
        this.bookX = (this.width - 192) / 2;
        this.bookY = (this.height - 192) / 2;
        this.loadTemplates();
        this.loadMarkedPlayers();
        int btnY = this.bookY + 192 - 30;
        this.addRenderableWidget(new PinkButton(this.bookX + (192 - 60) / 2, btnY, 60, 20,
            Component.translatable("gui.done"), b -> this.onClose()));
        if (this.isTouristMode && this.markedPlayerNames.size() >= 2) {
            this.addRenderableWidget(new PinkButton(this.bookX + 36, btnY - 22, 120, 18,
                Component.translatable("gui.pathsrole.shipper_book.regenerate"), b -> {
                    this.regenerateText();
                    this.initPageButtons();
                }));
        }
        this.regenerateText();
        this.initPageButtons();
    }

    private void initPageButtons() {
        if (this.totalPages > 1) {
            this.addRenderableWidget(new PinkButton(this.bookX + 36 - 5, this.bookY + 192 - 28, 20, 16,
                Component.literal("<"), b -> {
                    if (this.currentPage > 0) {
                        --this.currentPage;
                    }
                }));
            this.addRenderableWidget(new PinkButton(this.bookX + 36 + 120 - 15, this.bookY + 192 - 28, 20, 16,
                Component.literal(">"), b -> {
                    if (this.currentPage < this.totalPages - 1) {
                        ++this.currentPage;
                    }
                }));
        }
    }

    private void loadTemplates() {
        this.templates.clear();
        for (int i = 1; i <= 4; ++i) {
            String key = "text.pathsrole.shipper_template." + i;
            String translated = Component.translatable(key).getString();
            if (translated.equals(key)) continue;
            this.templates.add(translated);
        }
    }

    private void loadMarkedPlayers() {
        this.markedPlayerNames.clear();
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        ShipperPlayerComponent comp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)this.minecraft.player);
        if (comp != null) {
            for (UUID uuid : comp.getPairedLovers()) {
                this.markedPlayerNames.add(this.getPlayerNameWithRole(uuid, comp));
            }
            if (this.markedPlayerNames.isEmpty() && comp.hasEverBound()) {
                for (String name : comp.getAllPlayerNames()) {
                    this.markedPlayerNames.add(name);
                }
            }
            return;
        }
        this.isTouristMode = true;
        ItemStack stack = this.minecraft.player.getMainHandItem();
        if (!stack.is(ModItems.SHIPPER_BOOK)) {
            stack = this.minecraft.player.getOffhandItem();
        }
        if (!stack.is(ModItems.SHIPPER_BOOK)) {
            return;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }
        CompoundTag tag = customData.copyTag();
        String playerA = tag.getString("PlayerA");
        String playerB = tag.getString("PlayerB");
        if (!playerA.isEmpty()) {
            this.markedPlayerNames.add(playerA);
        }
        if (!playerB.isEmpty()) {
            this.markedPlayerNames.add(playerB);
        }
    }

    private String getPlayerNameWithRole(UUID uuid, ShipperPlayerComponent comp) {
        SRERole role;
        String name = comp.getPlayerName(uuid);
        if (name == null || name.isEmpty()) {
            name = "?";
        }
        if (SREClient.gameComponent != null && (role = SREClient.gameComponent.getRole(uuid)) != null) {
            return name + "\uff08" + role.getName().getString() + "\uff09";
        }
        return name;
    }

    private void regenerateText() {
        this.allLines.clear();
        this.currentPage = 0;
        ShipperPlayerComponent shipperComp = null;
        if (this.minecraft != null && this.minecraft.player != null) {
            shipperComp = (ShipperPlayerComponent)ShipperPlayerComponent.KEY.get((Object)this.minecraft.player);
        }
        if (shipperComp != null) {
            if (shipperComp.isRageActive() && shipperComp.getRageKiller() != null) {
                this.currentText = Component.translatable("message.pathsrole.shipper.rage_triggered").getString();
                this.allLines = this.font.split(Component.literal(this.currentText), 120);
                Objects.requireNonNull(this.font);
                this.linesPerPage = Math.max(1, 100 / 9);
                this.totalPages = Math.max(1, (this.allLines.size() + this.linesPerPage - 1) / this.linesPerPage);
                return;
            }
            if (!shipperComp.isRageActive() && !shipperComp.getAbsorbedPlayers().isEmpty() && shipperComp.getPairedLovers().isEmpty()) {
                this.currentText = Component.translatable("gui.pathsrole.shipper_book.absorbed_title").getString();
                for (UUID absorbedUUID : shipperComp.getAbsorbedPlayers()) {
                    this.currentText = this.currentText + "\n  - " + this.getPlayerNameWithRole(absorbedUUID, shipperComp);
                }
                this.allLines = this.font.split(Component.literal(this.currentText), 120);
                Objects.requireNonNull(this.font);
                this.linesPerPage = Math.max(1, 100 / 9);
                this.totalPages = Math.max(1, (this.allLines.size() + this.linesPerPage - 1) / this.linesPerPage);
                return;
            }
        }
        if (this.templates.isEmpty()) {
            this.currentText = Component.translatable("gui.pathsrole.shipper_book.no_templates").getString();
            this.allLines = this.font.split(Component.literal(this.currentText), 120);
            this.totalPages = 1;
            return;
        }
        if (this.markedPlayerNames.size() < 2) {
            this.currentText = Component.translatable("gui.pathsrole.shipper_book.no_players").getString();
            this.allLines = this.font.split(Component.literal(this.currentText), 120);
            this.totalPages = 1;
            return;
        }
        Random rand = new Random();
        String template = this.templates.get(rand.nextInt(this.templates.size()));
        String result = template.replace("\u3010\u73a9\u5bb6a\u3011", this.markedPlayerNames.get(0)).replace("\u3010\u73a9\u5bb6b\u3011", this.markedPlayerNames.get(1));
        if (shipperComp != null && !shipperComp.getAbsorbedPlayers().isEmpty()) {
            result = result + "\n\n" + Component.translatable("gui.pathsrole.shipper_book.absorbed_title").getString();
            for (UUID absorbedUUID : shipperComp.getAbsorbedPlayers()) {
                result = result + "\n  - " + this.getPlayerNameWithRole(absorbedUUID, shipperComp);
            }
        }
        this.currentText = result;
        this.allLines = this.font.split(Component.literal(this.currentText), 120);
        Objects.requireNonNull(this.font);
        this.linesPerPage = Math.max(1, 100 / 9);
        this.totalPages = Math.max(1, (this.allLines.size() + this.linesPerPage - 1) / this.linesPerPage);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.blit(BOOK_TEXTURE, this.bookX, this.bookY, 0, 0, 192, 192, 192, 192);
        if (!this.allLines.isEmpty()) {
            int lineY = this.bookY + 32;
            int startLine = this.currentPage * this.linesPerPage;
            int endLine = Math.min(startLine + this.linesPerPage, this.allLines.size());
            for (int i = startLine; i < endLine; ++i) {
                guiGraphics.drawString(this.font, this.allLines.get(i), this.bookX + 36, lineY, 0x000000, false);
                Objects.requireNonNull(this.font);
                lineY += 9;
            }
            if (this.totalPages > 1) {
                String pageText = this.currentPage + 1 + " / " + this.totalPages;
                int textWidth = this.font.width(pageText);
                guiGraphics.drawString(this.font, pageText, this.bookX + 36 + (120 - textWidth) / 2, this.bookY + 192 - 26, 0x333333, false);
            }
        }
    }

    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderTransparentBackground(guiGraphics);
    }

    public boolean isPauseScreen() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    private static class PinkButton extends Button {
        private static final int PINK_BG = 0xCCFF69B4;
        private static final int PINK_HOVER = 0xFFFF69B4;
        private static final int PINK_BORDER = 0xFFFF1493;
        private static final int PINK_TEXT = 0xCC000000;
        private static final int PINK_TEXT_HOVER = 0xFF000000;
        private static final int CORNER_RADIUS = 6;

        public PinkButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            boolean hovered = mouseX >= this.getX() && mouseX < this.getX() + this.getWidth()
                && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight();

            int bgColor = hovered ? PINK_HOVER : PINK_BG;
            this.drawRoundRect(guiGraphics, this.getX(), this.getY(),
                this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                CORNER_RADIUS, bgColor);

            this.drawRoundBorder(guiGraphics, this.getX(), this.getY(),
                this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                CORNER_RADIUS, 1, PINK_BORDER);

            if (hovered) {
                this.drawGlow(guiGraphics, this.getX(), this.getY(),
                    this.getX() + this.getWidth(), this.getY() + this.getHeight());
            }

            Minecraft mc = Minecraft.getInstance();
            int textColor = hovered ? PINK_TEXT_HOVER : PINK_TEXT;
            guiGraphics.drawCenteredString(mc.font, this.getMessage(),
                this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - 8) / 2,
                textColor);
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
            for (int row = 0; row < h; row++) {
                int edge = Math.min(row, h - 1 - row);
                int inset = 0;
                if (edge < r) {
                    int d = r - edge;
                    inset = r - (int)Math.floor(Math.sqrt((double)r * r - (double)d * d));
                }
                if (inset <= thickness) {
                    g.fill(x1, y1 + row, x1 + thickness, y1 + row + 1, color);
                    g.fill(x2 - thickness, y1 + row, x2, y1 + row + 1, color);
                }
            }
            int w = x2 - x1;
            for (int col = 0; col < w; col++) {
                int edge = Math.min(col, w - 1 - col);
                int inset = 0;
                if (edge < r) {
                    int d = r - edge;
                    inset = r - (int)Math.floor(Math.sqrt((double)r * r - (double)d * d));
                }
                if (inset <= thickness) {
                    g.fill(x1 + col, y1, x1 + col + 1, y1 + thickness, color);
                    g.fill(x1 + col, y2 - thickness, x1 + col + 1, y2, color);
                }
            }
        }

        private void drawGlow(GuiGraphics g, int x1, int y1, int x2, int y2) {
            int glowRadius = 2;
            for (int i = 1; i <= glowRadius; i++) {
                int alpha = 40 - i * 15;
                if (alpha > 0) {
                    int color = (alpha << 24) | 0xFFB6C1;
                    this.drawRoundRect(g, x1 - i, y1 - i, x2 + i, y2 + i, CORNER_RADIUS + i, color);
                }
            }
        }
    }
}