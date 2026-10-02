package org.agmas.pathsrole.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.*;
import java.util.function.Consumer;

public class DollItemSelectScreen extends Screen {

    private static final int ROWS = 7;
    private static final int GRID_X = 10;
    private static final int GRID_Y = 96;
    private static final int TAB_H = 22;
    private static final int RIGHT_W = 220;
    private static final int BOX_W = 28;
    private static final int BTN = 12;

    private int COLS;
    private int SLOT;

    private static final int TAB_COLOR = FastColor.ARGB32.color(35, 45, 60, 90);
    private static final int TAB_ACTIVE = FastColor.ARGB32.color(60, 80, 130, 200);

    private final Consumer<ItemSelectionResult> onConfirm;
    private final PreviewState previewState;
    private final Screen parentScreen;

    private EditBox searchBox;
    private final Map<String, List<ItemStack>> categorizedItems = new LinkedHashMap<>();
    private List<ItemStack> filteredItems = new ArrayList<>();
    private int scrollOffset = 0;
    private Item selectedItem = Items.AIR;
    private float itemRotX, itemRotY, itemRotZ;
    private float itemPosX, itemPosY, itemPosZ;
    private EditBox irx, iry, irz;
    private EditBox ipx, ipy, ipz;
    private float previewRotY = 150f;
    private float previewRotX = 10f;
    private boolean isDragging = false;
    private String activeCategory = null;
    private int tabScroll = 0;

    public DollItemSelectScreen(PreviewState state, Screen parent, Consumer<ItemSelectionResult> onConfirm) {
        super(Component.literal("携带物品"));
        this.previewState = state != null ? state : PreviewState.createDefault();
        this.parentScreen = parent;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();

        // 动态计算网格布局，完全填满左侧到预览分界线
        int gridRight = width - RIGHT_W - 10;
        int availableWidth = gridRight - GRID_X;

        // 目标列数：根据屏幕宽度动态调整，确保填满
        COLS = Math.max(8, Math.min(16, availableWidth / 24));
        SLOT = (availableWidth - (COLS - 1) * 4) / COLS;  // 槽位大小 + 4px 间距
        SLOT = Math.max(20, Math.min(28, SLOT));  // 限制在合理范围

        loadAllItems();

        searchBox = new EditBox(font, GRID_X, 46, gridRight - GRID_X - 160, 18, Component.literal(""));
        searchBox.setHint(Component.literal("搜索物品..."));
        searchBox.setMaxLength(50);
        searchBox.setResponder(s -> {
            activeCategory = null;
            applyFilter();
        });
        addRenderableWidget(searchBox);

        // ---- 右侧控件：严格对齐 ----
        int rx = width - RIGHT_W;
        int groupW = BTN + BOX_W + BTN;
        int gap = 8;
        int labelW = 28;
        int x1 = rx + 10 + labelW;
        int x2 = x1 + groupW + gap;
        int x3 = x2 + groupW + gap;
        int ctrlY = height - 72;

        irx = makeEditBoxWithButtons(x1, ctrlY, "0", 5f);
        iry = makeEditBoxWithButtons(x2, ctrlY, "0", 5f);
        irz = makeEditBoxWithButtons(x3, ctrlY, "0", 5f);
        ctrlY += 28;

        ipx = makeEditBoxWithButtons(x1, ctrlY, "0", 0.1f);
        ipy = makeEditBoxWithButtons(x2, ctrlY, "0", 0.1f);
        ipz = makeEditBoxWithButtons(x3, ctrlY, "0", 0.1f);

        addRenderableWidget(Button.builder(Component.literal("确认"), b -> confirm())
                .bounds(rx + 38, height - 28, 66, 20).build());
        addRenderableWidget(Button.builder(Component.literal("消除"), b -> clear())
                .bounds(rx + 116, height - 28, 66, 20).build());
    }

    private void confirm() {
        if (selectedItem != Items.AIR) {
            onConfirm.accept(new ItemSelectionResult(
                    BuiltInRegistries.ITEM.getKey(selectedItem),
                    itemRotX, itemRotY, itemRotZ,
                    itemPosX, itemPosY, itemPosZ));
        }
    }

    private void clear() {
        onConfirm.accept(ItemSelectionResult.EMPTY);
    }

    private EditBox makeEditBox(int x, int y, String val) {
        EditBox eb = new EditBox(font, x, y, BOX_W, 16, Component.empty());
        eb.setMaxLength(6);
        eb.setValue(val);
        eb.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?[0-9]*(\\.[0-9]*)?"));
        addRenderableWidget(eb);
        return eb;
    }

    private EditBox makeEditBoxWithButtons(int x, int y, String val, float step) {
        addRenderableWidget(Button.builder(Component.literal("-"), b -> adjustEditBox(x + BTN, y, -step))
                .bounds(x, y, BTN, BTN).build());
        EditBox eb = makeEditBox(x + BTN, y, val);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> adjustEditBox(x + BTN, y, step))
                .bounds(x + BTN + BOX_W, y, BTN, BTN).build());
        return eb;
    }

    private void adjustEditBox(int boxX, int boxY, float step) {
        for (var w : children()) {
            if (w instanceof EditBox eb && Math.abs(eb.getX() - boxX) < 3 && Math.abs(eb.getY() - boxY) < 3) {
                try {
                    float v = Float.parseFloat(eb.getValue().isEmpty() ? "0" : eb.getValue());
                    v = Math.round((v + step) * 100f) / 100f;
                    eb.setValue(v == (int) v ? String.valueOf((int) v) : String.format("%.2f", v));
                } catch (NumberFormatException e) {
                    eb.setValue("0");
                }
                return;
            }
        }
    }

    private void loadAllItems() {
        categorizedItems.clear();

        Map<String, List<ItemStack>> tabItems = new LinkedHashMap<>();

        try {
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                if (tab.getType() == CreativeModeTab.Type.CATEGORY) {
                    String tabName = tab.getDisplayName().getString();
                    List<ItemStack> items = new ArrayList<>();
                    try {
                        for (ItemStack displayItem : tab.getDisplayItems()) {
                            if (!displayItem.isEmpty()) {
                                items.add(displayItem.copy());
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    if (!items.isEmpty()) {
                        tabItems.put(tabName, items);
                    }
                }
            }
        } catch (Exception e) {
            org.agmas.pathsrole.PathsRoleMod.LOGGER.warn("[DollItemSelect] 创造标签加载失败，回退到全部物品: {}", e.toString());
        }

        if (tabItems.isEmpty()) {
            List<ItemStack> fallback = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != Items.AIR) {
                    fallback.add(new ItemStack(item));
                }
            }
            tabItems.put("全部物品", fallback);
        }

        List<ItemStack> all = new ArrayList<>();
        for (List<ItemStack> items : tabItems.values()) {
            all.addAll(items);
        }
        categorizedItems.put("全部", all);
        categorizedItems.putAll(tabItems);

        filteredItems = new ArrayList<>(all);
    }

    private void applyFilter() {
        List<ItemStack> source;
        if (activeCategory != null && categorizedItems.containsKey(activeCategory)) {
            source = categorizedItems.get(activeCategory);
        } else {
            source = categorizedItems.getOrDefault("全部", new ArrayList<>());
        }

        String lower = searchBox.getValue().toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            filteredItems = new ArrayList<>(source);
        } else {
            filteredItems = new ArrayList<>();
            for (ItemStack stack : source) {
                String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
                if (name.contains(lower)) {
                    filteredItems.add(stack);
                }
            }
        }
        scrollOffset = 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (parentScreen != null) {
                minecraft.setScreen(parentScreen);
            } else {
                minecraft.setScreen(null);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (my >= 70 && my < 70 + TAB_H) {
                handleTabClick(mx);
                return true;
            }
            int gridRight = width - RIGHT_W - 4;
            if (mx > GRID_X && mx < gridRight && my > GRID_Y && my < GRID_Y + ROWS * SLOT) {
                int col = (int) ((mx - GRID_X) / SLOT);
                int row = (int) ((my - GRID_Y) / SLOT);
                int idx = (scrollOffset + row) * COLS + col;
                if (col < COLS && idx >= 0 && idx < filteredItems.size()) {
                    selectedItem = filteredItems.get(idx).getItem();
                }
                return true;
            }
            if (mx >= width - RIGHT_W && my >= 50 && my <= height - 120) {
                isDragging = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void handleTabClick(double mx) {
        int tabY = 70;
        String[] categories = categorizedItems.keySet().toArray(new String[0]);
        int startIdx = tabScroll;
        int maxVisible = (width - RIGHT_W - 10 - GRID_X) / 52;
        int tx = GRID_X;

        for (int i = startIdx; i < Math.min(categories.length, startIdx + maxVisible); i++) {
            int tw = font.width(categories[i]) + 16;
            tw = Math.max(tw, 48);
            if (mx >= tx && mx < tx + tw) {
                if (categories[i].equals(activeCategory)) {
                    activeCategory = null;
                } else {
                    activeCategory = categories[i];
                }
                applyFilter();
                return;
            }
            tx += tw + 2;
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (isDragging && btn == 0) {
            previewRotY += (float) dx * 0.5f;
            previewRotX += (float) dy * 0.5f;
            previewRotX = Mth.clamp(previewRotX, -90f, 90f);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        isDragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (my >= GRID_Y && my < GRID_Y + ROWS * SLOT && mx < width - RIGHT_W) {
            int maxScroll = Math.max(0, (filteredItems.size() + COLS - 1) / COLS - ROWS);
            scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        if (my >= 70 && my < 70 + TAB_H) {
            String[] categories = categorizedItems.keySet().toArray(new String[0]);
            int maxVisible = (width - RIGHT_W - 10 - GRID_X) / 52;
            tabScroll = Mth.clamp(tabScroll - (int) Math.signum(scrollY), 0,
                    Math.max(0, categories.length - maxVisible));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public void tick() {
        readFloats();
        super.tick();
    }

    private void readFloats() {
        itemRotX = getF(irx);
        itemRotY = getF(iry);
        itemRotZ = getF(irz);
        itemPosX = getF(ipx);
        itemPosY = getF(ipy);
        itemPosZ = getF(ipz);
    }

    private float getF(EditBox eb) {
        try { return Float.parseFloat(eb.getValue().isEmpty() ? "0" : eb.getValue()); }
        catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        renderBg(g);
        super.render(g, mx, my, d);
        renderTabs(g, mx, my);
        renderGrid(g, mx, my);
        renderRightPanel(g);
        renderDollPreview(g, mx, my);
    }

    private void renderBg(GuiGraphics g) {
        Matrix4f m = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(m, 0, height, 0).setColor(FastColor.ARGB32.color(255, 12, 18, 38));
        buf.addVertex(m, width, height, 0).setColor(FastColor.ARGB32.color(255, 16, 24, 48));
        buf.addVertex(m, width, 0, 0).setColor(FastColor.ARGB32.color(255, 22, 32, 62));
        buf.addVertex(m, 0, 0, 0).setColor(FastColor.ARGB32.color(255, 10, 14, 30));
        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void renderTabs(GuiGraphics g, int mx, int my) {
        int tabY = 70;
        int gridRight = width - RIGHT_W - 4;

        g.fill(GRID_X, tabY, gridRight, tabY + TAB_H, FastColor.ARGB32.color(25, 30, 45, 100));

        String[] categories = categorizedItems.keySet().toArray(new String[0]);
        int maxVisible = (gridRight - GRID_X) / 52;
        int startIdx = tabScroll;
        int tx = GRID_X;

        for (int i = startIdx; i < Math.min(categories.length, startIdx + maxVisible); i++) {
            String cat = categories[i];
            int tw = font.width(cat) + 16;
            tw = Math.max(tw, 48);
            if (tx + tw > gridRight) break;

            boolean isActive = cat.equals(activeCategory) || (activeCategory == null && cat.equals("全部"));
            int bg = isActive ? TAB_ACTIVE : TAB_COLOR;
            if (!isActive && mx >= tx && mx < tx + tw && my >= tabY && my < tabY + TAB_H) {
                bg = FastColor.ARGB32.color(50, 65, 90, 140);
            }

            g.fill(tx, tabY, tx + tw, tabY + TAB_H, bg);
            g.drawString(font, cat, tx + 8, tabY + 6, isActive ? 0xFFFFEEAA : 0xFF8899BB);
            tx += tw + 2;
        }

        if (tabScroll > 0) {
            g.drawString(font, "<", GRID_X - 8, tabY + 6, 0xFF8899BB);
        }
    }

    private void renderGrid(GuiGraphics g, int mx, int my) {
        int gridRight = width - RIGHT_W - 4;
        int totalRows = Math.min(ROWS,
                Math.max(0, (filteredItems.size() + COLS - 1) / COLS - scrollOffset));

        for (int row = 0; row < totalRows; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = (scrollOffset + row) * COLS + col;
                if (idx >= filteredItems.size()) break;
                ItemStack stack = filteredItems.get(idx);
                int sx = GRID_X + col * SLOT + 3;
                int sy = GRID_Y + row * SLOT + 3;

                boolean hovered = mx >= sx - 2 && mx < sx + 16 && my >= sy - 2 && my < sy + 16;

                int bg;
                if (stack.getItem() == selectedItem) {
                    bg = FastColor.ARGB32.color(100, 120, 200, 255);
                } else if (hovered) {
                    bg = FastColor.ARGB32.color(60, 65, 80, 120);
                } else {
                    bg = FastColor.ARGB32.color(30, 35, 45, 60);
                }
                g.fill(sx - 2, sy - 2, sx + 16, sy + 16, bg);
                if (stack.getItem() == selectedItem) {
                    g.fill(sx - 3, sy - 3, sx + 17, sy - 2, FastColor.ARGB32.color(140, 180, 255, 220));
                    g.fill(sx - 3, sy + 16, sx + 17, sy + 17, FastColor.ARGB32.color(140, 180, 255, 220));
                    g.fill(sx - 3, sy - 3, sx - 2, sy + 17, FastColor.ARGB32.color(140, 180, 255, 220));
                    g.fill(sx + 16, sy - 3, sx + 17, sy + 17, FastColor.ARGB32.color(140, 180, 255, 220));
                }

                g.renderItem(stack, sx - 1, sy - 1);
                g.renderItemDecorations(font, stack, sx - 1, sy - 1);
            }
        }

        g.drawString(font, "携带物品", GRID_X + 4, 8, 0xFFC8E0FF);

        if (selectedItem != Items.AIR) {
            String name = new ItemStack(selectedItem).getHoverName().getString();
            g.drawString(font, "已选: " + name, GRID_X + 4, GRID_Y + totalRows * SLOT + 8, 0xFFFFCC66);
        }

        int maxScroll = Math.max(0, (filteredItems.size() + COLS - 1) / COLS - ROWS);
        if (maxScroll > 0) {
            int sbH = Math.max(20, ROWS * SLOT * ROWS / ((filteredItems.size() + COLS - 1) / COLS));
            int sbY = GRID_Y + (int) ((float) scrollOffset / maxScroll * (ROWS * SLOT - sbH));
            g.fill(gridRight + 2, sbY, gridRight + 6, sbY + sbH,
                    FastColor.ARGB32.color(80, 120, 160, 180));
            g.fill(gridRight + 1, GRID_Y, gridRight + 7, GRID_Y + ROWS * SLOT,
                    FastColor.ARGB32.color(25, 30, 45, 80));
        }
    }

    private void renderRightPanel(GuiGraphics g) {
        int rx = width - RIGHT_W;

        g.fill(rx, 0, width, height, FastColor.ARGB32.color(18, 14, 28, 52));
        g.fill(rx, 0, rx + 1, height, FastColor.ARGB32.color(45, 55, 85, 130));
        g.drawCenteredString(font, "预览", rx + RIGHT_W / 2, 12, 0xFFC0D8F0);

        int rotY = height - 72;
        int posY = rotY + 28;
        int divY = rotY - 5;

        g.fill(rx + 8, divY, width - 8, divY + 2, FastColor.ARGB32.color(60, 90, 150, 120));

        g.drawString(font, "旋转", rx + 10, rotY + 3, 0xFF8899BB);
        g.drawString(font, "位置", rx + 10, posY + 3, 0xFF8899BB);
    }

    void renderDollPreview(GuiGraphics g, int mx, int my) {
        int rx = width - RIGHT_W;
        int cx = rx + RIGHT_W / 2;
        int cy = 60 + 100;
        int sz = (int) (previewState.scale() * 76);

        DollCraftScreen.PREVIEW_MODEL.setCurrentTexture(previewState.texture());
        applyBonePoses();

        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 200);
        ps.scale(sz, -sz, sz);
        ps.mulPose(new Quaternionf().rotateY((float) Math.toRadians(previewRotY)));
        ps.mulPose(new Quaternionf().rotateX((float) Math.toRadians(previewRotX)));

        try {
            RenderSystem.enableDepthTest();
            MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
            RenderType renderType = RenderType.entityCutoutNoCull(previewState.texture());
            DollCraftScreen.DollPreviewRenderer.INSTANCE.defaultRender(
                    ps, DollCraftScreen.DUMMY, buf, renderType, null, 0, 0, 15728880);

            if (selectedItem != Items.AIR) {
                renderItemOnRightHand(ps, buf);
            }

            buf.endBatch();
        } catch (Exception ignored) {
        } finally {
            RenderSystem.disableDepthTest();
        }

        ps.popPose();

        g.drawCenteredString(font, "拖动旋转", rx + RIGHT_W / 2, cy + 100, 0xFF5577AA);
    }

    private void applyBonePoses() {
        var model = DollCraftScreen.PREVIEW_MODEL.getBakedModel(DollCraftScreen.MODEL);
        if (model == null) return;
        float[] b = previewState.boneRotations();
        if (b.length > 2) model.getBone("Head").ifPresent(bn -> { bn.setRotX(b[0]); bn.setRotY(b[1]); bn.setRotZ(b[2]); });
        if (b.length > 5) model.getBone("RightArm").ifPresent(bn -> { bn.setRotX(b[3]); bn.setRotY(b[4]); bn.setRotZ(b[5]); });
        if (b.length > 8) model.getBone("LeftArm").ifPresent(bn -> { bn.setRotX(b[6]); bn.setRotY(b[7]); bn.setRotZ(b[8]); });
        if (b.length > 11) model.getBone("RightLeg").ifPresent(bn -> { bn.setRotX(b[9]); bn.setRotY(b[10]); bn.setRotZ(b[11]); });
        if (b.length > 14) model.getBone("LeftLeg").ifPresent(bn -> { bn.setRotX(b[12]); bn.setRotY(b[13]); bn.setRotZ(b[14]); });
    }

    private void renderItemOnRightHand(PoseStack ps, MultiBufferSource.BufferSource buf) {
        Item item = BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(selectedItem));
        if (item == null || item == Items.AIR) return;

        ItemStack stack = new ItemStack(item);
        ps.pushPose();
        ps.translate(2.0 + itemPosX, 2.2 + itemPosY, itemPosZ);
        ps.scale(0.6f, 0.6f, 0.6f);
        ps.mulPose(new Quaternionf().rotateX((float) Math.toRadians(itemRotX)));
        ps.mulPose(new Quaternionf().rotateY((float) Math.toRadians(itemRotY)));
        ps.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(itemRotZ)));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                15728880, 0, ps, buf, null, 0);
        ps.popPose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float d) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public record ItemSelectionResult(ResourceLocation itemId,
                                       float rotX, float rotY, float rotZ,
                                       float posX, float posY, float posZ) {
        public static final ItemSelectionResult EMPTY = new ItemSelectionResult(
                ResourceLocation.withDefaultNamespace("air"), 0, 0, 0, 0, 0, 0);

        public boolean isEmpty() {
            return itemId.getPath().equals("air");
        }

        public float[] toTransformArray() {
            return new float[] { rotX, rotY, rotZ, posX, posY, posZ };
        }
    }
}