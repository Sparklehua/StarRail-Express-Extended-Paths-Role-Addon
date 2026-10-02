package org.agmas.pathsrole.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.agmas.pathsrole.network.BeggingResponsePayload;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class BeggingScreen extends Screen {

    private final UUID beggarUuid;
    private final String beggarName;

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 140;

    private EditBox amountField;
    private boolean responded = false;

    public BeggingScreen(UUID beggarUuid, String beggarName) {
        super(Component.literal("Begging"));
        this.beggarUuid = beggarUuid;
        this.beggarName = beggarName;
    }

    @Override
    protected void init() {
        super.init();
        int startX = (this.width - GUI_WIDTH) / 2;
        int startY = (this.height - GUI_HEIGHT) / 2;

        this.amountField = new EditBox(
                this.font,
                startX + 53, startY + 55,
                150, 18,
                Component.literal("")
        );
        this.amountField.setValue("30");
        this.amountField.setMaxLength(2);
        this.amountField.setFilter(s -> {
            if (s.isEmpty()) return true;
            try {
                int val = Integer.parseInt(s);
                return val >= 1 && val <= 50;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        this.addRenderableWidget(this.amountField);
        this.setInitialFocus(this.amountField);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int startX = (this.width - GUI_WIDTH) / 2;
        int startY = (this.height - GUI_HEIGHT) / 2;

        guiGraphics.fill(startX, startY, startX + GUI_WIDTH, startY + GUI_HEIGHT, 0xCC333333);
        guiGraphics.renderOutline(startX, startY, GUI_WIDTH, GUI_HEIGHT, 0xFF888888);

        if (this.font != null) {
            String line1 = this.beggarName + " 贫穷地向您乞讨...";
            int line1Width = this.font.width(line1);
            guiGraphics.drawString(this.font, line1,
                    startX + (GUI_WIDTH - line1Width) / 2,
                    startY + 18, 0xFFFFAA00, false);

            String line2 = "输入施舍金额 (20-50):";
            int line2Width = this.font.width(line2);
            guiGraphics.drawString(this.font, line2,
                    startX + (GUI_WIDTH - line2Width) / 2,
                    startY + 40, 0xFFAAAAAA, false);

            // ---- 施舍按钮 ----
            int btnDonateX = startX + 38;
            int btnDonateY = startY + 90;
            int btnWidth = 80;
            int btnHeight = 20;

            boolean hoverDonate = mouseX >= btnDonateX && mouseX < btnDonateX + btnWidth
                    && mouseY >= btnDonateY && mouseY < btnDonateY + btnHeight;
            int donateColor = hoverDonate ? 0xFF66CC66 : 0xFF44AA44;
            guiGraphics.fill(btnDonateX, btnDonateY, btnDonateX + btnWidth, btnDonateY + btnHeight, donateColor);
            guiGraphics.renderOutline(btnDonateX, btnDonateY, btnWidth, btnHeight, 0xFF228822);

            String donateText = "施舍";
            int donateTextWidth = this.font.width(donateText);
            guiGraphics.drawString(this.font, donateText,
                    btnDonateX + (btnWidth - donateTextWidth) / 2,
                    btnDonateY + (btnHeight - 8) / 2, 0xFFFFFFFF, false);

            // ---- 拒绝按钮 ----
            int btnDeclineX = startX + 138;
            int btnDeclineY = startY + 90;

            boolean hoverDecline = mouseX >= btnDeclineX && mouseX < btnDeclineX + btnWidth
                    && mouseY >= btnDeclineY && mouseY < btnDeclineY + btnHeight;
            int declineColor = hoverDecline ? 0xFFCC6666 : 0xFFAA4444;
            guiGraphics.fill(btnDeclineX, btnDeclineY, btnDeclineX + btnWidth, btnDeclineY + btnHeight, declineColor);
            guiGraphics.renderOutline(btnDeclineX, btnDeclineY, btnWidth, btnHeight, 0xFF882222);

            String declineText = "拒绝";
            int declineTextWidth = this.font.width(declineText);
            guiGraphics.drawString(this.font, declineText,
                    btnDeclineX + (btnWidth - declineTextWidth) / 2,
                    btnDeclineY + (btnHeight - 8) / 2, 0xFFFFFFFF, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = (this.width - GUI_WIDTH) / 2;
        int startY = (this.height - GUI_HEIGHT) / 2;

        int btnWidth = 80;
        int btnHeight = 20;

        // 施舍按钮
        int btnDonateX = startX + 38;
        int btnDonateY = startY + 90;
        if (mouseX >= btnDonateX && mouseX < btnDonateX + btnWidth
                && mouseY >= btnDonateY && mouseY < btnDonateY + btnHeight) {
            this.submitDonation();
            return true;
        }

        // 拒绝按钮
        int btnDeclineX = startX + 138;
        int btnDeclineY = startY + 90;
        if (mouseX >= btnDeclineX && mouseX < btnDeclineX + btnWidth
                && mouseY >= btnDeclineY && mouseY < btnDeclineY + btnHeight) {
            this.sendResponse(false, 0);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void submitDonation() {
        if (responded) return;
        String text = this.amountField != null ? this.amountField.getValue() : "";
        int amount;
        try {
            amount = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return;
        }

        if (amount < 20 || amount > 50) {
            return;
        }

        this.sendResponse(true, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            this.submitDonation();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (!responded) {
            this.sendResponse(false, 0);
        }
        super.onClose();
    }

    private void sendResponse(boolean accepted, int amount) {
        if (responded) return;
        responded = true;

        ClientPlayNetworking.send(new BeggingResponsePayload(this.beggarUuid, accepted, amount));

        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}