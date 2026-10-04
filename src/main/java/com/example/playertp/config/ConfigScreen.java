package com.example.playertp.config;

import com.example.playertp.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;
    private final List<ConfigEntry> entries = new ArrayList<>();
    private static final int ENTRY_HEIGHT = 25;

    public ConfigScreen(Screen parent, ModConfig config) {
        super(Component.translatable("playertp.config.title"));
        this.parent = parent;
        this.config = config;
        loadEntries();
    }

    private void loadEntries() {
        entries.clear();
        
        entries.add(new ConfigEntry(
            "requireExperience",
            Component.translatable("playertp.config.require_experience"),
            Component.translatable("playertp.config.require_experience.tooltip"),
            Config.requireExperience));
        
        entries.add(new ConfigEntry(
            "teleportExperienceCost",
            Component.translatable("playertp.config.teleport_experience_cost"),
            Component.translatable("playertp.config.teleport_experience_cost.tooltip"),
            Config.teleportExperienceCost));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        int x = this.width / 2 - 200;
        int y = 30;
        
        guiGraphics.drawCenteredString(this.font, Component.translatable("playertp.config.title"), this.width / 2, 10, 0xFFFFFF);
        
        for (int i = 0; i < entries.size(); i++) {
            ConfigEntry entry = entries.get(i);
            int entryY = y + i * ENTRY_HEIGHT;
            
            guiGraphics.drawString(this.font, entry.label.getString(), x, entryY + 5, 0xFFFFFF);
            
            if (entry.isBoolean) {
                String currentValue = entry.getBooleanValue() ? "ON" : "OFF";
                guiGraphics.drawString(this.font, currentValue, x + 200, entryY + 5, 
                    entry.getBooleanValue() ? 0x55FF55 : 0xFF5555);
            } else {
                guiGraphics.drawString(this.font, String.valueOf(entry.getIntValue()), x + 200, entryY + 5, 0xFFFFFF);
            }
            
            guiGraphics.drawString(this.font, entry.tooltip.getString(), x, entryY + 15, 0x888888);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = this.width / 2 - 200;
            int y = 30;
            
            for (int i = 0; i < entries.size(); i++) {
                ConfigEntry entry = entries.get(i);
                int entryY = y + i * ENTRY_HEIGHT;
                
                if (mouseX >= x + 200 && mouseX <= x + 250 &&
                    mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT) {
                    
                    if (entry.isBoolean) {
                        entry.setBooleanValue(!entry.getBooleanValue());
                    }
                    saveConfig();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void saveConfig() {
        if (config.getSpec() != null) {
            config.save();
        }
    }

    @Override
    public void onClose() {
        saveConfig();
        Minecraft.getInstance().setScreen(parent);
    }

    private static class ConfigEntry {
        final String key;
        final Component label;
        final Component tooltip;
        final boolean isBoolean;
        private boolean booleanValue;
        private int intValue;

        ConfigEntry(String key, Component label, Component tooltip, boolean defaultValue) {
            this.key = key;
            this.label = label;
            this.tooltip = tooltip;
            this.isBoolean = true;
            this.booleanValue = defaultValue;
        }

        ConfigEntry(String key, Component label, Component tooltip, int defaultValue) {
            this.key = key;
            this.label = label;
            this.tooltip = tooltip;
            this.isBoolean = false;
            this.intValue = defaultValue;
        }

        boolean getBooleanValue() {
            return booleanValue;
        }

        void setBooleanValue(boolean value) {
            this.booleanValue = value;
            if (key.equals("requireExperience")) {
                Config.requireExperience = value;
            }
        }

        int getIntValue() {
            return intValue;
        }
    }
}
