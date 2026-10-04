package com.example.playertp.core;

import com.example.playertp.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class TeleportHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportHelper.class);

    public static boolean teleportToCoordinates(ServerPlayer player, double x, double y, double z, ResourceLocation dimension) {
        if (player == null) {
            return false;
        }

        if (Config.requireExperience) {
            int currentExp = getPlayerTotalExperience(player);
            if (currentExp < Config.teleportExperienceCost) {
                player.sendSystemMessage(Component.translatable("message.playertp.insufficient_exp", Config.teleportExperienceCost).withStyle(ChatFormatting.RED));
                return false;
            }

            removeExperience(player, Config.teleportExperienceCost);
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimension);
        ServerLevel targetLevel = player.server.getLevel(dimensionKey);
        
        if (targetLevel == null) {
            player.sendSystemMessage(Component.translatable("message.playertp.invalid_dimension").withStyle(ChatFormatting.RED));
            return false;
        }

        ChunkPos chunkPos = new ChunkPos(BlockPos.containing(x, y, z));
        targetLevel.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.POST_TELEPORT, chunkPos, 1, player.getId());

        player.stopRiding();
        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }

        if (targetLevel == player.level()) {
            player.connection.teleport(x, y, z, player.getYRot(), player.getXRot(), Collections.emptySet());
        } else {
            player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
        }

        player.setYHeadRot(player.getYRot());
        player.sendSystemMessage(Component.translatable("message.playertp.teleport_success", x, y, z).withStyle(ChatFormatting.GREEN));

        LOGGER.info("Player {} teleported to coordinates {}, {}, {} in dimension {}", player.getName().getString(), x, y, z, dimension);
        
        TeleportHistory.get(player.server).addRecord(player.getUUID(), x, y, z, dimension);
        
        return true;
    }

    public static boolean teleportToCoordinates(ServerPlayer player, double x, double y, double z) {
        return teleportToCoordinates(player, x, y, z, player.level().dimension().location());
    }

    public static boolean teleportToPlayer(ServerPlayer source, ServerPlayer target) {
        return teleportToCoordinates(source, target.getX(), target.getY(), target.getZ(), target.level().dimension().location());
    }

    public static boolean hasEnoughExperience(ServerPlayer player) {
        if (!Config.requireExperience) {
            return true;
        }
        return getPlayerTotalExperience(player) >= Config.teleportExperienceCost;
    }

    public static int getRequiredExperience() {
        return Config.teleportExperienceCost;
    }

    private static int getPlayerTotalExperience(ServerPlayer player) {
        int exp = player.experienceLevel * 7;
        exp += Math.round(player.experienceProgress * 7);
        return exp;
    }

    private static void removeExperience(ServerPlayer player, int amount) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        int totalExp = level * 7 + Math.round(progress * 7);

        totalExp -= amount;

        if (totalExp < 0) {
            totalExp = 0;
        }

        player.experienceLevel = totalExp / 7;
        player.experienceProgress = (float) (totalExp % 7) / 7.0F;
    }
}