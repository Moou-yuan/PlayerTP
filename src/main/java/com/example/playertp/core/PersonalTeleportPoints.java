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

public class PersonalTeleportPoints extends SavedData {

    private static final String DATA_NAME = ExampleMod.MODID + "_personal_points";
    private static final String TAG_POINTS = "PersonalPoints";

    private final List<TeleportPoint> points = new ArrayList<>();

    public PersonalTeleportPoints() {}

    public static PersonalTeleportPoints get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(PersonalTeleportPoints::read, PersonalTeleportPoints::new, DATA_NAME);
    }

    public List<TeleportPoint> getPointsByPlayer(UUID playerUUID) {
        List<TeleportPoint> result = new ArrayList<>();
        for (TeleportPoint point : points) {
            if (point.ownerUUID.equals(playerUUID)) {
                result.add(point);
            }
        }
        return result;
    }

    public void addPoint(TeleportPoint point) {
        points.add(point);
        setDirty();
    }

    public void removePoint(UUID pointId, UUID playerUUID) {
        points.removeIf(p -> p.id.equals(pointId) && p.ownerUUID.equals(playerUUID));
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

    public void movePoint(UUID pointId, UUID playerUUID, boolean up) {
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).id.equals(pointId) && points.get(i).ownerUUID.equals(playerUUID)) {
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

    public static PersonalTeleportPoints read(CompoundTag tag) {
        PersonalTeleportPoints data = new PersonalTeleportPoints();
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