package org.agmas.pathsrole.mixin;

import io.wifi.starrailexpress.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.agmas.pathsrole.client.PlayerPaginationHelper;
import org.agmas.pathsrole.client.RoleScreenHelper;
import org.agmas.pathsrole.client.widget.KiraPlayerWidget;
import org.agmas.pathsrole.init.ModRoles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(value = LimitedInventoryScreen.class, priority = 500)
public abstract class KiraScreenMixin extends io.wifi.starrailexpress.client.gui.screen.ingame.LimitedHandledScreen<InventoryMenu> implements PlayerPaginationHelper.ScreenWithChildren {
    @Unique
    private static final PlayerPaginationHelper.PaginationTextProvider TEXT_PROVIDER = new PlayerPaginationHelper.PaginationTextProvider() {
        @Override
        public String getPageTranslationKey() {
            return "hud.pagination.page";
        }

        @Override
        public String getPrevTranslationKey() {
            return "hud.pagination.prev";
        }

        @Override
        public String getNextTranslationKey() {
            return "hud.pagination.next";
        }
    };

    @Shadow @Final
    public LocalPlayer player;

    @Unique
    private RoleScreenHelper<PlayerInfo> roleScreenHelper;

    public KiraScreenMixin(InventoryMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Unique
    private RoleScreenHelper<PlayerInfo> getRoleScreenHelper() {
        if (roleScreenHelper == null) {
            roleScreenHelper = new RoleScreenHelper<>(
                    player,
                    ModRoles.KIRA,
                    this::createKiraWidget,
                    TEXT_PROVIDER,
                    this::drawKiraSelectionHint,
                    this::getEligiblePlayers
            );
        }
        return roleScreenHelper;
    }

    @Unique
    private KiraPlayerWidget createKiraWidget(int x, int y, PlayerInfo playerEntity, int index) {
        KiraPlayerWidget widget = new KiraPlayerWidget(
                (LimitedInventoryScreen) (Object) this,
                x, y, playerEntity
        );
        addDrawableChild(widget);
        return widget;
    }

    @Unique
    private void drawKiraSelectionHint(GuiGraphics context, Point point) {
        Minecraft client = Minecraft.getInstance();
        Component text = Component.translatable("hud.pathsrole.kira.player_selection");
        int color = Color.CYAN.getRGB();

        int textWidth = client.font.width(text);
        context.drawString(client.font, text,
                point.x - textWidth / 2, point.y + 40, color);
    }

    @Unique
    private List<PlayerInfo> getEligiblePlayers() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return List.of();
        }

        return client.getConnection().getListedOnlinePlayers().stream()
                .filter(a -> a.getProfile().getId() != player.getUUID())
                .collect(Collectors.toList());
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0, expect = 0)
    private void kiraaddon$onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        getRoleScreenHelper().onRender(context, this);
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0, expect = 0)
    private void kiraaddon$hideDefaultKillerHud(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!kiraaddon$isKiraActive()) {
            return;
        }
    }

    @Unique
    private boolean kiraaddon$isKiraActive() {
        if (player == null || player.level() == null) {
            return false;
        }
        return getRoleScreenHelper().isRoleActive();
    }

    @Inject(method = "init", at = @At("HEAD"), require = 0, expect = 0)
    private void kiraaddon$onInit(CallbackInfo ci) {
        if (roleScreenHelper != null) {
            roleScreenHelper.getPaginationHelper().clearManagedWidgets(this);
        }
        roleScreenHelper = null;
        getRoleScreenHelper().onInit(this);
    }
}