package org.agmas.pathsrole.client.screen;

import io.wifi.starrailexpress.util.ShopEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.agmas.pathsrole.init.ModItems;
import org.agmas.pathsrole.network.ShrinePurchasePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ShrineShopScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger("ShrineShop");
    
    private static final ResourceLocation BG_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("pathsrole", "textures/gui/shrine_shop/shrine_shop_bg.png");
    private static final ResourceLocation CONFIRM_BUTTON = 
        ResourceLocation.fromNamespaceAndPath("pathsrole", "textures/gui/shrine_shop/confirm_button.png");
    
    private static final int GUI_WIDTH = 380;
    private static final int GUI_HEIGHT = 254;
    
    private static final int CARD_WIDTH = 48;
    private static final int CARD_HEIGHT = 58;
    private static final int CARD_GAP_X = 12;
    private static final int CARD_GAP_Y = 14;
    
    private static final int CARDS_PER_ROW = 5;
    private static final int CARD_START_X = 47;
    private static final int CARD_START_Y = 79;
    
    private static final int CONFIRM_BTN_X = 295;
    private static final int CONFIRM_BTN_Y = 216;
    private static final int CONFIRM_BTN_WIDTH = 75;
    private static final int CONFIRM_BTN_HEIGHT = 28;
    
    private static final int TOGGLE_BTN_X = 210;
    private static final int TOGGLE_BTN_Y = 216;
    private static final int TOGGLE_BTN_WIDTH = 78;
    private static final int TOGGLE_BTN_HEIGHT = 22;
    
    private static final ShrineShopType[] TOGGLE_ORDER = {
        ShrineShopType.KILLER,
        ShrineShopType.NEUTRAL_KILLER,
        ShrineShopType.INNOCENT,
        ShrineShopType.SPECIAL_NEUTRAL
    };
    
    private static final int ITEMS_PER_PAGE = 10;
    
    private List<ShopEntry> shopEntries = new ArrayList<>();
    private int selectedIndex = -1;
    private final ShrineShopType initialType;
    private ShrineShopType currentType;
    private final boolean isReimu;
    private int currentPage = 0;
    private int totalPages = 0;
    
    /** 各阵营剩余购买次数（由服务器同步） */
    public static int KILLER_REMAINING = 3;
    public static int NEUTRAL_KILLER_REMAINING = 3;
    public static int SPECIAL_NEUTRAL_REMAINING = 3;
    public static int INNOCENT_REMAINING = 3;
    
    private static final Set<Item> REIMU_PURCHASABLE = Set.of(
        ModItems.BROOM
    );
    
    /** 安全获取字体，避免空指针崩溃 */
    @Nullable
    private net.minecraft.client.gui.Font safeFont() {
        return this.font;
    }
    
    /** 安全获取 Minecraft 实例，避免空指针崩溃 */
    @Nullable
    private Minecraft safeMinecraft() {
        return this.minecraft;
    }
    
    public ShrineShopScreen(ShrineShopType type, boolean isReimu) {
        super(Component.translatable("gui.pathsrole.shrine_shop.title"));
        this.initialType = type;
        this.currentType = type;
        this.isReimu = isReimu;
        this.loadShopEntries();
    }
    
    private void loadShopEntries() {
        try {
            this.shopEntries = ShrineFactionShopHandler.getEntries(this.currentType);
            if (this.shopEntries == null) {
                this.shopEntries = new ArrayList<>();
            }
        } catch (Exception e) {
            LOGGER.error("[神社商店] 加载商品列表失败", e);
            this.shopEntries = new ArrayList<>();
        }
        this.selectedIndex = -1;
        this.currentPage = 0;
        this.totalPages = Math.max(1, (int) Math.ceil((double) this.shopEntries.size() / ITEMS_PER_PAGE));
    }
    
    private void cycleShopType() {
        int currentIdx = -1;
        for (int i = 0; i < TOGGLE_ORDER.length; i++) {
            if (TOGGLE_ORDER[i] == this.currentType) {
                currentIdx = i;
                break;
            }
        }
        int nextIdx = (currentIdx + 1) % TOGGLE_ORDER.length;
        this.currentType = TOGGLE_ORDER[nextIdx];
        this.loadShopEntries();
    }
    
    @Override
    protected void init() {
        super.init();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 空值防护：确保关键对象存在，防止服务器/客户端崩溃
        Minecraft mc = safeMinecraft();
        net.minecraft.client.gui.Font safeFont = safeFont();
        
        int startX = (this.width - GUI_WIDTH) / 2;
        int startY = (this.height - GUI_HEIGHT) / 2;
        
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        try {
            guiGraphics.blit(BG_TEXTURE, startX, startY, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } catch (Exception e) {
            LOGGER.error("[神社商店] 绘制背景失败", e);
            guiGraphics.fill(startX, startY, startX + GUI_WIDTH, startY + GUI_HEIGHT, 0xFF8B4513);
        }
        
        if (this.shopEntries != null) {
            int startIndex = currentPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, shopEntries.size());
            for (int i = startIndex; i < endIndex; i++) {
                int cardIndex = i - startIndex;
                renderCard(guiGraphics, i, startX + CARD_START_X + (cardIndex % CARDS_PER_ROW) * (CARD_WIDTH + CARD_GAP_X),
                           startY + CARD_START_Y + (cardIndex / CARDS_PER_ROW) * (CARD_HEIGHT + CARD_GAP_Y));
            }
        }
        
        renderPageButtons(guiGraphics, startX, startY, mouseX, mouseY);
        
        if (this.isReimu) {
            renderToggleButton(guiGraphics, startX + TOGGLE_BTN_X, startY + TOGGLE_BTN_Y, mouseX, mouseY);
        }
        
        if (this.isReimu && safeFont != null) {
            String reimuHint = "\u00a7e\u00a7l\u5deb\u5973\u5546\u5e97\u6a21\u5f0f";
            int hintWidth = safeFont.width(reimuHint);
            guiGraphics.drawString(safeFont, reimuHint,
                startX + (GUI_WIDTH - hintWidth) / 2, startY + 28, 0xFFFFFF, true);
        }
        
        renderConfirmButton(guiGraphics, startX, startY, mouseX, mouseY);
        
        renderRemainingCount(guiGraphics, startX, startY);
        
        renderCardTooltip(guiGraphics, startX, startY, mouseX, mouseY);
    }
    
    private void renderRemainingCount(GuiGraphics guiGraphics, int startX, int startY) {
        net.minecraft.client.gui.Font safeFont = safeFont();
        if (safeFont == null) return;
        
        int remaining = getCurrentRemaining();
        String text = "\u00a7e该阵营剩余购买次数：\u00a76" + remaining + "\u00a7e/\u00a763";
        
        // 定位在最左下角卡片（第二行第一列）的正下方
        int cardBottomY = startY + CARD_START_Y + 2 * CARD_HEIGHT + CARD_GAP_Y;
        int textX = startX + CARD_START_X;
        int textY = cardBottomY + 3;
        
        guiGraphics.drawString(safeFont, text, textX, textY, 0xFFFFAA00, true);
    }
    
    private int getCurrentRemaining() {
        switch (currentType) {
            case KILLER: return KILLER_REMAINING;
            case NEUTRAL_KILLER: return NEUTRAL_KILLER_REMAINING;
            case SPECIAL_NEUTRAL: return SPECIAL_NEUTRAL_REMAINING;
            case INNOCENT: return INNOCENT_REMAINING;
            default: return 3;
        }
    }
    
    private void renderCardTooltip(GuiGraphics guiGraphics, int startX, int startY, int mouseX, int mouseY) {
        Minecraft mc = safeMinecraft();
        net.minecraft.client.gui.Font safeFont = safeFont();
        if (mc == null || safeFont == null) return;
        if (this.shopEntries == null || this.shopEntries.isEmpty()) return;
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, shopEntries.size());
        int visibleCount = endIndex - startIndex;
        
        for (int i = 0; i < visibleCount; i++) {
            int actualIndex = startIndex + i;
            int cardX = startX + CARD_START_X + (i % CARDS_PER_ROW) * (CARD_WIDTH + CARD_GAP_X);
            int cardY = startY + CARD_START_Y + (i / CARDS_PER_ROW) * (CARD_HEIGHT + CARD_GAP_Y);
            
            if (mouseX >= cardX && mouseX < cardX + CARD_WIDTH &&
                mouseY >= cardY && mouseY < cardY + CARD_HEIGHT) {
                
                ShopEntry entry = this.shopEntries.get(actualIndex);
                if (entry == null || entry.stack() == null || entry.stack().isEmpty()) return;
                
                // 获取物品的完整 tooltip（包括自定义介绍）
                ItemStack stack = entry.stack();
                List<Component> tooltip = Screen.getTooltipFromItem(mc, stack);
                
                // 添加价格信息
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("gui.pathsrole.shrine_shop.price",
                    entry.price()).withStyle(ChatFormatting.GOLD));
                
                guiGraphics.renderTooltip(safeFont, tooltip, java.util.Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }
    
    private void renderToggleButton(GuiGraphics guiGraphics, int btnX, int btnY, int mouseX, int mouseY) {
        net.minecraft.client.gui.Font safeFont = safeFont();
        if (safeFont == null) return;
        
        boolean hovered = mouseX >= btnX && mouseX < btnX + TOGGLE_BTN_WIDTH 
                       && mouseY >= btnY && mouseY < btnY + TOGGLE_BTN_HEIGHT;
        
        int fillColor = hovered ? 0xFFFFCC00 : 0xFFFFDD00;
        guiGraphics.fill(btnX, btnY, btnX + TOGGLE_BTN_WIDTH, btnY + TOGGLE_BTN_HEIGHT, fillColor);
        guiGraphics.renderOutline(btnX, btnY, TOGGLE_BTN_WIDTH, TOGGLE_BTN_HEIGHT, 0xFFAAAA00);
        
        String label = getToggleLabel();
        int labelWidth = safeFont.width(label);
        guiGraphics.drawString(safeFont, label,
            btnX + (TOGGLE_BTN_WIDTH - labelWidth) / 2,
            btnY + (TOGGLE_BTN_HEIGHT - 8) / 2,
            0x000000, false);
    }
    
    private String getToggleLabel() {
        switch (this.currentType) {
            case KILLER: return "\u6740\u624b\u5546\u5e97";
            case NEUTRAL_KILLER: return "\u504f\u6740\u5546\u5e97";
            case INNOCENT: return "\u65e0\u8f9c\u5546\u5e97";
            case SPECIAL_NEUTRAL: return "\u7279\u6b8a\u5546\u5e97";
            default: return "\u5207\u6362";
        }
    }
    
    private void renderPageButtons(GuiGraphics guiGraphics, int startX, int startY, int mouseX, int mouseY) {
        net.minecraft.client.gui.Font safeFont = safeFont();
        if (safeFont == null) return;
        if (totalPages <= 1) return;
        
        int btnSize = 16;
        int btnCenterY = startY + GUI_HEIGHT / 2 - btnSize / 2;
        
        // 左翻页按钮 "<"
        if (currentPage > 0) {
            int btnX = startX + 8;
            int btnY = btnCenterY;
            boolean hovered = mouseX >= btnX && mouseX < btnX + btnSize && mouseY >= btnY && mouseY < btnY + btnSize;
            int color = hovered ? 0xFFFFDD00 : 0xFFD4A84B;
            guiGraphics.fill(btnX, btnY, btnX + btnSize, btnY + btnSize, color);
            guiGraphics.renderOutline(btnX, btnY, btnSize, btnSize, 0xFFAA8800);
            guiGraphics.drawString(safeFont, "\u25C0", btnX + 3, btnY + 3, 0x000000, false);
        }
        
        // 右翻页按钮 ">"
        if (currentPage < totalPages - 1) {
            int btnX = startX + GUI_WIDTH - 8 - btnSize;
            int btnY = btnCenterY;
            boolean hovered = mouseX >= btnX && mouseX < btnX + btnSize && mouseY >= btnY && mouseY < btnY + btnSize;
            int color = hovered ? 0xFFFFDD00 : 0xFFD4A84B;
            guiGraphics.fill(btnX, btnY, btnX + btnSize, btnY + btnSize, color);
            guiGraphics.renderOutline(btnX, btnY, btnSize, btnSize, 0xFFAA8800);
            guiGraphics.drawString(safeFont, "\u25B6", btnX + 3, btnY + 3, 0x000000, false);
        }
        
        // 页码显示
        String pageText = (currentPage + 1) + " / " + totalPages;
        int textWidth = safeFont.width(pageText);
        guiGraphics.drawString(safeFont, pageText, startX + (GUI_WIDTH - textWidth) / 2, startY + GUI_HEIGHT - 22, 0xFFFFAA00, true);
    }
    
    private void renderConfirmButton(GuiGraphics guiGraphics, int startX, int startY, int mouseX, int mouseY) {
        net.minecraft.client.gui.Font safeFont = safeFont();
        if (safeFont == null) return;
        
        int btnX = startX + CONFIRM_BTN_X;
        int btnY = startY + CONFIRM_BTN_Y;
        
        boolean hovered = mouseX >= btnX && mouseX < btnX + CONFIRM_BTN_WIDTH
                       && mouseY >= btnY && mouseY < btnY + CONFIRM_BTN_HEIGHT;
        boolean canConfirm = this.shopEntries != null
                          && this.selectedIndex >= 0 && this.selectedIndex < this.shopEntries.size()
                          && canBuyEntry(this.shopEntries.get(this.selectedIndex));
        
        int fillColor = canConfirm ? (hovered ? 0xFFE6B800 : 0xFFFFCC00) : 0xFF888888;
        guiGraphics.fill(btnX, btnY, btnX + CONFIRM_BTN_WIDTH, btnY + CONFIRM_BTN_HEIGHT, fillColor);
        guiGraphics.renderOutline(btnX, btnY, CONFIRM_BTN_WIDTH, CONFIRM_BTN_HEIGHT, 0xFFAA8800);
        
        String label = "\u786e\u8ba4\u8d2d\u4e70";
        int labelWidth = safeFont.width(label);
        int textColor = canConfirm ? 0x000000 : 0x444444;
        guiGraphics.drawString(safeFont, label,
            btnX + (CONFIRM_BTN_WIDTH - labelWidth) / 2,
            btnY + (CONFIRM_BTN_HEIGHT - 8) / 2,
            textColor, false);
    }
    
    private void renderCard(GuiGraphics guiGraphics, int index, int x, int y) {
        net.minecraft.client.gui.Font safeFont = safeFont();
        if (safeFont == null) return;
        if (index < 0 || index >= this.shopEntries.size()) {
            return;
        }
        
        ShopEntry entry = this.shopEntries.get(index);
        if (entry == null) {
            return;
        }
        
        boolean canBuy = canBuyEntry(entry);
        boolean isSelected = (index == this.selectedIndex);
        boolean isHovered = isMouseOverCard(x, y);
        
        if (isSelected) {
            guiGraphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0x80FFFF99);
        } else if (isHovered) {
            guiGraphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0x40FFFF99);
        }
        
        guiGraphics.renderOutline(x, y, CARD_WIDTH, CARD_HEIGHT, 0xFF4A2800);
        
        ItemStack stack = entry.stack();
        if (stack != null && !stack.isEmpty()) {
            guiGraphics.renderItem(stack, x + (CARD_WIDTH - 16) / 2, y + 10);
        }
        
        String priceText = String.valueOf(entry.price());
        int textWidth = safeFont.width(priceText);
        int color = canBuy ? 0xFFFFAA : 0x666666;
        guiGraphics.drawString(safeFont, priceText, 
                              x + (CARD_WIDTH - textWidth) / 2, 
                              y + CARD_HEIGHT - 14, color, true);
    }
    
    private boolean isMouseOverCard(int cardX, int cardY) {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.mouseHandler == null || mc.getWindow() == null) {
            return false;
        }
        double mx = mc.mouseHandler.xpos() * (double)this.width / (double)mc.getWindow().getGuiScaledWidth();
        double my = mc.mouseHandler.ypos() * (double)this.height / (double)mc.getWindow().getGuiScaledHeight();
        return mx >= cardX && mx < cardX + CARD_WIDTH && my >= cardY && my < cardY + CARD_HEIGHT;
    }
    
    private boolean canBuyEntry(ShopEntry entry) {
        if (entry == null || this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        if (this.isReimu) {
            if (entry.stack() == null || !REIMU_PURCHASABLE.contains(entry.stack().getItem())) {
                return false;
            }
        }
        try {
            return entry.canBuy(this.minecraft.player);
        } catch (Exception e) {
            LOGGER.error("[神社商店] 检查 canBuy 失败", e);
            return false;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = (this.width - GUI_WIDTH) / 2;
        int startY = (this.height - GUI_HEIGHT) / 2;
        
        // 左翻页按钮
        if (totalPages > 1 && currentPage > 0) {
            int btnSize = 16;
            int btnX = startX + 8;
            int btnY = startY + GUI_HEIGHT / 2 - btnSize / 2;
            if (mouseX >= btnX && mouseX < btnX + btnSize && mouseY >= btnY && mouseY < btnY + btnSize) {
                currentPage--;
                if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
                    this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        
        // 右翻页按钮
        if (totalPages > 1 && currentPage < totalPages - 1) {
            int btnSize = 16;
            int btnX = startX + GUI_WIDTH - 8 - btnSize;
            int btnY = startY + GUI_HEIGHT / 2 - btnSize / 2;
            if (mouseX >= btnX && mouseX < btnX + btnSize && mouseY >= btnY && mouseY < btnY + btnSize) {
                currentPage++;
                if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
                    this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        
        if (this.isReimu) {
            int toggleX = startX + TOGGLE_BTN_X;
            int toggleY = startY + TOGGLE_BTN_Y;
            if (mouseX >= toggleX && mouseX < toggleX + TOGGLE_BTN_WIDTH
                && mouseY >= toggleY && mouseY < toggleY + TOGGLE_BTN_HEIGHT) {
                this.cycleShopType();
                return true;
            }
        }
        
        int confirmX = startX + CONFIRM_BTN_X;
        int confirmY = startY + CONFIRM_BTN_Y;
        if (mouseX >= confirmX && mouseX < confirmX + CONFIRM_BTN_WIDTH
            && mouseY >= confirmY && mouseY < confirmY + CONFIRM_BTN_HEIGHT) {
            if (this.shopEntries != null
                && this.selectedIndex >= 0 && this.selectedIndex < this.shopEntries.size()
                && canBuyEntry(this.shopEntries.get(this.selectedIndex))) {
                this.onConfirmPurchase();
                if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
                    this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
            }
            return true;
        }
        
        if (this.shopEntries == null || this.shopEntries.isEmpty()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, shopEntries.size());
        int visibleCount = endIndex - startIndex;
        
        for (int i = 0; i < visibleCount; i++) {
            int actualIndex = startIndex + i;
            int cardX = startX + CARD_START_X + (i % CARDS_PER_ROW) * (CARD_WIDTH + CARD_GAP_X);
            int cardY = startY + CARD_START_Y + (i / CARDS_PER_ROW) * (CARD_HEIGHT + CARD_GAP_Y);
            
            if (mouseX >= cardX && mouseX < cardX + CARD_WIDTH &&
                mouseY >= cardY && mouseY < cardY + CARD_HEIGHT) {
                if (canBuyEntry(shopEntries.get(actualIndex))) {
                    this.selectedIndex = actualIndex;
                    if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
                        this.minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                }
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void onConfirmPurchase() {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().player == null) {
            LOGGER.warn("[神社商店] 尝试购买时玩家为空");
            return;
        }
        
        if (this.shopEntries == null || this.shopEntries.isEmpty()) {
            LOGGER.warn("[神社商店] 商品列表为空");
            return;
        }
        
        if (this.selectedIndex < 0 || this.selectedIndex >= this.shopEntries.size()) {
            LOGGER.warn("[神社商店] 无效的选中索引: {}", this.selectedIndex);
            return;
        }
        
        ShopEntry entry = this.shopEntries.get(this.selectedIndex);
        if (entry == null) {
            LOGGER.warn("[神社商店] 选中的商品为空");
            return;
        }
        
        if (!canBuyEntry(entry)) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("message.pathsrole.shrine_shop.cannot_buy")
                        .withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        
        try {
            ClientPlayNetworking.send(new ShrinePurchasePayload(entry.stack(), entry.price(), this.currentType));
        } catch (Exception e) {
            LOGGER.error("[神社商店] 发送购买请求失败", e);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("message.pathsrole.shrine_shop.purchase_failed")
                        .withStyle(ChatFormatting.RED), true);
            }
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}