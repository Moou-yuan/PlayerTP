package com.example.playertp.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "playertp", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientConfig {
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue playerListKey = CLIENT_BUILDER
            .comment("Default key code for opening the player list (GLFW key code, default: EQUAL = 61)")
            .defineInRange("playerListKey", GLFW.GLFW_KEY_EQUAL, 0, 350);

    public static final ForgeConfigSpec.IntValue quickAddKey = CLIENT_BUILDER
            .comment("Default key code for quick-adding a teleport point (GLFW key code, default: MINUS = 45)")
            .defineInRange("quickAddKey", GLFW.GLFW_KEY_MINUS, 0, 350);

    public static final ForgeConfigSpec.BooleanValue renderPersonalPoints = CLIENT_BUILDER
            .comment("Whether to show personal teleport point markers on the compass HUD")
            .define("renderPersonalPoints", true);

    public static final ForgeConfigSpec.BooleanValue renderPublicPoints = CLIENT_BUILDER
            .comment("Whether to show public teleport point markers on the compass HUD")
            .define("renderPublicPoints", true);

    public static final ForgeConfigSpec.IntValue beaconMaxDistance = CLIENT_BUILDER
            .comment("Maximum distance to show teleport point markers (in blocks)")
            .defineInRange("beaconMaxDistance", 10000, 100, 100000);

    public static final ForgeConfigSpec.BooleanValue renderCompass = CLIENT_BUILDER
            .comment("Whether to render the compass HUD at the top of the screen")
            .define("renderCompass", true);

    // ── Compass appearance (editable in CompassConfigScreen) ──────
    public static final ForgeConfigSpec.IntValue compassWidthPercent = CLIENT_BUILDER
            .comment("Compass bar width as percentage of screen width (40-100)")
            .defineInRange("compassWidthPercent", 100, 40, 100);

    public static final ForgeConfigSpec.IntValue compassHeight = CLIENT_BUILDER
            .comment("Compass bar height in pixels (8-30)")
            .defineInRange("compassHeight", 12, 8, 30);

    public static final ForgeConfigSpec.IntValue compassOpacity = CLIENT_BUILDER
            .comment("Compass bar background opacity percent (10-90)")
            .defineInRange("compassOpacity", 31, 10, 90);

    public static final ForgeConfigSpec.IntValue compassIconSize = CLIENT_BUILDER
            .comment("Teleport point diamond icon half-size in pixels (3-10)")
            .defineInRange("compassIconSize", 4, 3, 10);

    public static final ForgeConfigSpec.DoubleValue compassDistTextSize = CLIENT_BUILDER
            .comment("Distance text scale factor (0.4-1.0)")
            .defineInRange("compassDistTextSize", 0.6, 0.4, 1.0);

    public static final ForgeConfigSpec.IntValue compassDistTextColorIdx = CLIENT_BUILDER
            .comment("Distance text color index: 0=gray 1=white 2=yellow 3=red 4=green 5=blue")
            .defineInRange("compassDistTextColorIdx", 0, 0, 5);

    public static final ForgeConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
}