package org.agmas.pathsrole.client;

import io.wifi.starrailexpress.api.SRERole;
import io.wifi.starrailexpress.cca.SREGameWorldComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RoleScreenHelper<T> {
    private final LocalPlayer player;
    private final PlayerPaginationHelper<T> paginationHelper;
    private final SRERole role;
    private final BiConsumer<GuiGraphics, Point> extraDrawer;
    private final Supplier<List<T>> entriesSupplier;

    public RoleScreenHelper(LocalPlayer player,
                            SRERole role,
                            PlayerPaginationHelper.PlayerWidgetCreator<T> widgetCreator,
                            PlayerPaginationHelper.PaginationTextProvider textProvider,
                            BiConsumer<GuiGraphics, Point> extraDrawer,
                            Supplier<List<T>> entriesSupplier) {
        this.player = player;
        this.role = role;
        this.paginationHelper = new PlayerPaginationHelper<>(widgetCreator, textProvider);
        this.extraDrawer = extraDrawer;
        this.entriesSupplier = entriesSupplier;
    }

    public boolean isRoleActive() {
        SREGameWorldComponent gameWorldComponent = SREGameWorldComponent.KEY.get(player.level());
        return gameWorldComponent != null && gameWorldComponent.isRole(player, role);
    }

    public void onRender(GuiGraphics context, PlayerPaginationHelper.ScreenWithChildren screen) {
        if (!isRoleActive()) {
            return;
        }
        Screen screenAsScreen = (Screen) screen;
        int y = (screenAsScreen.height - 32) / 2;
        int x = screenAsScreen.width / 2;
        if (extraDrawer != null) {
            extraDrawer.accept(context, new Point(x, y));
        }
        paginationHelper.drawPagination(context, screenAsScreen, y);
    }

    public void onInit(PlayerPaginationHelper.ScreenWithChildren screen) {
        paginationHelper.clearManagedWidgets(screen);
        if (!isRoleActive()) {
            return;
        }
        List<T> entries = entriesSupplier.get();
        paginationHelper.setPlayerEntries(entries);
        paginationHelper.addPageWidgets((Screen) screen);
    }

    public PlayerPaginationHelper<T> getPaginationHelper() {
        return paginationHelper;
    }
}