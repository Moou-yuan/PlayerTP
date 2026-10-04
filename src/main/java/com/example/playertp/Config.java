package com.example.playertp;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue REQUIRE_EXPERIENCE = COMMON_BUILDER
            .comment("Whether teleportation requires experience points")
            .define("requireExperience", false);

    public static final ForgeConfigSpec.IntValue TELEPORT_EXPERIENCE_COST = COMMON_BUILDER
            .comment("Amount of experience points required for teleportation (1-1000)")
            .defineInRange("teleportExperienceCost", 10, 1, 1000);

    static final ForgeConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    public static boolean requireExperience;
    public static int teleportExperienceCost;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == net.minecraftforge.fml.config.ModConfig.Type.COMMON) {
            requireExperience = REQUIRE_EXPERIENCE.get();
            teleportExperienceCost = TELEPORT_EXPERIENCE_COST.get();
        }
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() == net.minecraftforge.fml.config.ModConfig.Type.COMMON) {
            requireExperience = REQUIRE_EXPERIENCE.get();
            teleportExperienceCost = TELEPORT_EXPERIENCE_COST.get();
        }
    }
}