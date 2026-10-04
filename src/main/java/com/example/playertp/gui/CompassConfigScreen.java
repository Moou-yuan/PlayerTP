package com.example.playertp.gui;

import com.example.playertp.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class CompassConfigScreen extends Screen {

    private final Screen parent;
    private static final int BORDER_COLOR = 0xFF4A90D9;
    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DIALOG_W = 300;
    private static final int DIALOG_H = 236;

    // Working copies (applied only on confirm)
    private boolean renderCompass;
    private int widthPercent;
    private int height;
    private int opacity;
    private int iconSize;
    private double distTextSize;
    private int distColorIdx;

    private static final String[] COLOR_NAMES = {"gui.playertp.compass.color.gray", "gui.playertp.compass.color.white", "gui.playertp.compass.color.yellow", "gui.playertp.compass.color.red", "gui.playertp.compass.color.green", "gui.playertp.compass.color.blue"};

    public CompassConfigScreen(Screen parent) {
        super(Component.translatable("gui.playertp.compass.title"));
        this.parent = parent;
        this.renderCompass = ClientConfig.renderCompass.get();
        this.widthPercent = ClientConfig.compassWidthPercent.get();
        this.height = ClientConfig.compassHeight.get();
        this.opacity = ClientConfig.compassOpacity.get();
        this.iconSize = ClientConfig.compassIconSize.get();
        this.distTextSize = ClientConfig.compassDistTextSize.get();
        this.distColorIdx = ClientConfig.compassDistTextColorIdx.get();
    }

    private int dialogSx() { return Math.max(2, (this.width - DIALOG_W) / 2); }
    private int dialogSy() { return Math.max(2, (this.height - DIALOG_H) / 2); }

    @Override
    protected void init() {
        int sx = dialogSx();
        int sy = dialogSy();
        int rowY = sy + 26;
        int minusX = sx + DIALOG_W - 90;
        int plusX = sx + DIALOG_W - 44;

        // Row 1: render toggle
        this.addRenderableWidget(Button.builder(
                Component.translatable(renderCompass ? "gui.playertp.compass.render_on" : "gui.playertp.compass.render_off"),
                (btn) -> {
                    renderCompass = !renderCompass;
                    btn.setMessage(Component.translatable(renderCompass ? "gui.playertp.compass.render_on" : "gui.playertp.compass.render_off"));
                }).bounds(sx + 14, rowY, 100, 20).build());

        // Rows 2-7: +/- steppers
        this.addRenderableWidget(Button.builder(Component.literal("§c-"), (btn) -> {
            widthPercent = Math.max(40, widthPercent - 10);
        }).bounds(minusX, rowY + 25, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§a+"), (btn) -> {
            widthPercent = Math.min(100, widthPercent + 10);
        }).bounds(plusX, rowY + 25, 26, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c-"), (btn) -> {
            height = Math.max(8, height - 2);
        }).bounds(minusX, rowY + 50, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§a+"), (btn) -> {
            height = Math.min(30, height + 2);
        }).bounds(plusX, rowY + 50, 26, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c-"), (btn) -> {
            opacity = Math.max(10, opacity - 5);
        }).bounds(minusX, rowY + 75, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§a+"), (btn) -> {
            opacity = Math.min(90, opacity + 5);
        }).bounds(plusX, rowY + 75, 26, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c-"), (btn) -> {
            iconSize = Math.max(3, iconSize - 1);
        }).bounds(minusX, rowY + 100, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§a+"), (btn) -> {
            iconSize = Math.min(10, iconSize + 1);
        }).bounds(plusX, rowY + 100, 26, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c-"), (btn) -> {
            distTextSize = Math.max(0.4, distTextSize - 0.1);
        }).bounds(minusX, rowY + 125, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("§a+"), (btn) -> {
            distTextSize = Math.min(1.0, distTextSize + 0.1);
        }).bounds(plusX, rowY + 125, 26, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.color"), (btn) -> {
            distColorIdx = (distColorIdx + 1) % COLOR_NAMES.length;
        }).bounds(minusX, rowY + 150, 46, 20).build());

        // Bottom buttons
        this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.confirm"), (btn) -> confirm())
                .bounds(sx + 60, sy + DIALOG_H - 34, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.playertp.button.cancel"), (btn) -> cancel())
                .bounds(sx + 160, sy + DIALOG_H - 34, 80, 20).build());
    }

    private void confirm() {
        ClientConfig.renderCompass.set(renderCompass);
        ClientConfig.compassWidthPercent.set(widthPercent);
        ClientConfig.compassHeight.set(height);
        ClientConfig.compassOpacity.set(opacity);
        ClientConfig.compassIconSize.set(iconSize);
        ClientConfig.compassDistTextSize.set(distTextSize);
        ClientConfig.compassDistTextColorIdx.set(distColorIdx);
        ClientConfig.CLIENT_SPEC.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void cancel() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int sx = dialogSx();
        int sy = dialogSy();
        int rowY = sy + 26;
        int labelX = sx + 14;

        g.fill(sx - 2, sy - 2, sx + DIALOG_W + 2, sy + DIALOG_H + 2, BORDER_COLOR);
        g.fill(sx, sy, sx + DIALOG_W, sy + DIALOG_H, BG_COLOR);

        g.drawCenteredString(font, Component.translatable("gui.playertp.compass.title_colored").getString(), this.width / 2, sy + 8, TEXT_COLOR);

        // Labels (aligned with each row's buttons)
        g.drawString(font, Component.translatable("gui.playertp.compass.label_width", widthPercent).getString(), labelX, rowY + 31, TEXT_COLOR);
        g.drawString(font, Component.translatable("gui.playertp.compass.label_height", height).getString(), labelX, rowY + 56, TEXT_COLOR);
        g.drawString(font, Component.translatable("gui.playertp.compass.label_opacity", opacity).getString(), labelX, rowY + 81, TEXT_COLOR);
        g.drawString(font, Component.translatable("gui.playertp.compass.label_icon_size", iconSize).getString(), labelX, rowY + 106, TEXT_COLOR);
        g.drawString(font, Component.translatable("gui.playertp.compass.label_dist_text_size", distTextSize).getString(), labelX, rowY + 131, TEXT_COLOR);
        g.drawString(font, Component.translatable("gui.playertp.compass.label_dist_color", Component.translatable(COLOR_NAMES[distColorIdx]).getString()).getString(), labelX, rowY + 156, TEXT_COLOR);

        g.drawCenteredString(font, Component.translatable("gui.playertp.compass.hint").getString(), this.width / 2, sy + DIALOG_H - 14, 0xFF888888);

        super.render(g, mouseX, mouseY, partialTick);
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
