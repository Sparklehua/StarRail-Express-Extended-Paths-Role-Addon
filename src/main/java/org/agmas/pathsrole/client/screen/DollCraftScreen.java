package org.agmas.pathsrole.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.agmas.pathsrole.client.renderer.SkinTextureManager;
import org.agmas.pathsrole.network.CraftDollPayload;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(value = EnvType.CLIENT)
public class DollCraftScreen extends Screen {

    private static final int PANEL_X = 10;
    private static final int PANEL_W = 320;
    private static final int RIGHT_X = PANEL_X + PANEL_W + 12;
    private static final int TOP = 36;

    private static final int BOX_W = 26;
    private static final int BTN = 12;
    private static final int ROW_H = 22;
    private static final int GAP = 4;

    private static final int TITLE_W = 50;
    private static final int AXIS_GROUP_W = BTN + BOX_W + BTN;
    private static final int AXIS_GAP = 6;
    private static final int AXIS_START_X = PANEL_X + TITLE_W;

    static final ResourceLocation STEVE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("pathsrole", "geo/steve.geo.json");

    private ResourceLocation previewTexture = STEVE;
    private boolean skinLoading = false;

    static final PreviewItem DUMMY = new PreviewItem();
    static final DollPreviewModel PREVIEW_MODEL = new DollPreviewModel();

    private static String persistedPlayerName = "";
    private static String persistedSize = "1.0";
    private static String persistedHeadRX = "0", persistedHeadRY = "0", persistedHeadRZ = "0";
    private static String persistedRaRX = "0", persistedRaRY = "0", persistedRaRZ = "0";
    private static String persistedLaRX = "0", persistedLaRY = "0", persistedLaRZ = "0";
    private static String persistedRlRX = "0", persistedRlRY = "0", persistedRlRZ = "0";
    private static String persistedRlPX = "0", persistedRlPY = "0", persistedRlPZ = "0";
    private static String persistedLlRX = "0", persistedLlRY = "0", persistedLlRZ = "0";
    private static String persistedLlPX = "0", persistedLlPY = "0", persistedLlPZ = "0";

    private EditBox nameInput;
    private EditBox sizeInput;

    private EditBox headRX, headRY, headRZ;
    private EditBox raRX, raRY, raRZ;
    private EditBox laRX, laRY, laRZ;
    private EditBox rlRX, rlRY, rlRZ;
    private EditBox rlPX, rlPY, rlPZ;
    private EditBox llRX, llRY, llRZ;
    private EditBox llPX, llPY, llPZ;

    private DollItemSelectScreen.ItemSelectionResult carriedItem = DollItemSelectScreen.ItemSelectionResult.EMPTY;
    private Button carryButton;

    private final List<Bubble> bubbles = new ArrayList<>();
    private final Random rnd = new Random();
    private float time = 0f;

    private float previewRotY = 180f;
    private float previewRotX = 10f;
    private boolean isDragging = false;

    public DollCraftScreen() {
        super(Component.translatable("screen.pathsrole.doll_craft"));
    }

    private void savePersistedData() {
        if (nameInput != null) persistedPlayerName = nameInput.getValue();
        if (sizeInput != null) persistedSize = sizeInput.getValue();
        if (headRX != null) persistedHeadRX = headRX.getValue();
        if (headRY != null) persistedHeadRY = headRY.getValue();
        if (headRZ != null) persistedHeadRZ = headRZ.getValue();
        if (raRX != null) persistedRaRX = raRX.getValue();
        if (raRY != null) persistedRaRY = raRY.getValue();
        if (raRZ != null) persistedRaRZ = raRZ.getValue();
        if (laRX != null) persistedLaRX = laRX.getValue();
        if (laRY != null) persistedLaRY = laRY.getValue();
        if (laRZ != null) persistedLaRZ = laRZ.getValue();
        if (rlRX != null) persistedRlRX = rlRX.getValue();
        if (rlRY != null) persistedRlRY = rlRY.getValue();
        if (rlRZ != null) persistedRlRZ = rlRZ.getValue();
        if (rlPX != null) persistedRlPX = rlPX.getValue();
        if (rlPY != null) persistedRlPY = rlPY.getValue();
        if (rlPZ != null) persistedRlPZ = rlPZ.getValue();
        if (llRX != null) persistedLlRX = llRX.getValue();
        if (llRY != null) persistedLlRY = llRY.getValue();
        if (llRZ != null) persistedLlRZ = llRZ.getValue();
        if (llPX != null) persistedLlPX = llPX.getValue();
        if (llPY != null) persistedLlPY = llPY.getValue();
        if (llPZ != null) persistedLlPZ = llPZ.getValue();
    }

    public static void clearPersistedData() {
        persistedPlayerName = "";
        persistedSize = "1.0";
        persistedHeadRX = "0"; persistedHeadRY = "0"; persistedHeadRZ = "0";
        persistedRaRX = "0"; persistedRaRY = "0"; persistedRaRZ = "0";
        persistedLaRX = "0"; persistedLaRY = "0"; persistedLaRZ = "0";
        persistedRlRX = "0"; persistedRlRY = "0"; persistedRlRZ = "0";
        persistedRlPX = "0"; persistedRlPY = "0"; persistedRlPZ = "0";
        persistedLlRX = "0"; persistedLlRY = "0"; persistedLlRZ = "0";
        persistedLlPX = "0"; persistedLlPY = "0"; persistedLlPZ = "0";
    }

    @Override
    protected void init() {
        forceScale4();
        String prevName = nameInput != null ? nameInput.getValue() : persistedPlayerName;
        String prevSize = sizeInput != null ? sizeInput.getValue() : persistedSize;
        String prevHeadRX = headRX != null ? headRX.getValue() : persistedHeadRX;
        String prevHeadRY = headRY != null ? headRY.getValue() : persistedHeadRY;
        String prevHeadRZ = headRZ != null ? headRZ.getValue() : persistedHeadRZ;
        String prevRaRX  = raRX  != null ? raRX.getValue()  : persistedRaRX;
        String prevRaRY  = raRY  != null ? raRY.getValue()  : persistedRaRY;
        String prevRaRZ  = raRZ  != null ? raRZ.getValue()  : persistedRaRZ;
        String prevLaRX  = laRX  != null ? laRX.getValue()  : persistedLaRX;
        String prevLaRY  = laRY  != null ? laRY.getValue()  : persistedLaRY;
        String prevLaRZ  = laRZ  != null ? laRZ.getValue()  : persistedLaRZ;
        String prevRlRX  = rlRX  != null ? rlRX.getValue()  : persistedRlRX;
        String prevRlRY  = rlRY  != null ? rlRY.getValue()  : persistedRlRY;
        String prevRlRZ  = rlRZ  != null ? rlRZ.getValue()  : persistedRlRZ;
        String prevRlPX  = rlPX  != null ? rlPX.getValue()  : persistedRlPX;
        String prevRlPY  = rlPY  != null ? rlPY.getValue()  : persistedRlPY;
        String prevRlPZ  = rlPZ  != null ? rlPZ.getValue()  : persistedRlPZ;
        String prevLlRX  = llRX  != null ? llRX.getValue()  : persistedLlRX;
        String prevLlRY  = llRY  != null ? llRY.getValue()  : persistedLlRY;
        String prevLlRZ  = llRZ  != null ? llRZ.getValue()  : persistedLlRZ;
        String prevLlPX  = llPX  != null ? llPX.getValue()  : persistedLlPX;
        String prevLlPY  = llPY  != null ? llPY.getValue()  : persistedLlPY;
        String prevLlPZ  = llPZ  != null ? llPZ.getValue()  : persistedLlPZ;

        super.init();
        initBubbles();

        int y = TOP;

        // 玩家名称行
        nameInput = makeEditBox(PANEL_X + 76, y, 100, "输入正版玩家名...");
        if (!prevName.isEmpty()) nameInput.setValue(prevName);
        addRenderableWidget(Button.builder(Component.literal("刷新"), b -> refreshSkin())
                .bounds(PANEL_X + 180, y, 38, 16).build());
        y += ROW_H + GAP + 8;

        // 大小比例行
        sizeInput = makeEditBox(PANEL_X + 76, y, 56, "1.0");
        sizeInput.setValue(prevSize);
        sizeInput.setFilter(s -> s.matches("[0-9.]*") && s.length() <= 5);
        y += ROW_H + GAP + 8;

        // 携带物品按钮
        carryButton = Button.builder(Component.literal("携带物品"), b -> openItemSelect())
                .bounds(PANEL_X + 8, y, PANEL_W - 16, 22).build();
        if (minecraft.player != null && minecraft.player.hasPermissions(2)) {
            addRenderableWidget(carryButton);
        }
        y += ROW_H + GAP + 10;

        // 头部旋转
        EditBox[] hr = makeAxisRow(y, 5f);
        headRX = hr[0]; headRY = hr[1]; headRZ = hr[2];
        headRX.setValue(prevHeadRX); headRY.setValue(prevHeadRY); headRZ.setValue(prevHeadRZ);
        y += ROW_H + GAP;

        // 右手
        EditBox[] rar = makeAxisRow(y, 5f);
        raRX = rar[0]; raRY = rar[1]; raRZ = rar[2];
        raRX.setValue(prevRaRX); raRY.setValue(prevRaRY); raRZ.setValue(prevRaRZ);
        y += ROW_H + GAP;

        // 左手
        EditBox[] lar = makeAxisRow(y, 5f);
        laRX = lar[0]; laRY = lar[1]; laRZ = lar[2];
        laRX.setValue(prevLaRX); laRY.setValue(prevLaRY); laRZ.setValue(prevLaRZ);
        y += ROW_H + GAP;

        // 右腿旋转
        EditBox[] rlr = makeAxisRow(y, 5f);
        rlRX = rlr[0]; rlRY = rlr[1]; rlRZ = rlr[2];
        rlRX.setValue(prevRlRX); rlRY.setValue(prevRlRY); rlRZ.setValue(prevRlRZ);
        y += ROW_H + GAP;
        // 右腿位置
        EditBox[] rlp = makeAxisRow(y, 0.1f);
        rlPX = rlp[0]; rlPY = rlp[1]; rlPZ = rlp[2];
        rlPX.setValue(prevRlPX); rlPY.setValue(prevRlPY); rlPZ.setValue(prevRlPZ);
        y += ROW_H + GAP;

        // 左腿旋转
        EditBox[] llr = makeAxisRow(y, 5f);
        llRX = llr[0]; llRY = llr[1]; llRZ = llr[2];
        llRX.setValue(prevLlRX); llRY.setValue(prevLlRY); llRZ.setValue(prevLlRZ);
        y += ROW_H + GAP;
        // 左腿位置
        EditBox[] llp = makeAxisRow(y, 0.1f);
        llPX = llp[0]; llPY = llp[1]; llPZ = llp[2];
        llPX.setValue(prevLlPX); llPY.setValue(prevLlPY); llPZ.setValue(prevLlPZ);
        y += ROW_H + GAP + 10;

        // 制作按钮
        addRenderableWidget(Button.builder(
                Component.translatable("screen.pathsrole.doll_craft.craft"),
                b -> onCraft())
                .bounds(PANEL_X + PANEL_W / 2 - 44, y, 88, 24).build());

        // 左下角清除数据按钮
        addRenderableWidget(Button.builder(
                Component.translatable("screen.pathsrole.doll_craft.clear_data"),
                b -> clearAllData())
                .bounds(PANEL_X + 4, y, 70, 24).build());

        // 返回和关闭按钮
        addRenderableWidget(Button.builder(Component.literal("←"), b -> onClose())
                .bounds(6, 4, 18, 18).build());
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> minecraft.setScreen(null))
                .bounds(width - 22, 4, 18, 18).build());
    }

    private void clearAllData() {
        clearPersistedData();
        if (nameInput != null) nameInput.setValue("");
        if (sizeInput != null) sizeInput.setValue("1.0");
        if (headRX != null) headRX.setValue("0");
        if (headRY != null) headRY.setValue("0");
        if (headRZ != null) headRZ.setValue("0");
        if (raRX != null) raRX.setValue("0");
        if (raRY != null) raRY.setValue("0");
        if (raRZ != null) raRZ.setValue("0");
        if (laRX != null) laRX.setValue("0");
        if (laRY != null) laRY.setValue("0");
        if (laRZ != null) laRZ.setValue("0");
        if (rlRX != null) rlRX.setValue("0");
        if (rlRY != null) rlRY.setValue("0");
        if (rlRZ != null) rlRZ.setValue("0");
        if (rlPX != null) rlPX.setValue("0");
        if (rlPY != null) rlPY.setValue("0");
        if (rlPZ != null) rlPZ.setValue("0");
        if (llRX != null) llRX.setValue("0");
        if (llRY != null) llRY.setValue("0");
        if (llRZ != null) llRZ.setValue("0");
        if (llPX != null) llPX.setValue("0");
        if (llPY != null) llPY.setValue("0");
        if (llPZ != null) llPZ.setValue("0");
        previewTexture = STEVE;
        carriedItem = DollItemSelectScreen.ItemSelectionResult.EMPTY;
        if (carryButton != null) {
            carryButton.setMessage(Component.literal("携带物品"));
        }
    }

    private void openItemSelect() {
        savePersistedData();
        PreviewState state = buildPreviewState();
        var screen = new DollItemSelectScreen(state, this, result -> {
            carriedItem = result;
            if (!result.isEmpty() && carryButton != null) {
                carryButton.setMessage(Component.literal(result.itemId().getPath()));
            }
            Minecraft.getInstance().setScreen(DollCraftScreen.this);
        });
        Minecraft.getInstance().setScreen(screen);
    }

    PreviewState buildPreviewState() {
        float r = (float) Math.PI / 180f;
        float[] bones = new float[] {
                getFloat(headRX, 0) * r, getFloat(headRY, 0) * r, getFloat(headRZ, 0) * r,
                getFloat(raRX, 0) * r, getFloat(raRY, 0) * r, getFloat(raRZ, 0) * r,
                getFloat(laRX, 0) * r, getFloat(laRY, 0) * r, getFloat(laRZ, 0) * r,
                getFloat(rlRX, 0) * r, getFloat(rlRY, 0) * r, getFloat(rlRZ, 0) * r,
                getFloat(rlPX, 0), getFloat(rlPY, 0), getFloat(rlPZ, 0),
                getFloat(llRX, 0) * r, getFloat(llRY, 0) * r, getFloat(llRZ, 0) * r,
                getFloat(llPX, 0), getFloat(llPY, 0), getFloat(llPZ, 0),
        };
        return new PreviewState(previewTexture, bones, previewRotY, previewRotX, clampSize());
    }

    void applyPreviewState(PreviewState state) {
        previewTexture = state.texture();
        previewRotY = state.modelRotY();
        previewRotX = state.modelRotX();

        float d = (float) (180f / Math.PI);
        float[] b = state.boneRotations();
        setBox(headRX, b[0] * d); setBox(headRY, b[1] * d); setBox(headRZ, b[2] * d);
        setBox(raRX, b[3] * d); setBox(raRY, b[4] * d); setBox(raRZ, b[5] * d);
        setBox(laRX, b[6] * d); setBox(laRY, b[7] * d); setBox(laRZ, b[8] * d);
        setBox(rlRX, b[9] * d); setBox(rlRY, b[10] * d); setBox(rlRZ, b[11] * d);
        if (b.length > 11) { setBox(rlPX, b[12]); setBox(rlPY, b[13]); setBox(rlPZ, b[14]); }
        if (b.length > 14) { setBox(llRX, b[15] * d); setBox(llRY, b[16] * d); setBox(llRZ, b[17] * d); }
        if (b.length > 17) { setBox(llPX, b[18]); setBox(llPY, b[19]); setBox(llPZ, b[20]); }
        sizeInput.setValue(fmt(state.scale()));
    }

    private void setBox(EditBox eb, float v) {
        eb.setValue(fmt(v));
    }

    private EditBox makeEditBox(int x, int y, int w, String hint) {
        EditBox eb = new EditBox(font, x, y, w, 16, Component.empty());
        eb.setMaxLength(16);
        eb.setHint(Component.literal(hint));
        eb.setBordered(true);
        addRenderableWidget(eb);
        return eb;
    }

    private EditBox[] makeAxisRow(int baseY, float step) {
        int x = AXIS_START_X;

        return new EditBox[] {
                makeAxis(x, baseY, step),
                makeAxis(x + AXIS_GROUP_W + AXIS_GAP, baseY, step),
                makeAxis(x + (AXIS_GROUP_W + AXIS_GAP) * 2, baseY, step)
        };
    }

    private EditBox makeAxis(int x, int y, float step) {
        addSmallButton(x, y, "-", () -> adjustBox(x, y, -step));
        EditBox box = makeValueBox(x + BTN, y);
        addSmallButton(x + BTN + BOX_W, y, "+", () -> adjustBox(x, y, step));
        return box;
    }

    private EditBox makeValueBox(int x, int y) {
        EditBox eb = new EditBox(font, x, y, BOX_W, 16, Component.empty());
        eb.setMaxLength(6);
        eb.setValue("0");
        eb.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?[0-9]*(\\.[0-9]*)?"));
        addRenderableWidget(eb);
        return eb;
    }

    private void addSmallButton(int x, int y, String text, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> action.run())
                .bounds(x, y + 1, BTN, BTN).build());
    }

    private void adjustBox(int x, int y, float step) {
        int ex = x + BTN;
        for (var w : children()) {
            if (w instanceof EditBox eb && Math.abs(eb.getX() - ex) < 3 && Math.abs(eb.getY() - y) < 3) {
                try {
                    float v = Float.parseFloat(eb.getValue().isEmpty() ? "0" : eb.getValue());
                    v = Math.round((v + step) * 100f) / 100f;
                    eb.setValue(fmt(v));
                } catch (NumberFormatException e) {
                    eb.setValue("0");
                }
                return;
            }
        }
    }

    static String fmt(float v) {
        return v == (int) v ? String.valueOf((int) v) : String.format("%.2f", v);
    }

    private void onCraft() {
        String name = nameInput.getValue().trim();
        if (name.isEmpty())
            return;
        savePersistedData();
        float r = (float) Math.PI / 180f;
        float[] boneData = new float[] {
                getFloat(headRX, 0) * r, getFloat(headRY, 0) * r, getFloat(headRZ, 0) * r,
                getFloat(raRX, 0) * r, getFloat(raRY, 0) * r, getFloat(raRZ, 0) * r,
                getFloat(laRX, 0) * r, getFloat(laRY, 0) * r, getFloat(laRZ, 0) * r,
                getFloat(rlRX, 0) * r, getFloat(rlRY, 0) * r, getFloat(rlRZ, 0) * r,
                getFloat(rlPX, 0), getFloat(rlPY, 0), getFloat(rlPZ, 0),
                getFloat(llRX, 0) * r, getFloat(llRY, 0) * r, getFloat(llRZ, 0) * r,
                getFloat(llPX, 0), getFloat(llPY, 0), getFloat(llPZ, 0),
                clampSize(),
        };
        String handId = carriedItem != null && !carriedItem.isEmpty() ? carriedItem.itemId().toString() : null;
        float[] handT = carriedItem != null && !carriedItem.isEmpty() ? carriedItem.toTransformArray() : null;

        ClientPlayNetworking.send(new CraftDollPayload(name, boneData, null, null, handId, handT));
        if (minecraft != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.5F));
        }
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void initBubbles() {
        bubbles.clear();
        for (int i = 0; i < 30; i++) {
            bubbles.add(new Bubble(rnd.nextFloat() * width, rnd.nextFloat() * height,
                    6f + rnd.nextFloat() * 16f, 0.15f + rnd.nextFloat() * 0.4f));
        }
    }

    @Override
    public void tick() {
        time += 0.016f;
        for (Bubble b : bubbles) {
            b.y -= b.speed;
            if (b.y < -30) {
                b.y = height + 20;
                b.x = rnd.nextFloat() * width;
            }
        }
        super.tick();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        double es = extraScale();
        forceScale4();
        int adjMX = (int) (mx / es);
        int adjMY = (int) (my / es);
        g.pose().pushPose();
        g.pose().scale((float) es, (float) es, 1.0f);
        renderBg(g);
        renderBubbles(g);
        super.render(g, adjMX, adjMY, d);
        renderUI(g);
        renderPreview(g);
        g.pose().popPose();
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

        int rx = RIGHT_X;
        int rw = Math.min(480, width - RIGHT_X - 10);
        int ry = TOP - 4;
        int rh = height - TOP - 6;

        g.fill(PANEL_X, 20, PANEL_X + PANEL_W, 21, FastColor.ARGB32.color(60, 80, 150, 200));
        g.fill(rx, ry, rx + rw, ry + 1, FastColor.ARGB32.color(50, 70, 140, 180));
        g.fill(rx, ry + rh, rx + rw, ry + rh + 1, FastColor.ARGB32.color(50, 70, 140, 180));
        g.fill(rx, ry, rx + 1, ry + rh, FastColor.ARGB32.color(50, 70, 140, 180));
        g.fill(rx + rw, ry, rx + rw + 1, ry + rh, FastColor.ARGB32.color(50, 70, 140, 180));

        RenderSystem.disableBlend();
    }

    private void renderBubbles(GuiGraphics g) {
        for (Bubble b : bubbles) {
            int a = 18 + (int) ((0.2f + 0.25f * Math.sin(time * 2.5f + b.x * 0.7f)) * 30);
            g.fill((int) (b.x - b.size / 2), (int) (b.y - b.size / 2),
                    (int) (b.x + b.size / 2), (int) (b.y + b.size / 2),
                    FastColor.ARGB32.color(a, 130, 180, 230));
        }
    }

    private void renderUI(GuiGraphics g) {
        g.drawString(font, Component.translatable("screen.pathsrole.doll_craft"),
                PANEL_X + PANEL_W / 2 - font.width(Component.translatable("screen.pathsrole.doll_craft")) / 2,
                10, 0xFFC8E0FF);

        int y = TOP;

        // 玩家名称
        g.fill(PANEL_X + 4, y - 2, PANEL_X + PANEL_W - 4, y + ROW_H + 2,
                FastColor.ARGB32.color(25, 30, 45, 80));
        g.drawString(font, "玩家名称", PANEL_X + 10, y + 4, 0xFF90B8DD);
        y += ROW_H + GAP + 8;

        // 大小比例
        g.fill(PANEL_X + 4, y - 2, PANEL_X + PANEL_W - 4, y + ROW_H + 2,
                FastColor.ARGB32.color(25, 30, 45, 80));
        g.drawString(font, "大小比例", PANEL_X + 10, y + 4, 0xFF90B8DD);
        g.drawString(font, "0.1 ~ 3.0", PANEL_X + 142, y + 4, 0xFF5577AA);
        y += ROW_H + GAP + 8;

        // 携带物品按钮区（不绘制文字行）
        y += ROW_H + GAP + 10;

        // 各骨骼区域
        y = boneSection(g, "头部旋转", y);
        y = boneSection(g, "右  手", y);
        y = boneSection(g, "左  手", y);
        y = boneSection(g, "右  腿", y);
        y = legPosSection(g, y);
        y = boneSection(g, "左  腿", y);
        y = legPosSection(g, y);
    }

    private int legPosSection(GuiGraphics g, int y) {
        g.drawString(font, "位置", PANEL_X + 28, y + 4, 0xFFCCAA66);
        drawAxisLabels(g, y);
        y += ROW_H + GAP;
        return y;
    }

    private int boneSection(GuiGraphics g, String title, int y) {
        g.fill(PANEL_X + 6, y - 3, PANEL_X + PANEL_W - 6, y - 2,
                FastColor.ARGB32.color(70, 150, 130, 50));
        g.drawString(font, title, PANEL_X + 10, y + 4, 0xFFFFCC66);
        drawAxisLabels(g, y);
        y += ROW_H + GAP;
        return y;
    }

    private void drawAxisLabels(GuiGraphics g, int y) {
        int x0 = AXIS_START_X + BTN + BOX_W / 2 - 3;
        int x1 = AXIS_START_X + AXIS_GROUP_W + AXIS_GAP + BTN + BOX_W / 2 - 3;
        int x2 = AXIS_START_X + (AXIS_GROUP_W + AXIS_GAP) * 2 + BTN + BOX_W / 2 - 3;
        g.drawString(font, "X", x0, y - 8, 0xFF88AACC);
        g.drawString(font, "Y", x1, y - 8, 0xFF88AACC);
        g.drawString(font, "Z", x2, y - 8, 0xFF88AACC);
    }

    void renderPreview(GuiGraphics g) {
        int rx = RIGHT_X;
        int rw = Math.min(480, width - RIGHT_X - 10);
        int ry = TOP - 4;
        int rh = height - TOP - 6;

        int cx = rx + rw / 2;
        int cy = ry + rh / 2 + 30;
        int sz = (int) (clampSize() * 76);

        String name = nameInput.getValue().trim();
        ResourceLocation cached = getTextureForName(name);
        if (cached != null) {
            previewTexture = cached;
        }
        PREVIEW_MODEL.setCurrentTexture(previewTexture);
        applyPosesToModel();

        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 200);
        ps.scale(sz, -sz, sz);
        ps.mulPose(new Quaternionf().rotateY((float) Math.toRadians(previewRotY)));
        ps.mulPose(new Quaternionf().rotateX((float) Math.toRadians(previewRotX)));

        try {
            RenderSystem.enableDepthTest();
            MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
            RenderType renderType = RenderType.entityCutoutNoCull(previewTexture);
            DollPreviewRenderer.INSTANCE.defaultRender(ps, DUMMY, buf, renderType, null,
                    0, 0, 15728880);

            renderCarriedItemOnPreview(ps, buf);

            buf.endBatch();
        } catch (Exception ignored) {
        } finally {
            RenderSystem.disableDepthTest();
        }

        ps.popPose();

        g.drawCenteredString(font, "拖动鼠标旋转 | 滚轮缩放", rx + rw / 2, TOP + 2, 0xFF5577AA);
    }

    void renderCarriedItemOnPreview(PoseStack ps, MultiBufferSource.BufferSource buf) {
        if (carriedItem == null || carriedItem.isEmpty()) return;
        Item item = BuiltInRegistries.ITEM.get(carriedItem.itemId());
        if (item == null || item == Items.AIR) return;

        ItemStack stack = new ItemStack(item);
        float[] t = carriedItem.toTransformArray();
        ps.pushPose();
        ps.translate(t[3], t[4], t[5]);
        ps.scale(0.45f, 0.45f, 0.45f);
        ps.mulPose(new Quaternionf().rotateX((float) Math.toRadians(t[0])));
        ps.mulPose(new Quaternionf().rotateY((float) Math.toRadians(t[1])));
        ps.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(t[2])));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                15728880, 0, ps, buf, null, 0);
        ps.popPose();
    }

    private ResourceLocation getTextureForName(String name) {
        if (name == null || name.isEmpty())
            return STEVE;
        ResourceLocation cached = SkinTextureManager.getCachedTexture(name);
        if (cached != null)
            return cached;
        return STEVE;
    }

    private void refreshSkin() {
        String name = nameInput.getValue().trim();
        if (name.isEmpty()) {
            previewTexture = STEVE;
            return;
        }
        SkinTextureManager.clearTriedSkin(name);
        skinLoading = true;
        SkinTextureManager.loadSkinForPlayer(name).thenAccept(tex -> {
            Minecraft.getInstance().execute(() -> {
                skinLoading = false;
                if (tex != null) {
                    previewTexture = tex;
                }
            });
        });
    }

    private void applyPosesToModel() {
        BakedGeoModel model;
        try {
            model = PREVIEW_MODEL.getBakedModel(MODEL);
        } catch (Exception e) {
            return;
        }
        if (model == null) return;
        float r = (float) Math.PI / 180f;

        setBone(model, "Head",
                getFloat(headRX, 0) * r, getFloat(headRY, 0) * r, getFloat(headRZ, 0) * r);
        setBone(model, "RightArm",
                getFloat(raRX, 0) * r, getFloat(raRY, 0) * r, getFloat(raRZ, 0) * r);
        setBone(model, "LeftArm",
                getFloat(laRX, 0) * r, getFloat(laRY, 0) * r, getFloat(laRZ, 0) * r);
        setBone(model, "RightLeg",
                getFloat(rlRX, 0) * r, getFloat(rlRY, 0) * r, getFloat(rlRZ, 0) * r);
        setBonePosition(model, "RightLeg",
                getFloat(rlPX, 0), getFloat(rlPY, 0), getFloat(rlPZ, 0));
        setBone(model, "LeftLeg",
                getFloat(llRX, 0) * r, getFloat(llRY, 0) * r, getFloat(llRZ, 0) * r);
        setBonePosition(model, "LeftLeg",
                getFloat(llPX, 0), getFloat(llPY, 0), getFloat(llPZ, 0));
    }

    private static void setBone(BakedGeoModel model, String name,
                                float rx, float ry, float rz) {
        model.getBone(name).ifPresent(b -> {
            b.setRotX(rx);
            b.setRotY(ry);
            b.setRotZ(rz);
        });
    }

    private static void setBonePosition(BakedGeoModel model, String name,
                                         float px, float py, float pz) {
        model.getBone(name).ifPresent(b -> {
            b.setPosX(px);
            b.setPosY(py);
            b.setPosZ(pz);
        });
    }

    private float clampSize() {
        return Mth.clamp(getFloat(sizeInput, 1f), 0.1f, 3.0f);
    }

    private static float getFloat(EditBox eb, float def) {
        try {
            float v = Float.parseFloat(eb.getValue().isEmpty() ? String.valueOf(def) : eb.getValue());
            if (Float.isNaN(v) || Float.isInfinite(v)) return def;
            return v;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float d) {
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mod) {
        for (var w : children())
            if (w instanceof EditBox eb && eb.isFocused())
                return eb.keyPressed(kc, sc, mod);
        if (kc == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(kc, sc, mod);
    }

    @Override
    public boolean charTyped(char c, int mod) {
        for (var w : children())
            if (w instanceof EditBox eb && eb.isFocused())
                return eb.charTyped(c, mod);
        return super.charTyped(c, mod);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (var w : children())
            if (w instanceof EditBox eb && eb.mouseClicked(mx, my, btn)) {
                setFocused(eb);
                return true;
            }
        if (btn == 0 && isInPreview(mx, my)) {
            isDragging = true;
            return true;
        }
        return super.mouseClicked(mx, my, btn);
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
    public boolean mouseScrolled(double mx, double my, double hd, double vd) {
        if (isInPreview(mx, my)) {
            float v = clampSize() - (float) vd * 0.1f;
            v = Mth.clamp(v, 0.1f, 3.0f);
            sizeInput.setValue(fmt(v));
            return true;
        }
        return super.mouseScrolled(mx, my, hd, vd);
    }

    private boolean isInPreview(double mx, double my) {
        int rw = Math.min(480, width - RIGHT_X - 10);
        int rh = height - TOP - 6;
        return mx >= RIGHT_X && mx <= RIGHT_X + rw && my >= TOP - 4 && my <= TOP - 4 + rh;
    }

    private void forceScale4() {
        var window = Minecraft.getInstance().getWindow();
        this.width = Math.max(window.getWidth() / 4, 1);
        this.height = Math.max(window.getHeight() / 4, 1);
    }

    @Override
    public void repositionElements() {
        super.repositionElements();
        forceScale4();
    }

    private double extraScale() {
        return 4.0 / Minecraft.getInstance().getWindow().getGuiScale();
    }

    @Override
    public void onClose() {
        savePersistedData();
        if (minecraft != null)
            minecraft.setScreen(new AsIWriteScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class Bubble {
        float x, y, size, speed;

        Bubble(float x, float y, float s, float sp) {
            this.x = x;
            this.y = y;
            this.size = s;
            this.speed = sp;
        }
    }

    static class PreviewItem implements GeoItem {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    static class DollPreviewModel extends GeoModel<PreviewItem> {
        private ResourceLocation currentTexture = STEVE;

        public void setCurrentTexture(ResourceLocation tex) {
            this.currentTexture = tex != null ? tex : STEVE;
        }

        @Override
        public ResourceLocation getModelResource(PreviewItem obj) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(PreviewItem obj) {
            return currentTexture;
        }

        @Override
        public ResourceLocation getAnimationResource(PreviewItem obj) {
            return null;
        }
    }

    static class DollPreviewRenderer implements GeoRenderer<PreviewItem> {
        static final DollPreviewRenderer INSTANCE = new DollPreviewRenderer();

        @Override
        public GeoModel<PreviewItem> getGeoModel() {
            return PREVIEW_MODEL;
        }

        @Override
        public PreviewItem getAnimatable() {
            return DUMMY;
        }

        @Override
        public ResourceLocation getTextureLocation(PreviewItem animatable) {
            return PREVIEW_MODEL.getTextureResource(animatable);
        }

        @Override
        public void fireCompileRenderLayersEvent() {}

        @Override
        public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
            return true;
        }

        @Override
        public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {}

        @Override
        public void updateAnimatedTextureFrame(PreviewItem animatable) {}
    }
}