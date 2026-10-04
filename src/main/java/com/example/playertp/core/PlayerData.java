package com.example.playertp.core;

import com.example.playertp.ExampleMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData extends SavedData {

    private static final String DATA_NAME = ExampleMod.MODID + "_player_data";
    private static final String TAG_HIDDEN_PLAYERS = "HiddenPlayers";

    private final Set<UUID> hiddenPlayers = new HashSet<>();

    public PlayerData() {}

    public static PlayerData get(MinecraftServer server) {
        if (server == null) {
            return new PlayerData();
        }
        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(PlayerData::read, PlayerData::new, DATA_NAME);
    }

    public boolean isHidden(UUID playerId) {
        return hiddenPlayers.contains(playerId);
    }

    public void toggleVisibility(UUID playerId) {
        if (hiddenPlayers.contains(playerId)) {
            hiddenPlayers.remove(playerId);
        } else {
            hiddenPlayers.add(playerId);
        }
        setDirty();
    }

    public void setVisibility(UUID playerId, boolean hidden) {
        if (hidden) {
            hiddenPlayers.add(playerId);
        } else {
            hiddenPlayers.remove(playerId);
        }
        setDirty();
    }

    public Set<UUID> getHiddenPlayers() {
        return Collections.unmodifiableSet(hiddenPlayers);
    }

    public static PlayerData read(CompoundTag tag) {
        PlayerData data = new PlayerData();
        ListTag hiddenList = tag.getList(TAG_HIDDEN_PLAYERS, Tag.TAG_INT_ARRAY);
        for (Tag entry : hiddenList) {
            UUID uuid = net.minecraft.nbt.NbtUtils.loadUUID(entry);
            data.hiddenPlayers.add(uuid);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag hiddenList = new ListTag();
        for (UUID uuid : hiddenPlayers) {
            hiddenList.add(net.minecraft.nbt.NbtUtils.createUUID(uuid));
        }
        tag.put(TAG_HIDDEN_PLAYERS, hiddenList);
        return tag;
    }
}