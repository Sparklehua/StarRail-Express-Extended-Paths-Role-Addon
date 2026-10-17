package org.agmas.pathsrole.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.game.roles.paths.abundance.pig.PigFormOffsets;
import org.agmas.pathsrole.mixin.client.QuadrupedModelAccessor;
import org.joml.Quaternionf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PigFormAdjustScreen extends Screen {

    private static final ResourceLocation PIG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");
    private static final ResourceLocation STEVE_SKIN =
            ResourceLocation.withDefaultNamespace("textures/entity/steve.png");

    private static final int PREVIEW_X = 20;
    private static final int PREVIEW_Y = 40;
    private static final int PREVIEW_W = 160;
    private static final int PREVIEW_H = 200;

    private static final float STEP = 0.5F;

    private final List<EditBox> fields = new ArrayList<>();
    private float previewRotY = 30.0F;
    private float previewRotX = 15.0F;
    private boolean dragging;
    private int dragStartX, dragStartY;
    private float dragStartRotY, dragStartRotX;
    private String statusMsg = "";
    private long statusMsgTime;

    private static PigModel pigModel;

    public PigFormAdjustScreen() {
        super(Component.literal("人头猪模型调整"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int panelX = PREVIEW_X + PREVIEW_W + 30;
        int y = 30;

        fields.clear();

        y = addSection("猪身体", panelX, y, false);
        y = addSection("玩家头", panelX, y, true);

        y += 10;

        addRenderableWidget(Button.builder(Component.literal("导出 JSON"), btn -> exportJson())
                .pos(panelX, y).size(100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("全部重置"), btn -> {
            PigFormOffsets.resetAll();
            syncFieldsFromOffsets();
        }).pos(panelX + 110, y).size(100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("完成"), btn -> onClose())
                .pos(panelX, y + 28).size(210, 20).build());

        syncFieldsFromOffsets();
    }

    private int addSection(String label, int x, int y, boolean isHead) {
        int ry = y;
        drawLabel(label, x, y);
        ry += 14;

        String[] axes = {"X", "Y", "Z"};
        for (int i = 0; i < 3; i++) {
            ry = addAxisRow(axes[i], x, ry, isHead, i);
        }

        ry += 4;
        String[] invLabels = {"反转X", "反转Y", "反转Z"};
        for (int i = 0; i < 3; i++) {
            final boolean hd = isHead;
            final int idx = i;
            addRenderableWidget(Button.builder(
                    Component.literal(invLabels[i] + ": " + (isHead ? getHeadInvert(i) : getBodyInvert(i))),
                    btn -> {
                        if (hd) toggleHeadInvert(idx); else toggleBodyInvert(idx);
                        btn.setMessage(Component.literal(invLabels[idx] + ": " +
                                (hd ? getHeadInvert(idx) : getBodyInvert(idx))));
                    })
                    .pos(x + i * 73, ry).size(68, 16).build());
        }
        ry += 20;

        return ry;
    }

    private int addAxisRow(String axis, int x, int y, boolean isHead, int idx) {
        String prefix = isHead ? "head" : "body";
        EditBox field = new EditBox(font, x + 18, y, 50, 14, Component.literal(prefix + axis));
        field.setValue(String.format("%.1f", isHead ? getHeadAxis(idx) : getBodyAxis(idx)));
        field.setFilter(s -> s.matches("-?\\d*\\.?\\d*"));
        field.setResponder(val -> {
            try {
                if (!val.isEmpty() && !val.equals("-") && !val.equals(".") && !val.equals("-.")) {
                    float v = Float.parseFloat(val);
                    if (isHead) setHeadAxis(idx, v); else setBodyAxis(idx, v);
                }
            } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(field);
        fields.add(field);

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            stepAxis(isHead, idx, STEP);
            syncField(field, isHead, idx);
        }).pos(x + 72, y).size(16, 14).build());

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            stepAxis(isHead, idx, -STEP);
            syncField(field, isHead, idx);
        }).pos(x + 92, y).size(16, 14).build());

        drawText(axis + ":", x, y + 2, 0xFFFFFF);

        return y + 18;
    }

    private void stepAxis(boolean isHead, int idx, float delta) {
        if (isHead) {
            switch (idx) {
                case 0 -> PigFormOffsets.headX += delta;
                case 1 -> PigFormOffsets.headY += delta;
                case 2 -> PigFormOffsets.headZ += delta;
            }
        } else {
            switch (idx) {
                case 0 -> PigFormOffsets.bodyX += delta;
                case 1 -> PigFormOffsets.bodyY += delta;
                case 2 -> PigFormOffsets.bodyZ += delta;
            }
        }
    }

    private void syncField(EditBox field, boolean isHead, int idx) {
        field.setValue(String.format("%.1f", isHead ? getHeadAxis(idx) : getBodyAxis(idx)));
    }

    private void syncFieldsFromOffsets() {
        int bodyIdx = 0, headIdx = 0;
        for (int i = 0; i < fields.size(); i++) {
            EditBox f = fields.get(i);
            if (i < 3) {
                f.setValue(String.format("%.1f", getBodyAxis(bodyIdx++)));
            } else {
                f.setValue(String.format("%.1f", getHeadAxis(headIdx++)));
            }
        }
    }

    private float getBodyAxis(int idx) {
        return switch (idx) {
            case 0 -> PigFormOffsets.bodyX;
            case 1 -> PigFormOffsets.bodyY;
            case 2 -> PigFormOffsets.bodyZ;
            default -> 0;
        };
    }

    private void setBodyAxis(int idx, float val) {
        switch (idx) {
            case 0 -> PigFormOffsets.bodyX = val;
            case 1 -> PigFormOffsets.bodyY = val;
            case 2 -> PigFormOffsets.bodyZ = val;
        }
    }

    private boolean getBodyInvert(int idx) {
        return switch (idx) {
            case 0 -> PigFormOffsets.invertBodyX;
            case 1 -> PigFormOffsets.invertBodyY;
            case 2 -> PigFormOffsets.invertBodyZ;
            default -> false;
        };
    }

    private void toggleBodyInvert(int idx) {
        switch (idx) {
            case 0 -> PigFormOffsets.invertBodyX = !PigFormOffsets.invertBodyX;
            case 1 -> PigFormOffsets.invertBodyY = !PigFormOffsets.invertBodyY;
            case 2 -> PigFormOffsets.invertBodyZ = !PigFormOffsets.invertBodyZ;
        }
    }

    private float getHeadAxis(int idx) {
        return switch (idx) {
            case 0 -> PigFormOffsets.headX;
            case 1 -> PigFormOffsets.headY;
            case 2 -> PigFormOffsets.headZ;
            default -> 0;
        };
    }

    private void setHeadAxis(int idx, float val) {
        switch (idx) {
            case 0 -> PigFormOffsets.headX = val;
            case 1 -> PigFormOffsets.headY = val;
            case 2 -> PigFormOffsets.headZ = val;
        }
    }

    private boolean getHeadInvert(int idx) {
        return switch (idx) {
            case 0 -> PigFormOffsets.invertHeadX;
            case 1 -> PigFormOffsets.invertHeadY;
            case 2 -> PigFormOffsets.invertHeadZ;
            default -> false;
        };
    }

    private void toggleHeadInvert(int idx) {
        switch (idx) {
            case 0 -> PigFormOffsets.invertHeadX = !PigFormOffsets.invertHeadX;
            case 1 -> PigFormOffsets.invertHeadY = !PigFormOffsets.invertHeadY;
            case 2 -> PigFormOffsets.invertHeadZ = !PigFormOffsets.invertHeadZ;
        }
    }

    private void drawLabel(String text, int x, int y) {
        drawText("§l" + text, x, y, 0xFFAA00);
    }

    private void drawText(String text, int x, int y, int color) {
        // will be rendered in the main render method
    }

    private void exportJson() {
        try {
            JsonObject root = new JsonObject();
            JsonObject body = new JsonObject();
            body.addProperty("x", PigFormOffsets.bodyX);
            body.addProperty("y", PigFormOffsets.bodyY);
            body.addProperty("z", PigFormOffsets.bodyZ);
            body.addProperty("invertX", PigFormOffsets.invertBodyX);
            body.addProperty("invertY", PigFormOffsets.invertBodyY);
            body.addProperty("invertZ", PigFormOffsets.invertBodyZ);
            root.add("body", body);

            JsonObject head = new JsonObject();
            head.addProperty("x", PigFormOffsets.headX);
            head.addProperty("y", PigFormOffsets.headY);
            head.addProperty("z", PigFormOffsets.headZ);
            head.addProperty("invertX", PigFormOffsets.invertHeadX);
            head.addProperty("invertY", PigFormOffsets.invertHeadY);
            head.addProperty("invertZ", PigFormOffsets.invertHeadZ);
            root.add("head", head);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Path configDir = Path.of("config", "pathsrole");
            Files.createDirectories(configDir);
            Path file = configDir.resolve("pigform_offsets.json");
            Files.writeString(file, gson.toJson(root));

            statusMsg = "已导出: " + file.toAbsolutePath();
            statusMsgTime = System.currentTimeMillis();
        } catch (IOException e) {
            statusMsg = "导出失败: " + e.getMessage();
            statusMsgTime = System.currentTimeMillis();
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx, mouseX, mouseY, delta);

        renderPreview(gfx, mouseX, mouseY);

        int panelX = PREVIEW_X + PREVIEW_W + 30;

        gfx.drawString(font, "§l§6人头猪模型调整工具", panelX, 10, 0xFFFFFF);
        gfx.drawString(font, "§7拖拽预览区旋转模型", PREVIEW_X, PREVIEW_Y + PREVIEW_H + 4, 0x888888);

        drawAllLabels(gfx, panelX);

        super.render(gfx, mouseX, mouseY, delta);

        if (!statusMsg.isEmpty() && System.currentTimeMillis() - statusMsgTime < 5000) {
            gfx.drawCenteredString(font, statusMsg, this.width / 2, this.height - 20, 0x55FF55);
        }
    }

    private void drawAllLabels(GuiGraphics gfx, int panelX) {
        int y = 30;
        y += 14;
        for (int i = 0; i < 3; i++) {
            gfx.drawString(font, (i == 0 ? "X" : i == 1 ? "Y" : "Z") + ":", panelX, y + 3, 0xFFFFFF);
            y += 18;
        }
        y += 8;
        y += 14;
        for (int i = 0; i < 3; i++) {
            gfx.drawString(font, (i == 0 ? "X" : i == 1 ? "Y" : "Z") + ":", panelX, y + 3, 0xFFFFFF);
            y += 18;
        }
    }

    private void renderPreview(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.fill(PREVIEW_X - 1, PREVIEW_Y - 1, PREVIEW_X + PREVIEW_W + 1, PREVIEW_Y + PREVIEW_H + 1, 0xFF333333);
        gfx.fill(PREVIEW_X, PREVIEW_Y, PREVIEW_X + PREVIEW_W, PREVIEW_Y + PREVIEW_H, 0xFF555555);

        gfx.pose().pushPose();
        gfx.pose().translate(PREVIEW_X + PREVIEW_W / 2.0, PREVIEW_Y + PREVIEW_H / 2.0 + 30, 100);
        gfx.pose().scale(1, 1, -1);
        gfx.pose().scale(40, 40, 40);

        gfx.pose().mulPose(new Quaternionf().rotateY((float) Math.toRadians(previewRotY)));
        gfx.pose().mulPose(new Quaternionf().rotateX((float) Math.toRadians(previewRotX)));

        gfx.pose().translate(
                PigFormOffsets.invertBodyX ? -PigFormOffsets.bodyX : PigFormOffsets.bodyX,
                PigFormOffsets.invertBodyY ? -PigFormOffsets.bodyY : PigFormOffsets.bodyY,
                PigFormOffsets.invertBodyZ ? -PigFormOffsets.bodyZ : PigFormOffsets.bodyZ
        );

        if (pigModel == null) {
            LayerDefinition layer = PigModel.createBodyLayer(CubeDeformation.NONE);
            pigModel = new PigModel(layer.bakeRoot());
        }
        pigModel.young = false;

        QuadrupedModelAccessor pigAccessor = (QuadrupedModelAccessor) (Object) pigModel;
        ModelPart pigHead = pigAccessor.getHead();
        pigHead.visible = false;

        PigModel pm = pigModel;
        VertexConsumer pigConsumer = gfx.bufferSource().getBuffer(pm.renderType(PIG_TEXTURE));
        pm.renderToBuffer(gfx.pose(), pigConsumer, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);

        pigHead.visible = true;

        float headAppliedX = pigHead.x + (PigFormOffsets.invertHeadX ? -PigFormOffsets.headX : PigFormOffsets.headX);
        float headAppliedY = pigHead.y + (PigFormOffsets.invertHeadY ? -PigFormOffsets.headY : PigFormOffsets.headY);
        float headAppliedZ = pigHead.z + (PigFormOffsets.invertHeadZ ? -PigFormOffsets.headZ : PigFormOffsets.headZ);

        float origHX = pigHead.x, origHY = pigHead.y, origHZ = pigHead.z;
        pigHead.x = headAppliedX;
        pigHead.y = headAppliedY;
        pigHead.z = headAppliedZ;

        VertexConsumer headConsumer = gfx.bufferSource().getBuffer(RenderType.entityTranslucent(STEVE_SKIN));
        pigHead.render(gfx.pose(), headConsumer, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);

        pigHead.x = origHX;
        pigHead.y = origHY;
        pigHead.z = origHZ;

        gfx.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInPreview(mouseX, mouseY) && button == 0) {
            dragging = true;
            dragStartX = (int) mouseX;
            dragStartY = (int) mouseY;
            dragStartRotY = previewRotY;
            dragStartRotX = previewRotX;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            previewRotY = dragStartRotY + ((float) mouseX - dragStartX) * 0.5F;
            previewRotX = dragStartRotX + ((float) mouseY - dragStartY) * 0.5F;
            previewRotX = Math.max(-90, Math.min(90, previewRotX));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isInPreview(double mx, double my) {
        return mx >= PREVIEW_X && mx <= PREVIEW_X + PREVIEW_W
                && my >= PREVIEW_Y && my <= PREVIEW_Y + PREVIEW_H;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}