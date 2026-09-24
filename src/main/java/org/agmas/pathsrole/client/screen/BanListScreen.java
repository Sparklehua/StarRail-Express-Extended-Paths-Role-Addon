package org.agmas.pathsrole.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.agmas.pathsrole.network.BanListRequestPayload;
import org.agmas.pathsrole.network.BanListResponsePayload;
import org.agmas.pathsrole.network.BanTogglePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class BanListScreen extends Screen {

    private static final int GUI_WIDTH = 640;
    private static final int GUI_HEIGHT = 420;

    private static final int LEFT_CARD_X = 25;
    private static final int LEFT_CARD_Y = 85;
    private static final int LEFT_CARD_WIDTH = 225;
    private static final int LEFT_CARD_HEIGHT = 310;

    private static final int LIST_X = 265;
    private static final int LIST_Y = 120;
    private static final int LIST_WIDTH = 355;
    private static final int LIST_HEIGHT = 280;
    private static final int ROW_HEIGHT = 30;
    private static final int VISIBLE_ROWS = 8;

    private static final int TOGGLE_BTN_WIDTH = 62;
    private static final int TOGGLE_BTN_HEIGHT = 20;

    private int scrollOffset = 0;
    private static final int SCROLL_BAR_X = LIST_X + LIST_WIDTH - 12;

    private List<UUID> displayUuids = new ArrayList<>();
    private boolean requested = false;
    private int requestTick = 0;

    public BanListScreen() {
        super(Component.translatable("gui.pathsrole.ban_list.title"));
    }

    @Override
    protected void init() {
        super.init();
        refreshData();
    }

    private void refreshData() {
        BanListResponsePayload.updateCache(new HashMap<>(), new HashSet<>());
        ClientPlayNetworking.send(new BanListRequestPayload());
        requested = true;
        requestTick = 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int startX = (this.width - GUI_WIDTH) / 2;
        int startY = (this.height - GUI_HEIGHT) / 2;

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        renderMainBackground(guiGraphics, startX, startY);
        renderTopDecoration(guiGraphics, startX, startY);
        renderLeftCard(guiGraphics, startX, startY);
        renderPlayerList(guiGraphics, startX, startY, mouseX, mouseY);

        requestTick++;
        if (requested && requestTick > 15 && BanListResponsePayload.hasData()) {
            buildDisplayList();
            requested = false;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void buildDisplayList() {
        HashMap<UUID, String> names = BanListResponsePayload.getCachedPlayerNames();
        displayUuids = new ArrayList<>(names.keySet());
        int maxScroll = Math.max(0, displayUuids.size() - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    private void renderMainBackground(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFF5EFE0);
        guiGraphics.renderOutline(x, y, GUI_WIDTH, GUI_HEIGHT, 0xFF8B6914);
        renderCornerDecorations(guiGraphics, x, y);
    }

    private void renderCornerDecorations(GuiGraphics guiGraphics, int x, int y) {
        int cornerSize = 20;
        int thickness = 3;

        guiGraphics.fill(x, y, x + cornerSize, y + thickness, 0xFF8B6914);
        guiGraphics.fill(x, y, x + thickness, y + cornerSize, 0xFF8B6914);

        guiGraphics.fill(x + GUI_WIDTH - cornerSize, y, x + GUI_WIDTH, y + thickness, 0xFF8B6914);
        guiGraphics.fill(x + GUI_WIDTH - thickness, y, x + GUI_WIDTH, y + cornerSize, 0xFF8B6914);

        guiGraphics.fill(x, y + GUI_HEIGHT - thickness, x + cornerSize, y + GUI_HEIGHT, 0xFF8B6914);
        guiGraphics.fill(x, y + GUI_HEIGHT - cornerSize, x + thickness, y + GUI_HEIGHT, 0xFF8B6914);

        guiGraphics.fill(x + GUI_WIDTH - cornerSize, y + GUI_HEIGHT - thickness, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFF8B6914);
        guiGraphics.fill(x + GUI_WIDTH - thickness, y + GUI_HEIGHT - cornerSize, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFF8B6914);
    }

    private void renderTopDecoration(GuiGraphics guiGraphics, int x, int y) {
        String title = "神社管理界面";
        guiGraphics.drawCenteredString(this.font, title, x + GUI_WIDTH / 2, y + 18, 0xFF5D3A1A);

        renderRopeDecoration(guiGraphics, x + 100, y + 38, x + GUI_WIDTH - 100, y + 38);
        renderBowDecoration(guiGraphics, x + 15, y + 25);
    }

    private void renderRopeDecoration(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2) {
        guiGraphics.fill(x1, y1, x2, y1 + 3, 0xFFD4A84B);
        for (int i = x1; i < x2; i += 12) {
            guiGraphics.fill(i, y1 - 2, i + 6, y1 + 5, 0xFFD4A84B);
        }
        guiGraphics.fill(x1 + 50, y1 + 3, x1 + 56, y1 + 18, 0xFFD4A84B);
        guiGraphics.fill(x2 - 56, y1 + 3, x2 - 50, y1 + 18, 0xFFD4A84B);
    }

    private void renderBowDecoration(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 40, y + 35, 0xFFCC0000);
        guiGraphics.fill(x + 5, y + 5, x + 35, y + 30, 0xFFFF3333);
        guiGraphics.fill(x + 17, y - 5, x + 23, y + 40, 0xFFAA0000);
        guiGraphics.fill(x - 5, y + 12, x + 45, y + 18, 0xFFAA0000);
    }

    private void renderLeftCard(GuiGraphics guiGraphics, int x, int y) {
        int cx = x + LEFT_CARD_X;
        int cy = y + LEFT_CARD_Y;

        guiGraphics.fill(cx, cy, cx + LEFT_CARD_WIDTH, cy + LEFT_CARD_HEIGHT, 0xFF4A2C1A);
        guiGraphics.renderOutline(cx, cy, LEFT_CARD_WIDTH, LEFT_CARD_HEIGHT, 0xFFD4A84B);

        int innerPadding = 8;
        int redPanelX = cx + innerPadding;
        int redPanelY = cy + innerPadding + 25;
        int redPanelW = LEFT_CARD_WIDTH - innerPadding * 2;
        int redPanelH = LEFT_CARD_HEIGHT - innerPadding * 2 - 25;

        fillRoundedRect(guiGraphics, redPanelX, redPanelY, redPanelW, redPanelH, 8, 0xFFC41E1E);
        fillRoundedRect(guiGraphics, redPanelX + 4, redPanelY + 4, redPanelW - 8, redPanelH - 8, 6, 0xFFD9362F);



        renderCardCorners(guiGraphics, cx, cy, LEFT_CARD_WIDTH, LEFT_CARD_HEIGHT);
    }

    private void renderCardCorners(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        int cs = 16;
        guiGraphics.fill(x, y + cs, x + cs, y + cs + 8, 0xFFD4A84B);
        guiGraphics.fill(x + cs, y, x + cs + 8, y + cs, 0xFFD4A84B);

        guiGraphics.fill(x + w - cs, y + cs, x + w - cs, y + cs + 8, 0xFFD4A84B);
        guiGraphics.fill(x + w - cs - 8, y, x + w - cs, y + cs, 0xFFD4A84B);

        guiGraphics.fill(x, y + h - cs - 8, x + cs, y + h - cs, 0xFFD4A84B);
        guiGraphics.fill(x + cs, y + h - cs, x + cs + 8, y + h, 0xFFD4A84B);

        guiGraphics.fill(x + w - cs, y + h - cs - 8, x + w - cs, y + h - cs, 0xFFD4A84B);
        guiGraphics.fill(x + w - cs - 8, y + h - cs, x + w - cs, y + h, 0xFFD4A84B);
    }

    private void renderPlayerList(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int listX = x + LIST_X;
        int listY = y + LIST_Y;

        guiGraphics.renderOutline(listX, listY - 5, LIST_WIDTH, LIST_HEIGHT, 0xFFD4A84B);

        String headerTitle = "神社玩家管理";
        guiGraphics.drawString(this.font, headerTitle, listX + 10, listY - 28, 0xFF5D3A1A, false);

        guiGraphics.fill(listX + 5, listY + 28, listX + LIST_WIDTH - 5, listY + 30, 0xFFD4A84B);

        drawColumnHeaders(guiGraphics, listX, listY);

        if (!BanListResponsePayload.hasData() || displayUuids.isEmpty()) {
            String emptyMsg = requested ? "加载中..." : "暂无神社玩家";
            int msgWidth = this.font.width(emptyMsg);
            guiGraphics.drawString(this.font, emptyMsg,
                    listX + (LIST_WIDTH - msgWidth) / 2, listY + 100, 0xFF8B6914, false);
            renderScrollBar(guiGraphics, listX, listY);
            return;
        }

        HashSet<UUID> bannedPlayers = BanListResponsePayload.getCachedBannedPlayers();
        HashMap<UUID, String> playerNames = BanListResponsePayload.getCachedPlayerNames();

        int maxScroll = Math.max(0, displayUuids.size() - VISIBLE_ROWS);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        for (int i = 0; i < VISIBLE_ROWS && (i + scrollOffset) < displayUuids.size(); i++) {
            UUID uuid = displayUuids.get(i + scrollOffset);
            String name = playerNames.getOrDefault(uuid, "???");
            int rowY = listY + 30 + i * ROW_HEIGHT;
            boolean isBanned = bannedPlayers.contains(uuid);

            if (i + scrollOffset < displayUuids.size() - 1) {
                guiGraphics.fill(listX + 8, rowY + ROW_HEIGHT - 1,
                        listX + LIST_WIDTH - 8, rowY + ROW_HEIGHT, 0xFFE8DCC8);
            }

            guiGraphics.drawString(this.font, name, listX + 15, rowY + 7, 0xFF3D2914, false);

            int toggleX = listX + LIST_WIDTH - TOGGLE_BTN_WIDTH - 25;
            int toggleY = rowY + (ROW_HEIGHT - TOGGLE_BTN_HEIGHT) / 2;
            renderToggleSwitch(guiGraphics, toggleX, toggleY, mouseX, mouseY, isBanned);
        }

        renderScrollBar(guiGraphics, listX, listY);
    }

    private void drawColumnHeaders(GuiGraphics guiGraphics, int listX, int listY) {
        guiGraphics.drawString(this.font, "玩家名称", listX + 15, listY + 5, 0xFF5D3A1A, false);
        String banHeader = "是否拉黑";
        guiGraphics.drawString(this.font, banHeader,
                listX + LIST_WIDTH - TOGGLE_BTN_WIDTH - 40, listY + 5, 0xFF5D3A1A, false);
    }

    private void renderToggleSwitch(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, boolean active) {
        boolean hovered = mouseX >= x && mouseX <= x + TOGGLE_BTN_WIDTH
                && mouseY >= y && mouseY <= y + TOGGLE_BTN_HEIGHT;

        int bgColor = active ? (hovered ? 0xFFD9362F : 0xFFC41E1E) : (hovered ? 0xFF9E9E9E : 0xFF757575);
        guiGraphics.fill(x, y, x + TOGGLE_BTN_WIDTH, y + TOGGLE_BTN_HEIGHT, bgColor);
        guiGraphics.renderOutline(x, y, TOGGLE_BTN_WIDTH, TOGGLE_BTN_HEIGHT, active ? 0xFFAA0000 : 0xFF555555);

        String text = active ? "已拉黑" : "未拉黑";
        int textWidth = this.font.width(text);
        int textColor = active ? 0xFFFFFFFF : 0xFFE0E0E0;
        guiGraphics.drawString(this.font, text,
                x + (TOGGLE_BTN_WIDTH - textWidth) / 2,
                y + (TOGGLE_BTN_HEIGHT - 8) / 2, textColor, false);
    }

    private void renderScrollBar(GuiGraphics guiGraphics, int listX, int listY) {
        int barX = x() + SCROLL_BAR_X;
        int barY = listY + 32;
        int barHeight = VISIBLE_ROWS * ROW_HEIGHT;

        guiGraphics.fill(barX - 2, barY, barX + 6, barY + barHeight, 0xFFD4C4A0);

        if (!displayUuids.isEmpty()) {
            int totalRows = displayUuids.size();
            int thumbHeight = Math.max(20, barHeight * VISIBLE_ROWS / totalRows);
            int thumbY = barY + scrollOffset * barHeight / totalRows;
            guiGraphics.fill(barX - 4, thumbY, barX + 8, thumbY + thumbHeight, 0xFF8B6914);
            guiGraphics.renderOutline(barX - 4, thumbY, 12, thumbHeight, 0xFF5D3A1A);
        }
    }

    private int x() {
        return (this.width - GUI_WIDTH) / 2;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listX = x() + LIST_X;
        int listY = (this.height - GUI_HEIGHT) / 2 + LIST_Y;

        if (mouseX >= listX && mouseX <= listX + LIST_WIDTH
                && mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
            scrollOffset -= (int) scrollY;
            int maxScroll = Math.max(0, displayUuids.size() - VISIBLE_ROWS);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = (this.width - GUI_WIDTH) / 2;
        int startY = (this.height - GUI_HEIGHT) / 2;

        if (!BanListResponsePayload.hasData() || displayUuids.isEmpty()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int listX = startX + LIST_X;
        int listY = startY + LIST_Y;

        if (mouseX >= listX && mouseX <= listX + LIST_WIDTH
                && mouseY >= listY + 30 && mouseY <= listY + LIST_HEIGHT) {

            int relativeRow = (int) ((mouseY - listY - 30) / ROW_HEIGHT);
            int actualIndex = relativeRow + scrollOffset;

            if (relativeRow >= 0 && relativeRow < VISIBLE_ROWS
                    && actualIndex >= 0 && actualIndex < displayUuids.size()) {

                int toggleX = listX + LIST_WIDTH - TOGGLE_BTN_WIDTH - 25;
                int toggleY = listY + 30 + relativeRow * ROW_HEIGHT
                        + (ROW_HEIGHT - TOGGLE_BTN_HEIGHT) / 2;

                if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_BTN_WIDTH
                        && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_BTN_HEIGHT) {

                    UUID targetUuid = displayUuids.get(actualIndex);
                    ClientPlayNetworking.send(new BanTogglePayload(targetUuid));

                    HashSet<UUID> banned = BanListResponsePayload.getCachedBannedPlayers();
                    if (banned.contains(targetUuid)) {
                        banned.remove(targetUuid);
                    } else {
                        banned.add(targetUuid);
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void fillRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        guiGraphics.fill(x + radius, y, x + width - radius, y + height, color);
        guiGraphics.fill(x, y + radius, x + width, y + height - radius, color);
        guiGraphics.fill(x + radius, y + radius, x + width - radius, y + height - radius, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}