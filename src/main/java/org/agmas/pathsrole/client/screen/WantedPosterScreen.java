package org.agmas.pathsrole.client.screen;

import io.wifi.starrailexpress.client.SREClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.agmas.pathsrole.client.widget.WantedPosterPlayerWidget;
import org.agmas.pathsrole.init.ModRoles;
import org.agmas.pathsrole.network.WantedPosterSignC2SPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class WantedPosterScreen extends Screen {
    private final List<WantedPosterPlayerWidget> playerWidgets = new ArrayList<>();
    private EditBox searchWidget;

    public WantedPosterScreen() {
        super(Component.translatable("screen.pathsrole.wanted_poster.title"));
    }

    @Override
    protected void init() {
        super.init();
        refreshPlayers(null);
    }

    private void refreshPlayers(String searchText) {
        for (var w : playerWidgets) {
            removeWidget(w);
        }
        playerWidgets.clear();

        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            onClose();
            return;
        }

        List<Player> candidates = new ArrayList<>();
        for (Player p : minecraft.level.players()) {
            if (p.isAlive() && !p.isSpectator()) candidates.add(p);
        }
        candidates.sort(Comparator.comparing(p -> p.getName().getString(), String.CASE_INSENSITIVE_ORDER));
        candidates.removeIf(p -> p.getUUID().equals(minecraft.player.getUUID()));

        if (SREClient.gameComponent != null) {
            candidates.removeIf(p -> {
                var role = SREClient.gameComponent.getRole(p);
                return SREClient.gameComponent.isRole(p, ModRoles.BOUNTY_HUNTER)
                    || (role != null && role.isKillerTeam());
            });
        }

        if (searchText != null && !searchText.isBlank()) {
            String lower = searchText.toLowerCase();
            candidates.removeIf(p -> p.getName().getString().toLowerCase().contains(lower));
        }

        int widgetSize = 32;
        int spacing = 8;
        int columns = Math.min(8, Math.max(candidates.size(), 1));
        int rows = Math.max(1, (int) Math.ceil(candidates.size() / 8.0));
        int totalWidth = columns * (widgetSize + spacing) - spacing;
        int totalHeight = rows * (widgetSize + spacing) - spacing;
        int startX = (width - totalWidth) / 2;
        int startY = (height - totalHeight) / 2 + 20;

        if (searchWidget == null) {
            searchWidget = new EditBox(font, startX, startY - 40, totalWidth, 20, Component.empty());
            searchWidget.setHint(Component.translatable("screen.pathsrole.search.placeholder").withStyle(ChatFormatting.GRAY));
            searchWidget.setResponder(this::refreshPlayers);
            addRenderableWidget(searchWidget);
        }

        boolean empty = candidates.isEmpty();
        searchWidget.setTextColor(empty ? Color.RED.getRGB() : Color.WHITE.getRGB());

        for (int i = 0; i < candidates.size(); i++) {
            int col = i % 8;
            int row = i / 8;
            int x = startX + col * (widgetSize + spacing);
            int y = startY + row * (widgetSize + spacing);

            Player target = candidates.get(i);
            UUID uuid = target.getUUID();
            String name = target.getName().getString();
            ResourceLocation skin = DefaultPlayerSkin.get(uuid).texture();
            PlayerInfo playerInfo = minecraft.getConnection() != null
                ? minecraft.getConnection().getPlayerInfo(uuid) : null;
            if (playerInfo != null && playerInfo.getSkin() != null) {
                skin = playerInfo.getSkin().texture();
            }

            var widget = new WantedPosterPlayerWidget(this, x, y, widgetSize, uuid, name, skin);
            playerWidgets.add(widget);
            addRenderableWidget(widget);
        }
    }

    public void onPlayerSelected(UUID targetUuid, String targetName) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        ClientPlayNetworking.send(new WantedPosterSignC2SPacket(targetUuid));
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(
                font,
                Component.translatable("screen.pathsrole.wanted_poster.title"),
                centerX,
                18,
                0xFFE6C37A
        );
        guiGraphics.drawCenteredString(
                font,
                Component.translatable("screen.pathsrole.wanted_poster.subtitle"),
                centerX,
                34,
                0xFFFFFFFF
        );
    }
}