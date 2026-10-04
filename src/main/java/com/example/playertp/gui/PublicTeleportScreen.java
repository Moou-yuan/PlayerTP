package com.example.playertp.gui;

import com.example.playertp.KeyBindings;
import com.example.playertp.config.ClientConfig;
import com.example.playertp.core.TeleportPoint;
import com.example.playertp.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class PublicTeleportScreen extends Screen {
    private static List<TeleportPoint> publicPoints = new ArrayList<>();
    private final List<PointEntry> pointEntries = new ArrayList<>();
    private static final int WIDTH = 340;
    private static final int ENTRY_HEIGHT = 55;
    private static final int BORDER_COLOR = 0xFF4A90D9;
    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int BG_HOVER_COLOR = 0xCC2D2D44;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int HEADER_HEIGHT = 58;
    private static final int PANEL_HEIGHT = 240;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int VISIBLE_LIST_HEIGHT = PANEL_HEIGHT - HEADER_HEIGHT - 5;

    private Button playerListButton;
    private Button personalButton;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public PublicTeleportScreen() {
        super(Component.translatable("gui.playertp.public_points.title"));
    }

    @Override
    protected void init() {
        ModNetwork.sendToServer(new ModNetwork.RequestSyncPointsPacket());

        int x = (this.width - WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        // Nav buttons — top row
        playerListButton = Button.builder(Component.translatable("gui.playertp.nav.player_list"), (btn) -> {
            minecraft.setScreen(new PlayerListScreen());
        }).bounds(x + 10, y + 5, 75, 20).build();
        this.addWidget(playerListButton);

        personalButton = Button.builder(Component.translatable("gui.playertp.nav.personal"), (btn) -> {
            minecraft.setScreen(new PersonalTeleportScreen());
        }).bounds(x + WIDTH - 85, y + 5, 75, 20).build();
        this.addWidget(personalButton);
    }

    public static void updatePoints(List<TeleportPoint> points) {
        publicPoints = points;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int x = (this.width - WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        drawCustomBackground(guiGraphics, x, y, WIDTH, PANEL_HEIGHT);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, y + 12, TEXT_COLOR);
        guiGraphics.drawString(font, Component.translatable("gui.playertp.total_points", publicPoints.size()).getString(), x + 10, y + 35, TEXT_COLOR);

        playerListButton.render(guiGraphics, mouseX, mouseY, partialTick);
        personalButton.render(guiGraphics, mouseX, mouseY, partialTick);

        // Entries with scroll
        int listTopY = y + HEADER_HEIGHT;
        int listBottomY = y + PANEL_HEIGHT - 10;

        pointEntries.clear();
        for (int i = 0; i < publicPoints.size(); i++) {
            int entryY = listTopY + i * ENTRY_HEIGHT - scrollOffset;
            pointEntries.add(new PointEntry(publicPoints.get(i), entryY));
        }

        guiGraphics.enableScissor(x + 5, listTopY, x + WIDTH - 5, listBottomY);
        for (PointEntry entry : pointEntries) {
            entry.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        guiGraphics.disableScissor();

        int totalContentHeight = publicPoints.size() * ENTRY_HEIGHT;
        maxScroll = Math.max(0, totalContentHeight - VISIBLE_LIST_HEIGHT);
        if (maxScroll > 0) {
            drawScrollbar(guiGraphics, x + WIDTH - 2, listTopY, VISIBLE_LIST_HEIGHT, maxScroll);
        }

        if (publicPoints.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.playertp.empty_public").getString(), this.width / 2, y + PANEL_HEIGHT / 2, TEXT_COLOR);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawScrollbar(GuiGraphics guiGraphics, int barX, int barY, int barHeight, int maxScrollVal) {
        int barThumbHeight = Math.max(16, barHeight * barHeight / (barHeight + maxScrollVal));
        int barThumbY = barY + (scrollOffset * (barHeight - barThumbHeight) / maxScrollVal);
        guiGraphics.fill(barX, barY, barX + SCROLLBAR_WIDTH, barY + barHeight, 0x44FFFFFF);
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
        for (PointEntry entry : pointEntries) {
            if (entry.y >= listTopY - 5 && entry.y <= y + PANEL_HEIGHT) {
                if (entry.mouseClicked(mouseX, mouseY, button)) return true;
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
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Toggle this screen off
        if (keyCode == KeyBindings.OPEN_PUBLIC_POINTS.getKey().getValue()) {
            this.onClose();
            return true;
        }
        // "=" exits to game
        if (keyCode == KeyBindings.OPEN_PLAYER_LIST.getKey().getValue()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static List<TeleportPoint> getPoints() { return publicPoints; }

    // ═══════════════════════════════════════════════════════════════
    // PointEntry
    // ═══════════════════════════════════════════════════════════════

    private class PointEntry {
        private final TeleportPoint point;
        private final int y;
        private final Button upButton;
        private final Button downButton;
        private final Button teleportButton;
        private final Button deleteButton;
        private final Button renderButton;

        PointEntry(TeleportPoint point, int y) {
            this.point = point;
            this.y = y;

            upButton = Button.builder(Component.literal("§7▲"), (btn) -> {
                ModNetwork.sendToServer(new ModNetwork.MovePointPacket(point.id, true, true));
            }).bounds(0, 0, 16, 18).build();

            downButton = Button.builder(Component.literal("§7▼"), (btn) -> {
                ModNetwork.sendToServer(new ModNetwork.MovePointPacket(point.id, true, false));
            }).bounds(0, 0, 16, 18).build();

            teleportButton = Button.builder(Component.translatable("gui.playertp.button.teleport"), (btn) -> {
                ModNetwork.sendToServer(new ModNetwork.TeleportToPointPacket(point.id, true));
                minecraft.setScreen(null);
            }).bounds(0, 0, 34, 18).build();

            deleteButton = Button.builder(Component.translatable("gui.playertp.button.delete"), (btn) -> {
                if (minecraft != null) {
                    minecraft.setScreen(new PersonalTeleportScreen.ConfirmDeleteScreen(
                            PublicTeleportScreen.this, point.name, () -> {
                        ModNetwork.sendToServer(new ModNetwork.DeletePublicPointPacket(point.id));
                    }));
                }
            }).bounds(0, 0, 34, 18).build();

            boolean pointVisible = com.example.playertp.render.CompassRenderer.isPointVisible(point.id);
            renderButton = Button.builder(
                    Component.translatable(pointVisible ? "gui.playertp.button.render_on" : "gui.playertp.button.render_off"),
                    (btn) -> {
                        com.example.playertp.render.CompassRenderer.togglePointRender(point.id);
                        if (minecraft != null) {
                            minecraft.setScreen(new PublicTeleportScreen());
                        }
                    }
            ).bounds(0, 0, 34, 18).build();
        }

        void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = (width - WIDTH) / 2;
            boolean isHovered = mouseX >= x + 5 && mouseX <= x + WIDTH - 5
                    && mouseY >= y && mouseY <= y + ENTRY_HEIGHT - 5;

            guiGraphics.fill(x + 5, y, x + WIDTH - 5, y + ENTRY_HEIGHT - 5,
                    isHovered ? BG_HOVER_COLOR : BG_COLOR);

            // ▲▼ on left, aligned with right-button rows
            upButton.setX(x + 4); upButton.setY(y + 4);
            upButton.render(guiGraphics, mouseX, mouseY, partialTick);
            downButton.setX(x + 4); downButton.setY(y + 27);
            downButton.render(guiGraphics, mouseX, mouseY, partialTick);

            int leftPad = 24;
            String icon = (point.iconText != null && !point.iconText.isEmpty()) ? point.iconText : "★";
            guiGraphics.drawString(font, "§6" + icon, x + leftPad, y + 10, TEXT_COLOR);
            guiGraphics.drawString(font, "§f" + point.name, x + leftPad + 16, y + 10, TEXT_COLOR);

            String coords = String.format("§6%.0f §b%.0f §a%.0f", point.x, point.y, point.z);
            guiGraphics.drawString(font, coords, x + leftPad + 16, y + 30, TEXT_COLOR);

            guiGraphics.drawString(font, "§7" + point.ownerName, x + leftPad + 16, y + 44, TEXT_COLOR);

            String dimName = getDimensionName(point.dimension);
            guiGraphics.drawString(font, "§7" + dimName, x + WIDTH - 160, y + 30, TEXT_COLOR);

            // Uniform right-aligned rows: row1 (y+4) two 34px buttons; row2 (y+27) render(34)
            int btnGap = 1;
            int bx = x + WIDTH - 4;
            bx -= 34; deleteButton.setX(bx); deleteButton.setY(y + 4);
            deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            bx -= btnGap + 34; teleportButton.setX(bx); teleportButton.setY(y + 4);
            teleportButton.render(guiGraphics, mouseX, mouseY, partialTick);

            bx = x + WIDTH - 4 - 34;
            renderButton.setX(bx); renderButton.setY(y + 27);
            renderButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        private String getDimensionName(ResourceLocation dimension) {
            if (dimension == null) return Component.translatable("dimension.playertp.unknown").getString();
            String name = dimension.getPath();
            if (name.contains("overworld")) return Component.translatable("dimension.playertp.overworld").getString();
            if (name.contains("nether")) return Component.translatable("dimension.playertp.nether").getString();
            if (name.contains("end")) return Component.translatable("dimension.playertp.end").getString();
            return name;
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            int x = (width - WIDTH) / 2;
            // ▲▼ on left
            if (inRect(mouseX, mouseY, x + 4, y + 4, 16, 18)) {
                upButton.mouseClicked(mouseX, mouseY, button); return true; }
            if (inRect(mouseX, mouseY, x + 4, y + 27, 16, 18)) {
                downButton.mouseClicked(mouseX, mouseY, button); return true; }
            // Row 1 (y+4): delete/teleport, 34px each
            int btnGap = 1;
            int bx = x + WIDTH - 4;
            bx -= 34;
            if (inRect(mouseX, mouseY, bx, y + 4, 34, 18)) { deleteButton.mouseClicked(mouseX, mouseY, button); return true; }
            bx -= btnGap + 34;
            if (inRect(mouseX, mouseY, bx, y + 4, 34, 18)) { teleportButton.mouseClicked(mouseX, mouseY, button); return true; }
            // Row 2 (y+27): render(34)
            bx = x + WIDTH - 4 - 34;
            if (inRect(mouseX, mouseY, bx, y + 27, 34, 18)) { renderButton.mouseClicked(mouseX, mouseY, button); return true; }
            return false;
        }

        private boolean inRect(double mx, double my, int rx, int ry, int rw, int rh) {
            return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
        }
    }
}
