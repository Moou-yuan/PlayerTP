package com.example.playertp.gui;

import com.example.playertp.KeyBindings;
import com.example.playertp.config.ClientConfig;
import com.example.playertp.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerListScreen extends Screen {
    private static List<PlayerInfo> serverPlayers = new ArrayList<>();
    private final List<PlayerEntry> playerEntries = new ArrayList<>();
    private static final int WIDTH = 340;
    private static final int ENTRY_HEIGHT = 40;
    private static final int BORDER_COLOR = 0xFF4A90D9;
    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int BG_HOVER_COLOR = 0xCC2D2D44;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    // Layout constants
    private static final int HEADER_HEIGHT = 55;
    private static final int PANEL_HEIGHT = 260;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int VISIBLE_LIST_HEIGHT = PANEL_HEIGHT - HEADER_HEIGHT - 10;

    private Button personalButton;
    private Button publicButton;
    private Button compassToggleButton;
    private Button refreshButton;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public PlayerListScreen() {
        super(Component.translatable("gui.playertp.player_list.title"));
    }

    @Override
    protected void init() {
        ModNetwork.sendToServer(new ModNetwork.RequestPlayerListPacket());

        int x = (this.width - WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        personalButton = Button.builder(Component.translatable("gui.playertp.nav.personal_points"), (btn) -> {
            minecraft.setScreen(new PersonalTeleportScreen());
        }).bounds(x + 10, y + 30, 85, 20).build();
        this.addWidget(personalButton);

        publicButton = Button.builder(Component.translatable("gui.playertp.nav.public_points"), (btn) -> {
            minecraft.setScreen(new PublicTeleportScreen());
        }).bounds(x + WIDTH - 95, y + 30, 85, 20).build();
        this.addWidget(publicButton);

        // Compass config button — opens the compass editor screen
        boolean compassOn = ClientConfig.renderCompass.get();
        compassToggleButton = Button.builder(
                Component.translatable(compassOn ? "gui.playertp.nav.compass_on" : "gui.playertp.nav.compass_off"),
                (btn) -> {
                    minecraft.setScreen(new CompassConfigScreen(this));
                }
        ).bounds(x + 100, y + 30, 44, 20).build();
        this.addWidget(compassToggleButton);

        // Refresh skins button
        refreshButton = Button.builder(
                Component.literal("§d🔄"),
                (btn) -> {
                    ModNetwork.sendToServer(new ModNetwork.RefreshCompassSkinsPacket());
                }
        ).bounds(x + WIDTH / 2 - 22, y + 30, 44, 20).build();
        this.addWidget(refreshButton);
    }

    public static void updatePlayers(List<PlayerInfo> players) {
        serverPlayers = players;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int x = (this.width - WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        // Draw panel background
        drawCustomBackground(guiGraphics, x, y, WIDTH, PANEL_HEIGHT);

        // Centered title at top
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.playertp.player_list.title"), this.width / 2, y + 10, TEXT_COLOR);

        // Online count below title
        String countText = Component.translatable("gui.playertp.online_count", serverPlayers.size()).getString();
        guiGraphics.drawCenteredString(this.font, countText, this.width / 2, y + 22, TEXT_COLOR);

        // Render nav buttons
        personalButton.render(guiGraphics, mouseX, mouseY, partialTick);
        publicButton.render(guiGraphics, mouseX, mouseY, partialTick);
        compassToggleButton.render(guiGraphics, mouseX, mouseY, partialTick);
        refreshButton.render(guiGraphics, mouseX, mouseY, partialTick);

        // Build player entries
        playerEntries.clear();
        int listTopY = y + HEADER_HEIGHT;
        UUID selfUUID = minecraft.player != null ? minecraft.player.getUUID() : null;
        boolean selfIsHidden = false;

        // Find self visibility state
        for (PlayerInfo pi : serverPlayers) {
            if (selfUUID != null && pi.uuid.equals(selfUUID)) {
                selfIsHidden = pi.isHidden;
                break;
            }
        }

        // Build sorted list: self first, then others
        List<PlayerInfo> sortedPlayers = new ArrayList<>();
        for (PlayerInfo pi : serverPlayers) {
            if (selfUUID != null && pi.uuid.equals(selfUUID)) {
                sortedPlayers.add(pi);
                break;
            }
        }
        for (PlayerInfo pi : serverPlayers) {
            if (selfUUID == null || !pi.uuid.equals(selfUUID)) {
                sortedPlayers.add(pi);
            }
        }

        // Calculate entry positions with scroll offset
        int entryY = listTopY;
        for (int i = 0; i < sortedPlayers.size(); i++) {
            PlayerInfo player = sortedPlayers.get(i);
            boolean isSelf = selfUUID != null && player.uuid.equals(selfUUID);
            int actualY = listTopY + i * ENTRY_HEIGHT - scrollOffset;
            PlayerEntry entry = new PlayerEntry(player, actualY, isSelf, selfIsHidden);
            playerEntries.add(entry);
        }

        // Enable scissor for the list area
        int listBottomY = y + PANEL_HEIGHT - 5;
        guiGraphics.enableScissor(x + 5, listTopY, x + WIDTH - 5, listBottomY);

        // Render entries inside scissor region
        for (PlayerEntry entry : playerEntries) {
            entry.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        guiGraphics.disableScissor();

        // Calculate max scroll
        int totalContentHeight = sortedPlayers.size() * ENTRY_HEIGHT;
        maxScroll = Math.max(0, totalContentHeight - VISIBLE_LIST_HEIGHT);

        // Render scrollbar if needed
        if (maxScroll > 0) {
            drawScrollbar(guiGraphics, x + WIDTH - 2, listTopY, VISIBLE_LIST_HEIGHT, maxScroll);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawScrollbar(GuiGraphics guiGraphics, int barX, int barY, int barHeight, int maxScrollVal) {
        int barThumbHeight = Math.max(20, barHeight * barHeight / (barHeight + maxScrollVal));
        int barThumbY = barY + (scrollOffset * (barHeight - barThumbHeight) / maxScrollVal);

        // Track background
        guiGraphics.fill(barX, barY, barX + SCROLLBAR_WIDTH, barY + barHeight, 0x44FFFFFF);
        // Thumb
        guiGraphics.fill(barX, barThumbY, barX + SCROLLBAR_WIDTH, barThumbY + barThumbHeight, 0x88FFFFFF);
    }

    private void drawCustomBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, BORDER_COLOR);
        guiGraphics.fill(x, y, x + width, y + height, BG_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;
        int listTopY = y + HEADER_HEIGHT;

        // Only pass clicks within the visible list area to entries
        for (PlayerEntry entry : playerEntries) {
            if (entry.y >= listTopY - 5 && entry.y <= y + PANEL_HEIGHT) {
                if (entry.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            scrollOffset -= (int) (delta * 20);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KeyBindings.OPEN_PLAYER_LIST.getKey().getValue()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static List<PlayerInfo> getPlayers() {
        return serverPlayers;
    }

    public static class PlayerInfo {
        public final UUID uuid;
        public final String name;
        public final double x;
        public final double y;
        public final double z;
        public final ResourceLocation dimension;
        public final boolean isHidden;

        public PlayerInfo(UUID uuid, String name, double x, double y, double z, ResourceLocation dimension, boolean isHidden) {
            this.uuid = uuid;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.isHidden = isHidden;
        }
    }

    private class PlayerEntry {
        private final PlayerInfo player;
        private final int y;
        private final boolean isSelf;
        private final boolean selfIsHidden;
        private Button actionButton;

        public PlayerEntry(PlayerInfo player, int y, boolean isSelf, boolean selfIsHidden) {
            this.player = player;
            this.y = y;
            this.isSelf = isSelf;
            this.selfIsHidden = selfIsHidden;

            if (isSelf) {
                this.actionButton = Button.builder(Component.translatable(selfIsHidden ? "gui.playertp.button.show_coords" : "gui.playertp.button.hide_coords"), (btn) -> {
                    toggleSelfVisibility();
                }).bounds(0, 0, 80, 22).build();
            } else if (player.isHidden) {
                this.actionButton = Button.builder(Component.translatable("gui.playertp.button.hidden"), (btn) -> {}).bounds(0, 0, 65, 22).build();
                this.actionButton.active = false;
            } else {
                this.actionButton = Button.builder(Component.translatable("gui.playertp.button.teleport"), (btn) -> {
                    teleport();
                }).bounds(0, 0, 65, 22).build();
            }
        }

        private void toggleSelfVisibility() {
            ModNetwork.sendToServer(new ModNetwork.ToggleVisibilityPacket());
            ModNetwork.sendToServer(new ModNetwork.RequestPlayerListPacket());
        }

        private void teleport() {
            if (minecraft.player != null && !minecraft.player.getUUID().equals(player.uuid)) {
                ModNetwork.sendToServer(new ModNetwork.TeleportToPlayerPacket(player.x, player.y, player.z, player.dimension));
                minecraft.setScreen(null);
            }
        }

        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = (width - WIDTH) / 2;

            boolean isHovered = mouseX >= x + 5 && mouseX <= x + WIDTH - 5 &&
                    mouseY >= y && mouseY <= y + ENTRY_HEIGHT - 5;

            guiGraphics.fill(x + 5, y, x + WIDTH - 5, y + ENTRY_HEIGHT - 5,
                    isHovered ? BG_HOVER_COLOR : BG_COLOR);

            guiGraphics.drawString(font, "§b●", x + 12, y + 12, TEXT_COLOR);

            String playerName = isSelf ? Component.translatable("gui.playertp.self_name", player.name).getString() : "§f" + player.name;
            guiGraphics.drawString(font, playerName, x + 28, y + 10, TEXT_COLOR);

            if (isSelf) {
                String coords = String.format("§6X: %.1f §bY: %.1f §aZ: %.1f", player.x, player.y, player.z);
                guiGraphics.drawString(font, coords, x + 28, y + 26, TEXT_COLOR);
                String dimName = getDimensionName(player.dimension);
                guiGraphics.drawString(font, "§7" + dimName, x + WIDTH - 140, y + 12, TEXT_COLOR);
            } else if (!player.isHidden) {
                String coords = String.format("§6X: %.1f §bY: %.1f §aZ: %.1f", player.x, player.y, player.z);
                guiGraphics.drawString(font, coords, x + 28, y + 26, TEXT_COLOR);
                String dimName = getDimensionName(player.dimension);
                guiGraphics.drawString(font, "§7" + dimName, x + WIDTH - 140, y + 12, TEXT_COLOR);
            } else {
                guiGraphics.drawString(font, Component.translatable("gui.playertp.coords_hidden").getString(), x + 28, y + 26, 0xFF888888);
            }

            int buttonX = x + WIDTH - (isSelf ? 85 : 70);
            actionButton.setX(buttonX);
            actionButton.setY(y + 8);
            actionButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        private String getDimensionName(ResourceLocation dimension) {
            if (dimension == null) return Component.translatable("dimension.playertp.unknown").getString();
            String name = dimension.getPath();
            if (name.contains("overworld")) return Component.translatable("dimension.playertp.overworld").getString();
            if (name.contains("nether")) return Component.translatable("dimension.playertp.nether").getString();
            if (name.contains("end")) return Component.translatable("dimension.playertp.end").getString();
            return name;
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int x = (width - WIDTH) / 2;
            int buttonWidth = isSelf ? 80 : 65;
            int buttonX = x + WIDTH - (isSelf ? 85 : 70);

            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                    mouseY >= y + 8 && mouseY <= y + 30) {
                actionButton.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            return false;
        }
    }
}
