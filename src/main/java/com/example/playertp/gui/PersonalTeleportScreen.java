package com.example.playertp.gui;

import com.example.playertp.KeyBindings;
import com.example.playertp.config.ClientConfig;
import com.example.playertp.core.TeleportPoint;
import com.example.playertp.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PersonalTeleportScreen extends Screen {
    private static List<TeleportPoint> personalPoints = new ArrayList<>();
    private final List<PointEntry> pointEntries = new ArrayList<>();
    private static final int WIDTH = 340;
    private static final int ENTRY_HEIGHT = 50;
    private static final int BORDER_COLOR = 0xFF4A90D9;
    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int BG_HOVER_COLOR = 0xCC2D2D44;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    // Layout
    private static final int HEADER_HEIGHT = 58;
    private static final int PANEL_HEIGHT = 240;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int VISIBLE_LIST_HEIGHT = PANEL_HEIGHT - HEADER_HEIGHT - 5;

    private Button playerListButton;
    private Button publicButton;
    private Button addButton;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Quick teleport: only one point can be marked
    private static UUID quickTeleportId = null;

    public static UUID getQuickTeleportId() {
        return quickTeleportId;
    }

    public PersonalTeleportScreen() {
        super(Component.translatable("gui.playertp.personal_points.title"));
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

        publicButton = Button.builder(Component.translatable("gui.playertp.nav.public"), (btn) -> {
            minecraft.setScreen(new PublicTeleportScreen());
        }).bounds(x + WIDTH - 85, y + 5, 75, 20).build();
        this.addWidget(publicButton);

        // Action buttons — second row
        addButton = Button.builder(Component.translatable("gui.playertp.button.add"), (btn) -> openAddConfig()).bounds(x + 10, y + 28, 75, 20).build();
        this.addWidget(addButton);
    }

    public static void updatePoints(List<TeleportPoint> points) {
        personalPoints = points;
    }

    private void openAddConfig() {
        if (minecraft != null && minecraft.player != null) {
            int count = personalPoints.size();
            String defaultName = Component.translatable("gui.playertp.point_default_name", count + 1).getString();
            double px = minecraft.player.getX();
            double py = minecraft.player.getY();
            double pz = minecraft.player.getZ();
            minecraft.setScreen(new PointConfigScreen(this, defaultName, px, py, pz, null));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int x = (this.width - WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        drawCustomBackground(guiGraphics, x, y, WIDTH, PANEL_HEIGHT);

        // Centered title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, y + 12, TEXT_COLOR);

        // Dynamic hint texts
        String addKey = KeyBindings.QUICK_ADD_POINT.getTranslatedKeyMessage().getString();
        String tpKey = KeyBindings.QUICK_TELEPORT.getTranslatedKeyMessage().getString();
        String hint = Component.translatable("gui.playertp.hint_personal", addKey, tpKey).getString();
        guiGraphics.drawString(font, hint, x + 10, y + PANEL_HEIGHT - 10, 0xFF888888);

        // Render buttons
        playerListButton.render(guiGraphics, mouseX, mouseY, partialTick);
        publicButton.render(guiGraphics, mouseX, mouseY, partialTick);
        addButton.render(guiGraphics, mouseX, mouseY, partialTick);

        // Build entries with scroll
        int listTopY = y + HEADER_HEIGHT;
        int listBottomY = y + PANEL_HEIGHT - 16;

        pointEntries.clear();
        for (int i = 0; i < personalPoints.size(); i++) {
            int entryY = listTopY + i * ENTRY_HEIGHT - scrollOffset;
            pointEntries.add(new PointEntry(personalPoints.get(i), entryY));
        }

        guiGraphics.enableScissor(x + 5, listTopY, x + WIDTH - 5, listBottomY);
        for (PointEntry entry : pointEntries) {
            entry.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        guiGraphics.disableScissor();

        // Scrollbar
        int totalContentHeight = personalPoints.size() * ENTRY_HEIGHT;
        maxScroll = Math.max(0, totalContentHeight - VISIBLE_LIST_HEIGHT);
        if (maxScroll > 0) {
            drawScrollbar(guiGraphics, x + WIDTH - 2, listTopY, VISIBLE_LIST_HEIGHT, maxScroll);
        }

        if (personalPoints.isEmpty()) {
            guiGraphics.drawCenteredString(font, Component.translatable("gui.playertp.empty_personal").getString(), this.width / 2, y + PANEL_HEIGHT / 2, TEXT_COLOR);
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
        if (keyCode == KeyBindings.OPEN_PERSONAL_POINTS.getKey().getValue()) {
            this.onClose();
            return true;
        }
        // "=" exits to game (not switch to player list)
        if (keyCode == KeyBindings.OPEN_PLAYER_LIST.getKey().getValue()) {
            this.onClose();
            return true;
        }
        // Quick add opens config screen
        if (keyCode == KeyBindings.QUICK_ADD_POINT.getKey().getValue()) {
            if (minecraft != null && minecraft.player != null) {
                int count = personalPoints.size();
                String defaultName = Component.translatable("gui.playertp.point_default_name", count + 1).getString();
                minecraft.setScreen(new PointConfigScreen(this, defaultName,
                        minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), null));
            }
            return true;
        }
        // Quick teleport
        if (keyCode == KeyBindings.QUICK_TELEPORT.getKey().getValue()) {
            if (quickTeleportId != null) {
                ModNetwork.sendToServer(new ModNetwork.TeleportToPointPacket(quickTeleportId, false));
                this.onClose();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static List<TeleportPoint> getPoints() { return personalPoints; }

    // ═══════════════════════════════════════════════════════════════
    // PointConfigScreen
    // ═══════════════════════════════════════════════════════════════

    public static class PointConfigScreen extends Screen {
        private final Screen parent;
        private final String defaultName;
        private final double x, y, z;
        private final TeleportPoint existingPoint;
        private EditBox nameInput;
        private EditBox renderNameInput;
        private Button iconPickButton;
        private String selectedIcon; // instance field — survives init() re-runs
        private static final int BORDER_COLOR = 0xFF4A90D9;
        private static final int BG_COLOR = 0xCC1A1A2E;
        private static final int TEXT_COLOR = 0xFFFFFFFF;
        private static final int DIALOG_W = 280;
        private static final int DIALOG_H = 180;

        public PointConfigScreen(Screen parent, String defaultName, double x, double y, double z, TeleportPoint existingPoint) {
            super(Component.translatable(existingPoint != null ? "gui.playertp.point_config.title_edit" : "gui.playertp.point_config.title_add"));
            this.parent = parent;
            this.defaultName = defaultName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.existingPoint = existingPoint;
            this.selectedIcon = existingPoint != null && existingPoint.iconText != null ? existingPoint.iconText : "";
        }

        @Override
        protected void init() {
            int sx = (this.width - DIALOG_W) / 2;
            int sy = (this.height - DIALOG_H) / 2;

            // Icon picker — button opens picker, selection stored in instance field
            iconPickButton = Button.builder(
                    Component.literal(iconPreviewLabel()),
                    (btn) -> {
                        if (minecraft != null) {
                            minecraft.setScreen(new IconPickerScreen(this, icon -> selectedIcon = icon));
                        }
                    }
            ).bounds(sx + 10, sy + 46, 36, 20).build();
            this.addRenderableWidget(iconPickButton);

            // Render name input (short name shown on compass)
            renderNameInput = new EditBox(font, sx + 52, sy + 46, DIALOG_W - 62, 20, Component.empty());
            renderNameInput.setValue(existingPoint != null && existingPoint.renderName != null ? existingPoint.renderName : "");
            this.addRenderableWidget(renderNameInput);

            // Name input (list name)
            nameInput = new EditBox(font, sx + 10, sy + 74, DIALOG_W - 20, 20, Component.empty());
            nameInput.setValue(existingPoint != null ? existingPoint.name : defaultName);
            this.addRenderableWidget(nameInput);

            // Buttons
            this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.confirm"), (btn) -> confirm())
                    .bounds(sx + 50, sy + 105, 80, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.cancel"), (btn) -> cancel())
                    .bounds(sx + 150, sy + 105, 80, 20).build());
        }

        private String iconPreviewLabel() {
            return selectedIcon == null || selectedIcon.isEmpty() ? Component.translatable("gui.playertp.icon_empty").getString() : selectedIcon;
        }

        private static String getFirstChar(String s) {
            if (s == null || s.isEmpty()) return "●";
            return s.substring(0, 1);
        }

        private void confirm() {
            String name = nameInput.getValue().trim();
            if (name.isEmpty()) name = defaultName;
            String icon = selectedIcon != null ? selectedIcon : "";
            String renderName = renderNameInput.getValue().trim();

            if (existingPoint != null) {
                ModNetwork.sendToServer(new ModNetwork.RenamePointPacket(existingPoint.id, name, icon, renderName, false));
            } else {
                ModNetwork.sendToServer(new ModNetwork.AddPersonalPointPacket(name, icon, renderName));
            }
            returnToParent();
        }

        private void cancel() { returnToParent(); }

        private void returnToParent() {
            if (minecraft != null) minecraft.setScreen(parent);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            int sx = (this.width - DIALOG_W) / 2;
            int sy = (this.height - DIALOG_H) / 2;

            guiGraphics.fill(sx - 2, sy - 2, sx + DIALOG_W + 2, sy + DIALOG_H + 2, BORDER_COLOR);
            guiGraphics.fill(sx, sy, sx + DIALOG_W, sy + DIALOG_H, BG_COLOR);

            String title = Component.translatable(existingPoint != null ? "gui.playertp.point_config.title_edit_colored" : "gui.playertp.point_config.title_add_colored").getString();
            guiGraphics.drawCenteredString(font, title, this.width / 2, sy + 10, TEXT_COLOR);

            String coordText = Component.translatable("gui.playertp.point_config.coords", x, y, z).getString();
            guiGraphics.drawCenteredString(font, coordText, this.width / 2, sy + 28, TEXT_COLOR);

            // Labels for inputs
            guiGraphics.drawString(font, Component.translatable("gui.playertp.point_config.label_icon").getString(), sx + 10, sy + 36, TEXT_COLOR);
            guiGraphics.drawString(font, Component.translatable("gui.playertp.point_config.label_compass_name").getString(), sx + 52, sy + 36, TEXT_COLOR);
            guiGraphics.drawString(font, Component.translatable("gui.playertp.point_config.label_list_name").getString(), sx + 10, sy + 64, TEXT_COLOR);

            // Hint at bottom
            guiGraphics.drawCenteredString(font, Component.translatable("gui.playertp.point_config.hint").getString(), this.width / 2, sy + DIALOG_H - 14, TEXT_COLOR);

            // Render inputs
            iconPickButton.setMessage(Component.literal(iconPreviewLabel()));
            iconPickButton.render(guiGraphics, mouseX, mouseY, partialTick);
            renderNameInput.render(guiGraphics, mouseX, mouseY, partialTick);
            nameInput.render(guiGraphics, mouseX, mouseY, partialTick);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) { confirm(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { cancel(); return true; }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean isPauseScreen() { return false; }

        @Override
        public void onClose() { cancel(); }
    }

    // ═══════════════════════════════════════════════════════════════
    // ConfirmDeleteScreen
    // ═══════════════════════════════════════════════════════════════

    public static class ConfirmDeleteScreen extends Screen {
        private final Screen parent;
        private final String pointName;
        private final Runnable onConfirm;
        private static final int BORDER_COLOR = 0xFF4A90D9;
        private static final int BG_COLOR = 0xCC1A1A2E;
        private static final int DIALOG_W = 260;
        private static final int DIALOG_H = 110;

        public ConfirmDeleteScreen(Screen parent, String pointName, Runnable onConfirm) {
            super(Component.translatable("gui.playertp.confirm_delete.title"));
            this.parent = parent;
            this.pointName = pointName;
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            int sx = (this.width - DIALOG_W) / 2;
            int sy = (this.height - DIALOG_H) / 2;
            this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.confirm_delete"), (btn) -> {
                onConfirm.run();
                if (minecraft != null) minecraft.setScreen(parent);
            }).bounds(sx + 50, sy + 60, 80, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.cancel_green"), (btn) -> {
                if (minecraft != null) minecraft.setScreen(parent);
            }).bounds(sx + 150, sy + 60, 80, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            this.renderBackground(g);
            int sx = (this.width - DIALOG_W) / 2;
            int sy = (this.height - DIALOG_H) / 2;
            g.fill(sx - 2, sy - 2, sx + DIALOG_W + 2, sy + DIALOG_H + 2, BORDER_COLOR);
            g.fill(sx, sy, sx + DIALOG_W, sy + DIALOG_H, BG_COLOR);
            g.drawCenteredString(font, Component.translatable("gui.playertp.confirm_delete.text").getString(), this.width / 2, sy + 14, 0xFFFFFFFF);
            g.drawCenteredString(font, "§7" + pointName, this.width / 2, sy + 36, 0xFFAAAAAA);
            g.drawCenteredString(font, Component.translatable("gui.playertp.confirm_delete.hint").getString(), this.width / 2, sy + DIALOG_H - 14, 0xFF888888);
            super.render(g, mx, my, pt);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                onConfirm.run();
                if (minecraft != null) minecraft.setScreen(parent);
                return true;
            }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                if (minecraft != null) minecraft.setScreen(parent);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean isPauseScreen() { return false; }

        @Override
        public void onClose() {
            if (minecraft != null) minecraft.setScreen(parent);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // RenameScreen (backward compat)
    // ═══════════════════════════════════════════════════════════════

    public static class RenameScreen extends Screen {
        private final TeleportPoint point;
        private final boolean isPublic;
        private EditBox nameInput;

        public RenameScreen(TeleportPoint point, boolean isPublic) {
            super(Component.translatable("gui.playertp.point_config.title_edit"));
            this.point = point;
            this.isPublic = isPublic;
        }

        @Override
        protected void init() {
            int rx = (this.width - 250) / 2;
            int ry = (this.height - 100) / 2;
            nameInput = new EditBox(font, rx + 10, ry + 25, 230, 20, Component.empty());
            nameInput.setValue(point.name);
            nameInput.setFocused(true);
            this.addRenderableWidget(nameInput);
            this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.confirm"), (btn) -> rename()).bounds(rx + 80, ry + 60, 90, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.cancel"), (btn) -> onClose()).bounds(rx + 180, ry + 60, 70, 20).build());
        }

        private void rename() {
            String newName = nameInput.getValue().trim();
            if (!newName.isEmpty()) {
                ModNetwork.sendToServer(new ModNetwork.RenamePointPacket(point.id, newName, point.iconText, isPublic));
            }
            onClose();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(guiGraphics);
            int rx = (this.width - 250) / 2;
            int ry = (this.height - 100) / 2;
            guiGraphics.fill(rx - 2, ry - 2, rx + 252, ry + 92, BORDER_COLOR);
            guiGraphics.fill(rx, ry, rx + 250, ry + 90, BG_COLOR);
            guiGraphics.drawCenteredString(font, Component.translatable("gui.playertp.point_config.title_edit_colored").getString(), this.width / 2, ry + 12, TEXT_COLOR);
            nameInput.render(guiGraphics, mouseX, mouseY, partialTick);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) { rename(); return true; }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean isPauseScreen() { return false; }
    }

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
        private final Button shareButton;
        private final Button renameButton;
        private final Button quickToggleButton;
        private final Button renderButton;

        PointEntry(TeleportPoint point, int y) {
            this.point = point;
            this.y = y;

            upButton = Button.builder(Component.literal("§7▲"), (btn) -> {
                ModNetwork.sendToServer(new ModNetwork.MovePointPacket(point.id, false, true));
            }).bounds(0, 0, 16, 18).build();

            downButton = Button.builder(Component.literal("§7▼"), (btn) -> {
                ModNetwork.sendToServer(new ModNetwork.MovePointPacket(point.id, false, false));
            }).bounds(0, 0, 16, 18).build();

            teleportButton = Button.builder(Component.translatable("gui.playertp.button.teleport"), (btn) -> {
                ModNetwork.sendToServer(new ModNetwork.TeleportToPointPacket(point.id, false));
                minecraft.setScreen(null);
            }).bounds(0, 0, 34, 18).build();

            deleteButton = Button.builder(Component.translatable("gui.playertp.button.delete"), (btn) -> {
                // Delete confirmation
                if (minecraft != null) {
                    minecraft.setScreen(new ConfirmDeleteScreen(PersonalTeleportScreen.this,
                            point.name, () -> {
                        ModNetwork.sendToServer(new ModNetwork.DeletePersonalPointPacket(point.id));
                    }));
                }
            }).bounds(0, 0, 34, 18).build();

            shareButton = Button.builder(Component.translatable("gui.playertp.button.share"), (btn) -> {
                ModNetwork.sendToServer(new ModNetwork.SharePointPacket(point.id));
            }).bounds(0, 0, 34, 18).build();

            renameButton = Button.builder(Component.translatable("gui.playertp.button.rename"), (btn) -> {
                if (minecraft != null) {
                    minecraft.setScreen(new PointConfigScreen(PersonalTeleportScreen.this,
                            point.name, point.x, point.y, point.z, point));
                }
            }).bounds(0, 0, 34, 18).build();

            boolean isQuick = point.id.equals(quickTeleportId);
            quickToggleButton = Button.builder(
                    Component.translatable(isQuick ? "gui.playertp.button.quick_on" : "gui.playertp.button.quick_off"),
                    (btn) -> {
                        if (point.id.equals(quickTeleportId)) {
                            quickTeleportId = null;
                        } else {
                            quickTeleportId = point.id;
                        }
                        // Refresh all entries to update button states
                        if (minecraft != null) {
                            minecraft.setScreen(new PersonalTeleportScreen());
                        }
                    }
            ).bounds(0, 0, 40, 18).build();

            boolean pointVisible = com.example.playertp.render.CompassRenderer.isPointVisible(point.id);
            renderButton = Button.builder(
                    Component.translatable(pointVisible ? "gui.playertp.button.render_on" : "gui.playertp.button.render_off"),
                    (btn) -> {
                        com.example.playertp.render.CompassRenderer.togglePointRender(point.id);
                        if (minecraft != null) {
                            minecraft.setScreen(new PersonalTeleportScreen());
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

            int leftPad = 24; // offset for ▲▼ buttons
            String icon = (point.iconText != null && !point.iconText.isEmpty()) ? point.iconText : "●";
            guiGraphics.drawString(font, "§b" + icon, x + leftPad, y + 10, TEXT_COLOR);
            guiGraphics.drawString(font, "§f" + point.name, x + leftPad + 16, y + 10, TEXT_COLOR);

            String coords = String.format("§6%.0f §b%.0f §a%.0f", point.x, point.y, point.z);
            guiGraphics.drawString(font, coords, x + leftPad + 16, y + 30, TEXT_COLOR);

            String dimName = getDimensionName(point.dimension);
            guiGraphics.drawString(font, "§7" + dimName, x + WIDTH - 160, y + 30, TEXT_COLOR);

            // Uniform right-aligned rows: row1 (y+4) four 34px buttons; row2 (y+27) quick(40)+render(34)
            int btnGap = 1;
            int row1X = x + WIDTH - 4;
            row1X -= 34; renameButton.setX(row1X); renameButton.setY(y + 4);
            renameButton.render(guiGraphics, mouseX, mouseY, partialTick);
            row1X -= btnGap + 34; shareButton.setX(row1X); shareButton.setY(y + 4);
            shareButton.render(guiGraphics, mouseX, mouseY, partialTick);
            row1X -= btnGap + 34; deleteButton.setX(row1X); deleteButton.setY(y + 4);
            deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            row1X -= btnGap + 34; teleportButton.setX(row1X); teleportButton.setY(y + 4);
            teleportButton.render(guiGraphics, mouseX, mouseY, partialTick);

            int row2X = x + WIDTH - 4;
            row2X -= 34; renderButton.setX(row2X); renderButton.setY(y + 27);
            renderButton.render(guiGraphics, mouseX, mouseY, partialTick);
            row2X -= btnGap + 40; quickToggleButton.setX(row2X); quickToggleButton.setY(y + 27);
            quickToggleButton.render(guiGraphics, mouseX, mouseY, partialTick);
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
            // Row 1 (y+4): rename/share/delete/teleport, 34px each
            int btnGap = 1;
            int bx = x + WIDTH - 4;
            bx -= 34;
            if (inRect(mouseX, mouseY, bx, y + 4, 34, 18)) { renameButton.mouseClicked(mouseX, mouseY, button); return true; }
            bx -= btnGap + 34;
            if (inRect(mouseX, mouseY, bx, y + 4, 34, 18)) { shareButton.mouseClicked(mouseX, mouseY, button); return true; }
            bx -= btnGap + 34;
            if (inRect(mouseX, mouseY, bx, y + 4, 34, 18)) { deleteButton.mouseClicked(mouseX, mouseY, button); return true; }
            bx -= btnGap + 34;
            if (inRect(mouseX, mouseY, bx, y + 4, 34, 18)) { teleportButton.mouseClicked(mouseX, mouseY, button); return true; }
            // Row 2 (y+27): render(34) + quick(40)
            bx = x + WIDTH - 4;
            bx -= 34;
            if (inRect(mouseX, mouseY, bx, y + 27, 34, 18)) { renderButton.mouseClicked(mouseX, mouseY, button); return true; }
            bx -= btnGap + 40;
            if (inRect(mouseX, mouseY, bx, y + 27, 40, 18)) { quickToggleButton.mouseClicked(mouseX, mouseY, button); return true; }
            return false;
        }

        private boolean inRect(double mx, double my, int rx, int ry, int rw, int rh) {
            return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
        }
    }
}
