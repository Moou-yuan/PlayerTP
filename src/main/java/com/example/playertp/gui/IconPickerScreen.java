package com.example.playertp.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IconPickerScreen extends Screen {

    private final Screen parent;
    private final Consumer<String> onSelect;
    private final List<Button> iconButtons = new ArrayList<>();
    private static final int BORDER_COLOR = 0xFF4A90D9;
    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int DIALOG_W = 300;
    private static final int DIALOG_H = 250;
    private static final int COLS = 10;
    private static final int ROWS = 5;
    private static final int CELL = 26;
    private static final int PER_PAGE = COLS * ROWS;

    private int page = 0;

    public static final String[] ICONS = {
            "", "💀", "🏠",
            // Minecraft-related elements (front section)
            "🪙", "🔩", "💎", "⛏️", "🪓", "🗡️", "🛡️", "🏹", "🧨", "⚔️",
            "🌊", "🌲", "🏘️", "🚢", "⛵", "⚓", "🏔️", "🌋", "🍄", "🌵",
            "🏖️", "❄️", "🧊", "🔥", "💧", "⚡", "🌙", "☀️", "🌈", "☁️",
            "🐉", "🧟", "👻", "🕷️", "🐺", "🐱", "🐴", "🐢", "🐝", "🐷",
            "🐔", "🐑", "🐮", "🐟", "🐙", "🐰", "🐸", "🦊", "🐻", "🦇",
            "🐦", "🎣", "🍖", "🍗", "🥚", "🍞", "🍎", "🥕", "🥔", "🍉",
            "🎃", "🍯", "🌾", "🍀", "🌸", "🧪", "🔮", "📜", "🗝️", "✨",
            "📦", "🛏️", "🚪", "🪜", "🏰", "⛪", "🗼", "🏭", "🚂", "⛲",
            "⛺", "🎯", "🏁", "👑", "🧭", "🔔", "🎵", "🕯️", "🗿", "⚱️",
            "🧱", "🪨", "🪵", "🔧", "🛢️", "⭐", "❤️", "💚", "💙", "💛",
            "💜", "🎁", "🎈", "🎄", "💣", "🦅", "🦉", "🦈", "🐋", "🦀",
            "🦋", "🐌", "🦂", "🐜", "🪲", "🪃", "🔪", "🍺", "🍵", "🫧",
            "🥩", "🍇", "🌪️", "🌀", "🪟", "🪑", "🎣", "🍬", "🧁", "🍪"
    };

    public IconPickerScreen(Screen parent, Consumer<String> onSelect) {
        super(Component.translatable("gui.playertp.icon_picker.title"));
        this.parent = parent;
        this.onSelect = onSelect;
    }

    private void buildPageButtons() {
        for (Button b : iconButtons) this.removeWidget(b);
        iconButtons.clear();
        int sx = dialogSx();
        int sy = dialogSy();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= ICONS.length) break;
            String label = ICONS[idx].isEmpty() ? Component.translatable("gui.playertp.icon_empty").getString() : ICONS[idx];
            int col = i % COLS;
            int row = i / COLS;
            final int iconIdx = idx;
            Button b = Button.builder(Component.literal(label), (btn) -> {
                        if (onSelect != null) onSelect.accept(ICONS[iconIdx]);
                        if (minecraft != null) minecraft.setScreen(parent);
                    })
                    .bounds(sx + 10 + col * CELL, sy + 26 + row * CELL, CELL - 2, 20)
                    .build();
            iconButtons.add(b);
            this.addRenderableWidget(b);
        }
    }

    private int dialogSx() { return Math.max(2, (this.width - DIALOG_W) / 2); }
    private int dialogSy() { return Math.max(2, (this.height - DIALOG_H) / 2); }
    private int pageCount() { return (ICONS.length + PER_PAGE - 1) / PER_PAGE; }

    @Override
    protected void init() {
        int sx = dialogSx();
        int sy = dialogSy();
        buildPageButtons();

        // Prev page
        this.addRenderableWidget(Button.builder(Component.literal("§7◀"), (btn) -> {
            page = Math.max(0, page - 1);
            buildPageButtons();
        }).bounds(sx + 10, sy + DIALOG_H - 30, 24, 20).build());

        // Next page
        this.addRenderableWidget(Button.builder(Component.literal("§7▶"), (btn) -> {
            page = Math.min(pageCount() - 1, page + 1);
            buildPageButtons();
        }).bounds(sx + DIALOG_W - 34, sy + DIALOG_H - 30, 24, 20).build());

        // Cancel
        this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.cancel"), (btn) -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).bounds(sx + DIALOG_W - 104, sy + DIALOG_H - 30, 56, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int sx = dialogSx();
        int sy = dialogSy();
        g.fill(sx - 2, sy - 2, sx + DIALOG_W + 2, sy + DIALOG_H + 2, BORDER_COLOR);
        g.fill(sx, sy, sx + DIALOG_W, sy + DIALOG_H, BG_COLOR);
        g.drawCenteredString(font, Component.translatable("gui.playertp.icon_picker.title_colored").getString(), this.width / 2, sy + 10, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("gui.playertp.icon_picker.hint").getString(), this.width / 2, sy + DIALOG_H - 14, 0xFF888888);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            page = Math.max(0, page - 1);
            buildPageButtons();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            page = Math.min(pageCount() - 1, page + 1);
            buildPageButtons();
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
