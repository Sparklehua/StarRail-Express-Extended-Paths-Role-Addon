package org.agmas.pathsrole.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerPaginationHelper<T> {
    private static final int PLAYERS_PER_PAGE = 8;
    
    private int currentPage = 0;
    private List<T> playerEntries = List.of();
    
    private final List<Button> managedButtons = new ArrayList<>();
    private final List<Button> managedPlayerWidgets = new ArrayList<>();
    
    private final PlayerWidgetCreator<T> widgetCreator;
    private final PaginationTextProvider textProvider;
    
    public interface PlayerWidgetCreator<T> {
        Button createWidget(int x, int y, T playerEntry, int index);
    }
    
    public interface PaginationTextProvider {
        String getPageTranslationKey();
        String getPrevTranslationKey();
        String getNextTranslationKey();
    }
    
    public PlayerPaginationHelper(PlayerWidgetCreator<T> widgetCreator, PaginationTextProvider textProvider) {
        this.widgetCreator = widgetCreator;
        this.textProvider = textProvider;
    }
    
    public void setPlayerEntries(List<T> playerEntries) {
        this.playerEntries = List.copyOf(playerEntries);
        this.currentPage = 0;
    }
    
    public void drawPagination(GuiGraphics context, Screen screen, int centerY) {
        int totalPages = getTotalPages();
        if (totalPages > 1) {
            Component pageText = Component.translatable(textProvider.getPageTranslationKey(), currentPage + 1, totalPages);
            int pageTextWidth = Minecraft.getInstance().font.width(pageText);
            context.drawString(Minecraft.getInstance().font, pageText,
                    screen.width / 2 - pageTextWidth / 2,
                    centerY + 120, Color.WHITE.getRGB());
        }
    }
    
    public void addPageWidgets(Screen screen) {
        int totalPages = getTotalPages();
        if (totalPages == 0) {
            return;
        }
        
        int apart = 36;
        int startIndex = currentPage * PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, playerEntries.size());
        int visibleCount = endIndex - startIndex;
        int x = screen.width / 2 - visibleCount * apart / 2 + 9;
        int centerY = (screen.height - 32) / 2;
        int y = centerY + 80;

        for (int i = startIndex; i < endIndex; ++i) {
            T playerEntry = playerEntries.get(i);
            Button playerWidget = widgetCreator.createWidget(x + apart * (i - startIndex), y, playerEntry, i);
            if (playerWidget != null) {
                managedPlayerWidgets.add(playerWidget);
            }
        }

        if (totalPages > 1) {
            int buttonY = y + 40;
            
            Button prevButton = Button.builder(Component.translatable(textProvider.getPrevTranslationKey()), button -> {
                if (currentPage > 0) {
                    currentPage--;
                    refreshPage(screen);
                }
            }).bounds(screen.width / 2 - 80, buttonY, 50, 20).build();
            
            Button nextButton = Button.builder(Component.translatable(textProvider.getNextTranslationKey()), button -> {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    refreshPage(screen);
                }
            }).bounds(screen.width / 2 + 30, buttonY, 50, 20).build();
            
            managedButtons.add(prevButton);
            managedButtons.add(nextButton);
            
            ((ScreenWithChildren) screen).addDrawableChild(prevButton);
            ((ScreenWithChildren) screen).addDrawableChild(nextButton);
        }
    }
    
    public void clearManagedWidgets(ScreenWithChildren screen) {
        for (Button button : managedButtons) {
            screen.removeDrawableChild(button);
        }
        managedButtons.clear();
        
        for (Button widget : managedPlayerWidgets) {
            screen.removeDrawableChild(widget);
        }
        managedPlayerWidgets.clear();
    }
    
    public void refreshPage(Screen screen) {
        clearManagedWidgets((ScreenWithChildren) screen);
        addPageWidgets(screen);
    }
    
    private int getTotalPages() {
        return playerEntries.isEmpty() ? 0 : (int) Math.ceil((double) playerEntries.size() / PLAYERS_PER_PAGE);
    }
    
    public interface ScreenWithChildren {
        void addDrawableChild(Button button);
        void removeDrawableChild(Button button);
        void clearChildren();
    }
}