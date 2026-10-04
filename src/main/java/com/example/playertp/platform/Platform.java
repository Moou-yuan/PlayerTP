package com.example.playertp.platform;

public interface Platform {

    boolean isClient();

    boolean isDedicatedServer();

    String getPlatformName();
}