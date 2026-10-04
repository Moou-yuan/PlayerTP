package com.example.playertp.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FMLLoader;

public class ForgePlatform implements Platform {

    public static final ForgePlatform INSTANCE = new ForgePlatform();

    private ForgePlatform() {}

    @Override
    public boolean isClient() {
        return FMLLoader.getDist() == Dist.CLIENT;
    }

    @Override
    public boolean isDedicatedServer() {
        return FMLLoader.getDist() == Dist.DEDICATED_SERVER;
    }

    @Override
    public String getPlatformName() {
        return "Forge";
    }
}