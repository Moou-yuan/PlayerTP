package com.example.playertp.api;

import com.example.playertp.core.PlayerData;
import com.example.playertp.core.TeleportHistory;
import com.example.playertp.core.TeleportHistory.TeleportRecord;
import com.example.playertp.core.TeleportHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerTPAPI {

    private PlayerTPAPI() {}

    public static boolean teleportToPlayer(ServerPlayer source, ServerPlayer target) {
        return TeleportHelper.teleportToPlayer(source, target);
    }

    public static boolean teleportToCoordinates(ServerPlayer player, double x, double y, double z) {
        return TeleportHelper.teleportToCoordinates(player, x, y, z);
    }

    public static boolean teleportToCoordinates(ServerPlayer player, double x, double y, double z, ResourceLocation dimension) {
        return TeleportHelper.teleportToCoordinates(player, x, y, z, dimension);
    }

    public static boolean hasEnoughExperience(ServerPlayer player) {
        return TeleportHelper.hasEnoughExperience(player);
    }

    public static int getRequiredExperience() {
        return TeleportHelper.getRequiredExperience();
    }

    public static List<TeleportRecord> getTeleportHistory(Player player) {
        return TeleportHistory.get(player.getServer()).getHistory(player.getUUID());
    }

    public static void addTeleportRecord(ServerPlayer player, double x, double y, double z, ResourceLocation dimension) {
        TeleportHistory.get(player.getServer()).addRecord(player.getUUID(), x, y, z, dimension);
    }

    public static void clearTeleportHistory(Player player) {
        TeleportHistory.get(player.getServer()).clearHistory(player.getUUID());
    }

    public static boolean isPlayerHidden(Player player, UUID targetPlayerId) {
        return PlayerData.get(player.getServer()).isHidden(targetPlayerId);
    }

    public static void togglePlayerVisibility(Player player, UUID targetPlayerId) {
        PlayerData.get(player.getServer()).toggleVisibility(targetPlayerId);
    }

    public static void setPlayerVisibility(Player player, UUID targetPlayerId, boolean hidden) {
        PlayerData.get(player.getServer()).setVisibility(targetPlayerId, hidden);
    }
}