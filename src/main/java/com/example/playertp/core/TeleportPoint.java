package com.example.playertp.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class TeleportPoint {
    public final UUID id;
    public final UUID ownerUUID;
    public final String ownerName;
    public String name;
    public String renderName; // short name shown on the compass (empty = use name)
    public final double x;
    public final double y;
    public final double z;
    public final ResourceLocation dimension;
    public final long timestamp;
    public String iconText; // empty = default diamond marker on compass

    public TeleportPoint(UUID id, UUID ownerUUID, String ownerName, String name,
                         double x, double y, double z, ResourceLocation dimension, long timestamp,
                         String iconText, String renderName) {
        this.id = id;
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.timestamp = timestamp;
        this.iconText = (iconText != null && !iconText.isEmpty()) ? iconText : "";
        this.renderName = (renderName != null && !renderName.isEmpty()) ? renderName : "";
    }

    public static TeleportPoint create(UUID ownerUUID, String ownerName, String name,
                                       double x, double y, double z, ResourceLocation dimension,
                                       String iconText, String renderName) {
        return new TeleportPoint(UUID.randomUUID(), ownerUUID, ownerName, name, x, y, z, dimension,
                System.currentTimeMillis(), iconText, renderName);
    }

    public static TeleportPoint create(UUID ownerUUID, String ownerName, String name,
                                       double x, double y, double z, ResourceLocation dimension,
                                       String iconText) {
        return create(ownerUUID, ownerName, name, x, y, z, dimension, iconText, "");
    }

    public static TeleportPoint create(UUID ownerUUID, String ownerName, String name,
                                       double x, double y, double z, ResourceLocation dimension) {
        return create(ownerUUID, ownerName, name, x, y, z, dimension, null, "");
    }

    public String getCompassName() {
        return (renderName != null && !renderName.isEmpty()) ? renderName : name;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("OwnerUUID", ownerUUID);
        tag.putString("OwnerName", ownerName);
        tag.putString("Name", name);
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Timestamp", timestamp);
        tag.putString("IconText", iconText != null ? iconText : "");
        tag.putString("RenderName", renderName != null ? renderName : "");
        return tag;
    }

    public static TeleportPoint fromNBT(CompoundTag tag) {
        String name = tag.getString("Name");
        String iconText = tag.contains("IconText") ? tag.getString("IconText") : "";
        String renderName = tag.contains("RenderName") ? tag.getString("RenderName") : "";
        return new TeleportPoint(
            tag.getUUID("Id"),
            tag.getUUID("OwnerUUID"),
            tag.getString("OwnerName"),
            name,
            tag.getDouble("X"),
            tag.getDouble("Y"),
            tag.getDouble("Z"),
            ResourceLocation.tryParse(tag.getString("Dimension")) != null
                ? ResourceLocation.tryParse(tag.getString("Dimension"))
                : ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
            tag.getLong("Timestamp"),
            iconText,
            renderName
        );
    }
}
