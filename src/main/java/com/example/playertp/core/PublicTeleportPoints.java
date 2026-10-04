package com.example.playertp.core;

import com.example.playertp.ExampleMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PublicTeleportPoints extends SavedData {

    private static final String DATA_NAME = ExampleMod.MODID + "_public_points";
    private static final String TAG_POINTS = "PublicPoints";

    private final List<TeleportPoint> points = new ArrayList<>();

    public PublicTeleportPoints() {}

    public static PublicTeleportPoints get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(PublicTeleportPoints::read, PublicTeleportPoints::new, DATA_NAME);
    }

    public List<TeleportPoint> getAllPoints() {
        return new ArrayList<>(points);
    }

    public void addPoint(TeleportPoint point) {
        points.add(point);
        setDirty();
    }

    public void removePoint(UUID pointId) {
        points.removeIf(p -> p.id.equals(pointId));
        setDirty();
    }

    public boolean renamePoint(UUID pointId, String newName) {
        for (TeleportPoint point : points) {
            if (point.id.equals(pointId)) {
                point.name = newName;
                setDirty();
                return true;
            }
        }
        return false;
    }

    public boolean updatePointAttributes(UUID pointId, String newName, String iconText, String renderName) {
        for (TeleportPoint point : points) {
            if (point.id.equals(pointId)) {
                point.name = newName;
                point.iconText = iconText != null ? iconText : "";
                point.renderName = renderName != null ? renderName : "";
                setDirty();
                return true;
            }
        }
        return false;
    }

    public boolean containsPoint(UUID pointId) {
        return points.stream().anyMatch(p -> p.id.equals(pointId));
    }

    public void movePoint(UUID pointId, boolean up) {
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).id.equals(pointId)) {
                int target = up ? i - 1 : i + 1;
                if (target >= 0 && target < points.size()) {
                    TeleportPoint tmp = points.get(i);
                    points.set(i, points.get(target));
                    points.set(target, tmp);
                    setDirty();
                }
                return;
            }
        }
    }

    public static PublicTeleportPoints read(CompoundTag tag) {
        PublicTeleportPoints data = new PublicTeleportPoints();
        ListTag pointsList = tag.getList(TAG_POINTS, Tag.TAG_COMPOUND);
        for (Tag entry : pointsList) {
            data.points.add(TeleportPoint.fromNBT((CompoundTag) entry));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag pointsList = new ListTag();
        for (TeleportPoint point : points) {
            pointsList.add(point.toNBT());
        }
        tag.put(TAG_POINTS, pointsList);
        return tag;
    }
}