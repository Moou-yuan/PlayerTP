package com.example.playertp.core;

import com.example.playertp.ExampleMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.stream.Collectors;

public class TeleportHistory extends SavedData {

    private static final String DATA_NAME = ExampleMod.MODID + "_teleport_history";
    private static final String TAG_HISTORY = "TeleportHistory";
    private static final int MAX_RECORDS_PER_PLAYER = 10;

    private final Map<UUID, List<TeleportRecord>> historyMap = new HashMap<>();

    public TeleportHistory() {}

    public static TeleportHistory get(MinecraftServer server) {
        if (server == null) {
            return new TeleportHistory();
        }
        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(TeleportHistory::read, TeleportHistory::new, DATA_NAME);
    }

    public List<TeleportRecord> getHistory(UUID playerId) {
        return historyMap.getOrDefault(playerId, Collections.emptyList());
    }

    public void addRecord(UUID playerId, double x, double y, double z, ResourceLocation dimension) {
        historyMap.computeIfAbsent(playerId, k -> new ArrayList<>());
        List<TeleportRecord> records = historyMap.get(playerId);
        
        records.add(0, new TeleportRecord(x, y, z, dimension, System.currentTimeMillis()));
        
        while (records.size() > MAX_RECORDS_PER_PLAYER) {
            records.remove(records.size() - 1);
        }
        
        setDirty();
    }

    public void clearHistory(UUID playerId) {
        historyMap.remove(playerId);
        setDirty();
    }

    public static TeleportHistory read(CompoundTag tag) {
        TeleportHistory history = new TeleportHistory();
        CompoundTag historyTag = tag.getCompound(TAG_HISTORY);
        
        for (String key : historyTag.getAllKeys()) {
            UUID playerId = UUID.fromString(key);
            ListTag recordsList = historyTag.getList(key, Tag.TAG_COMPOUND);
            
            List<TeleportRecord> records = new ArrayList<>();
            for (Tag entry : recordsList) {
                CompoundTag recordTag = (CompoundTag) entry;
                double x = recordTag.getDouble("X");
                double y = recordTag.getDouble("Y");
                double z = recordTag.getDouble("Z");
                ResourceLocation dimension = ResourceLocation.tryParse(recordTag.getString("Dimension")) != null ? ResourceLocation.tryParse(recordTag.getString("Dimension")) : ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
                long timestamp = recordTag.getLong("Timestamp");
                records.add(new TeleportRecord(x, y, z, dimension, timestamp));
            }
            
            history.historyMap.put(playerId, records);
        }
        
        return history;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag historyTag = new CompoundTag();
        
        for (Map.Entry<UUID, List<TeleportRecord>> entry : historyMap.entrySet()) {
            ListTag recordsList = new ListTag();
            for (TeleportRecord record : entry.getValue()) {
                CompoundTag recordTag = new CompoundTag();
                recordTag.putDouble("X", record.x);
                recordTag.putDouble("Y", record.y);
                recordTag.putDouble("Z", record.z);
                recordTag.putString("Dimension", record.dimension.toString());
                recordTag.putLong("Timestamp", record.timestamp);
                recordsList.add(recordTag);
            }
            historyTag.put(entry.getKey().toString(), recordsList);
        }
        
        tag.put(TAG_HISTORY, historyTag);
        return tag;
    }

    public static class TeleportRecord {
        public final double x;
        public final double y;
        public final double z;
        public final ResourceLocation dimension;
        public final long timestamp;

        public TeleportRecord(double x, double y, double z, ResourceLocation dimension, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.timestamp = timestamp;
        }
    }
}