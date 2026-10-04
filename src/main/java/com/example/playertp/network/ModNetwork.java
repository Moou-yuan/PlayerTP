package com.example.playertp.network;

import com.example.playertp.core.PersonalTeleportPoints;
import com.example.playertp.core.PublicTeleportPoints;
import com.example.playertp.core.TeleportHelper;
import com.example.playertp.core.TeleportPoint;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("playertp", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, AddPersonalPointPacket.class, AddPersonalPointPacket::encode, AddPersonalPointPacket::decode, AddPersonalPointPacket::handle);
        INSTANCE.registerMessage(id++, DeletePersonalPointPacket.class, DeletePersonalPointPacket::encode, DeletePersonalPointPacket::decode, DeletePersonalPointPacket::handle);
        INSTANCE.registerMessage(id++, RenamePointPacket.class, RenamePointPacket::encode, RenamePointPacket::decode, RenamePointPacket::handle);
        INSTANCE.registerMessage(id++, SharePointPacket.class, SharePointPacket::encode, SharePointPacket::decode, SharePointPacket::handle);
        INSTANCE.registerMessage(id++, DeletePublicPointPacket.class, DeletePublicPointPacket::encode, DeletePublicPointPacket::decode, DeletePublicPointPacket::handle);
        INSTANCE.registerMessage(id++, SyncPersonalPointsPacket.class, SyncPersonalPointsPacket::encode, SyncPersonalPointsPacket::decode, SyncPersonalPointsPacket::handle);
        INSTANCE.registerMessage(id++, SyncPublicPointsPacket.class, SyncPublicPointsPacket::encode, SyncPublicPointsPacket::decode, SyncPublicPointsPacket::handle);
        INSTANCE.registerMessage(id++, RequestSyncPointsPacket.class, RequestSyncPointsPacket::encode, RequestSyncPointsPacket::decode, RequestSyncPointsPacket::handle);
        INSTANCE.registerMessage(id++, TeleportToPointPacket.class, TeleportToPointPacket::encode, TeleportToPointPacket::decode, TeleportToPointPacket::handle);
        INSTANCE.registerMessage(id++, QuickAddPointPacket.class, QuickAddPointPacket::encode, QuickAddPointPacket::decode, QuickAddPointPacket::handle);
        INSTANCE.registerMessage(id++, RequestPlayerListPacket.class, RequestPlayerListPacket::encode, RequestPlayerListPacket::decode, RequestPlayerListPacket::handle);
        INSTANCE.registerMessage(id++, SyncPlayerListPacket.class, SyncPlayerListPacket::encode, SyncPlayerListPacket::decode, SyncPlayerListPacket::handle);
        INSTANCE.registerMessage(id++, ToggleVisibilityPacket.class, ToggleVisibilityPacket::encode, ToggleVisibilityPacket::decode, ToggleVisibilityPacket::handle);
        INSTANCE.registerMessage(id++, TeleportToPlayerPacket.class, TeleportToPlayerPacket::encode, TeleportToPlayerPacket::decode, TeleportToPlayerPacket::handle);
        INSTANCE.registerMessage(id++, SyncHiddenPlayersPacket.class, SyncHiddenPlayersPacket::encode, SyncHiddenPlayersPacket::decode, SyncHiddenPlayersPacket::handle);
        INSTANCE.registerMessage(id++, SyncAllPlayerPositionsPacket.class, SyncAllPlayerPositionsPacket::encode, SyncAllPlayerPositionsPacket::decode, SyncAllPlayerPositionsPacket::handle);
        INSTANCE.registerMessage(id++, RefreshCompassSkinsPacket.class, RefreshCompassSkinsPacket::encode, RefreshCompassSkinsPacket::decode, RefreshCompassSkinsPacket::handle);
        INSTANCE.registerMessage(id++, MovePointPacket.class, MovePointPacket::encode, MovePointPacket::decode, MovePointPacket::handle);
    }

    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToAll(Object message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static class AddPersonalPointPacket {
        private final String name;
        private final String iconText;
        private final String renderName;

        public AddPersonalPointPacket(String name, String iconText, String renderName) {
            this.name = name;
            this.iconText = iconText;
            this.renderName = renderName;
        }

        public AddPersonalPointPacket(String name, String iconText) {
            this(name, iconText, "");
        }

        public AddPersonalPointPacket(String name) {
            this(name, "", "");
        }

        public static void encode(AddPersonalPointPacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.name);
            buf.writeUtf(msg.iconText != null ? msg.iconText : "");
            buf.writeUtf(msg.renderName != null ? msg.renderName : "");
        }

        public static AddPersonalPointPacket decode(FriendlyByteBuf buf) {
            return new AddPersonalPointPacket(buf.readUtf(), buf.readUtf(), buf.readUtf());
        }

        public static void handle(AddPersonalPointPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                TeleportPoint point = TeleportPoint.create(
                        player.getUUID(),
                        player.getName().getString(),
                        msg.name,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.level().dimension().location(),
                        msg.iconText,
                        msg.renderName
                );

                PersonalTeleportPoints.get(player.server).addPoint(point);
                player.sendSystemMessage(Component.translatable("message.playertp.point_added", msg.name));

                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPersonalPointsPacket(
                        PersonalTeleportPoints.get(player.server).getPointsByPlayer(player.getUUID())
                ));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class DeletePersonalPointPacket {
        private final UUID pointId;

        public DeletePersonalPointPacket(UUID pointId) {
            this.pointId = pointId;
        }

        public static void encode(DeletePersonalPointPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.pointId);
        }

        public static DeletePersonalPointPacket decode(FriendlyByteBuf buf) {
            return new DeletePersonalPointPacket(buf.readUUID());
        }

        public static void handle(DeletePersonalPointPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PersonalTeleportPoints.get(player.server).removePoint(msg.pointId, player.getUUID());

                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPersonalPointsPacket(
                        PersonalTeleportPoints.get(player.server).getPointsByPlayer(player.getUUID())
                ));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class RenamePointPacket {
        private final UUID pointId;
        private final String newName;
        private final String iconText;
        private final String renderName;
        private final boolean isPublic;

        public RenamePointPacket(UUID pointId, String newName, String iconText, String renderName, boolean isPublic) {
            this.pointId = pointId;
            this.newName = newName;
            this.iconText = iconText;
            this.renderName = renderName;
            this.isPublic = isPublic;
        }

        public RenamePointPacket(UUID pointId, String newName, String iconText, boolean isPublic) {
            this(pointId, newName, iconText, "", isPublic);
        }

        public RenamePointPacket(UUID pointId, String newName, boolean isPublic) {
            this(pointId, newName, "", "", isPublic);
        }

        public static void encode(RenamePointPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.pointId);
            buf.writeUtf(msg.newName);
            buf.writeUtf(msg.iconText != null ? msg.iconText : "");
            buf.writeUtf(msg.renderName != null ? msg.renderName : "");
            buf.writeBoolean(msg.isPublic);
        }

        public static RenamePointPacket decode(FriendlyByteBuf buf) {
            return new RenamePointPacket(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
        }

        public static void handle(RenamePointPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                if (msg.isPublic) {
                    PublicTeleportPoints.get(player.server).updatePointAttributes(msg.pointId, msg.newName, msg.iconText, msg.renderName);
                    INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncPublicPointsPacket(
                            PublicTeleportPoints.get(player.server).getAllPoints()
                    ));
                } else {
                    PersonalTeleportPoints.get(player.server).updatePointAttributes(msg.pointId, msg.newName, msg.iconText, msg.renderName);
                    INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPersonalPointsPacket(
                            PersonalTeleportPoints.get(player.server).getPointsByPlayer(player.getUUID())
                    ));
                    // Sync matching shared public point (same UUID) to ALL players
                    if (PublicTeleportPoints.get(player.server).updatePointAttributes(msg.pointId, msg.newName, msg.iconText, msg.renderName)) {
                        INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncPublicPointsPacket(
                                PublicTeleportPoints.get(player.server).getAllPoints()
                        ));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SharePointPacket {
        private final UUID pointId;

        public SharePointPacket(UUID pointId) {
            this.pointId = pointId;
        }

        public static void encode(SharePointPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.pointId);
        }

        public static SharePointPacket decode(FriendlyByteBuf buf) {
            return new SharePointPacket(buf.readUUID());
        }

        public static void handle(SharePointPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PersonalTeleportPoints personalData = PersonalTeleportPoints.get(player.server);
                PublicTeleportPoints publicData = PublicTeleportPoints.get(player.server);

                for (TeleportPoint point : personalData.getPointsByPlayer(player.getUUID())) {
                    if (point.id.equals(msg.pointId)) {
                        // Copy to public list — keep the original in personal list
                        publicData.addPoint(point);

                        player.sendSystemMessage(Component.translatable("message.playertp.point_shared"));

                        // Only sync public points — personal list is unchanged
                        INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncPublicPointsPacket(
                                PublicTeleportPoints.get(player.server).getAllPoints()
                        ));
                        return;
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class DeletePublicPointPacket {
        private final UUID pointId;

        public DeletePublicPointPacket(UUID pointId) {
            this.pointId = pointId;
        }

        public static void encode(DeletePublicPointPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.pointId);
        }

        public static DeletePublicPointPacket decode(FriendlyByteBuf buf) {
            return new DeletePublicPointPacket(buf.readUUID());
        }

        public static void handle(DeletePublicPointPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PublicTeleportPoints.get(player.server).removePoint(msg.pointId);

                INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncPublicPointsPacket(
                        PublicTeleportPoints.get(player.server).getAllPoints()
                ));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SyncPersonalPointsPacket {
        private final List<TeleportPoint> points;

        public SyncPersonalPointsPacket(List<TeleportPoint> points) {
            this.points = points;
        }

        public static void encode(SyncPersonalPointsPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.points.size());
            for (TeleportPoint point : msg.points) {
                buf.writeUUID(point.id);
                buf.writeUUID(point.ownerUUID);
                buf.writeUtf(point.ownerName);
                buf.writeUtf(point.name);
                buf.writeDouble(point.x);
                buf.writeDouble(point.y);
                buf.writeDouble(point.z);
                buf.writeResourceLocation(point.dimension);
                buf.writeLong(point.timestamp);
                buf.writeUtf(point.iconText != null ? point.iconText : "");
                buf.writeUtf(point.renderName != null ? point.renderName : "");
            }
        }

        public static SyncPersonalPointsPacket decode(FriendlyByteBuf buf) {
            List<TeleportPoint> points = new ArrayList<>();
            int size = buf.readInt();
            for (int i = 0; i < size; i++) {
                points.add(new TeleportPoint(
                        buf.readUUID(),
                        buf.readUUID(),
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readResourceLocation(),
                        buf.readLong(),
                        buf.readUtf(),
                        buf.readUtf()
                ));
            }
            return new SyncPersonalPointsPacket(points);
        }

        public static void handle(SyncPersonalPointsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                com.example.playertp.gui.PersonalTeleportScreen.updatePoints(msg.points);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SyncPublicPointsPacket {
        private final List<TeleportPoint> points;

        public SyncPublicPointsPacket(List<TeleportPoint> points) {
            this.points = points;
        }

        public static void encode(SyncPublicPointsPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.points.size());
            for (TeleportPoint point : msg.points) {
                buf.writeUUID(point.id);
                buf.writeUUID(point.ownerUUID);
                buf.writeUtf(point.ownerName);
                buf.writeUtf(point.name);
                buf.writeDouble(point.x);
                buf.writeDouble(point.y);
                buf.writeDouble(point.z);
                buf.writeResourceLocation(point.dimension);
                buf.writeLong(point.timestamp);
                buf.writeUtf(point.iconText != null ? point.iconText : "");
                buf.writeUtf(point.renderName != null ? point.renderName : "");
            }
        }

        public static SyncPublicPointsPacket decode(FriendlyByteBuf buf) {
            List<TeleportPoint> points = new ArrayList<>();
            int size = buf.readInt();
            for (int i = 0; i < size; i++) {
                points.add(new TeleportPoint(
                        buf.readUUID(),
                        buf.readUUID(),
                        buf.readUtf(),
                        buf.readUtf(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readResourceLocation(),
                        buf.readLong(),
                        buf.readUtf(),
                        buf.readUtf()
                ));
            }
            return new SyncPublicPointsPacket(points);
        }

        public static void handle(SyncPublicPointsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                com.example.playertp.gui.PublicTeleportScreen.updatePoints(msg.points);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class RequestSyncPointsPacket {
        public RequestSyncPointsPacket() {}

        public static void encode(RequestSyncPointsPacket msg, FriendlyByteBuf buf) {}

        public static RequestSyncPointsPacket decode(FriendlyByteBuf buf) {
            return new RequestSyncPointsPacket();
        }

        public static void handle(RequestSyncPointsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPersonalPointsPacket(
                        PersonalTeleportPoints.get(player.server).getPointsByPlayer(player.getUUID())
                ));
                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPublicPointsPacket(
                        PublicTeleportPoints.get(player.server).getAllPoints()
                ));
                // Also sync hidden-players state for compass
                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncHiddenPlayersPacket(
                        com.example.playertp.core.PlayerData.get(player.server).getHiddenPlayers()
                ));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class TeleportToPointPacket {
        private final UUID pointId;
        private final boolean isPublic;

        public TeleportToPointPacket(UUID pointId, boolean isPublic) {
            this.pointId = pointId;
            this.isPublic = isPublic;
        }

        public static void encode(TeleportToPointPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.pointId);
            buf.writeBoolean(msg.isPublic);
        }

        public static TeleportToPointPacket decode(FriendlyByteBuf buf) {
            return new TeleportToPointPacket(buf.readUUID(), buf.readBoolean());
        }

        public static void handle(TeleportToPointPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                List<TeleportPoint> points = msg.isPublic
                        ? PublicTeleportPoints.get(player.server).getAllPoints()
                        : PersonalTeleportPoints.get(player.server).getPointsByPlayer(player.getUUID());

                for (TeleportPoint point : points) {
                    if (point.id.equals(msg.pointId)) {
                        TeleportHelper.teleportToCoordinates(player, point.x, point.y, point.z, point.dimension);
                        return;
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class QuickAddPointPacket {
        public QuickAddPointPacket() {}

        public static void encode(QuickAddPointPacket msg, FriendlyByteBuf buf) {}

        public static QuickAddPointPacket decode(FriendlyByteBuf buf) {
            return new QuickAddPointPacket();
        }

        public static void handle(QuickAddPointPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                int count = PersonalTeleportPoints.get(player.server).getPointsByPlayer(player.getUUID()).size();
                String name = "传送点 " + (count + 1);

                TeleportPoint point = TeleportPoint.create(
                        player.getUUID(),
                        player.getName().getString(),
                        name,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.level().dimension().location()
                );

                PersonalTeleportPoints.get(player.server).addPoint(point);
                player.sendSystemMessage(Component.translatable("message.playertp.quick_added", name));

                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPersonalPointsPacket(
                        PersonalTeleportPoints.get(player.server).getPointsByPlayer(player.getUUID())
                ));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class RequestPlayerListPacket {
        public RequestPlayerListPacket() {}

        public static void encode(RequestPlayerListPacket msg, FriendlyByteBuf buf) {}

        public static RequestPlayerListPacket decode(FriendlyByteBuf buf) {
            return new RequestPlayerListPacket();
        }

        public static void handle(RequestPlayerListPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                List<com.example.playertp.gui.PlayerListScreen.PlayerInfo> players = new ArrayList<>();
                for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                    boolean isHidden = com.example.playertp.core.PlayerData.get(player.server).isHidden(p.getUUID());
                    players.add(new com.example.playertp.gui.PlayerListScreen.PlayerInfo(
                            p.getUUID(),
                            p.getName().getString(),
                            p.getX(),
                            p.getY(),
                            p.getZ(),
                            p.level().dimension().location(),
                            isHidden
                    ));
                }

                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerListPacket(players));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class SyncPlayerListPacket {
        private final List<com.example.playertp.gui.PlayerListScreen.PlayerInfo> players;

        public SyncPlayerListPacket(List<com.example.playertp.gui.PlayerListScreen.PlayerInfo> players) {
            this.players = players;
        }

        public static void encode(SyncPlayerListPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.players.size());
            for (com.example.playertp.gui.PlayerListScreen.PlayerInfo player : msg.players) {
                buf.writeUUID(player.uuid);
                buf.writeUtf(player.name);
                buf.writeDouble(player.x);
                buf.writeDouble(player.y);
                buf.writeDouble(player.z);
                buf.writeResourceLocation(player.dimension);
                buf.writeBoolean(player.isHidden);
            }
        }

        public static SyncPlayerListPacket decode(FriendlyByteBuf buf) {
            List<com.example.playertp.gui.PlayerListScreen.PlayerInfo> players = new ArrayList<>();
            int size = buf.readInt();
            for (int i = 0; i < size; i++) {
                players.add(new com.example.playertp.gui.PlayerListScreen.PlayerInfo(
                        buf.readUUID(),
                        buf.readUtf(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readDouble(),
                        buf.readResourceLocation(),
                        buf.readBoolean()
                ));
            }
            return new SyncPlayerListPacket(players);
        }

        public static void handle(SyncPlayerListPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                com.example.playertp.gui.PlayerListScreen.updatePlayers(msg.players);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ToggleVisibilityPacket {
        public ToggleVisibilityPacket() {}

        public static void encode(ToggleVisibilityPacket msg, FriendlyByteBuf buf) {}

        public static ToggleVisibilityPacket decode(FriendlyByteBuf buf) {
            return new ToggleVisibilityPacket();
        }

        public static void handle(ToggleVisibilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                com.example.playertp.core.PlayerData.get(player.server).toggleVisibility(player.getUUID());

                // Broadcast updated hidden set to all players
                SyncHiddenPlayersPacket sync = new SyncHiddenPlayersPacket(
                        com.example.playertp.core.PlayerData.get(player.server).getHiddenPlayers());
                INSTANCE.send(PacketDistributor.ALL.noArg(), sync);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class TeleportToPlayerPacket {
        private final double x, y, z;
        private final ResourceLocation dimension;

        public TeleportToPlayerPacket(double x, double y, double z, ResourceLocation dimension) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }

        public static void encode(TeleportToPlayerPacket msg, FriendlyByteBuf buf) {
            buf.writeDouble(msg.x);
            buf.writeDouble(msg.y);
            buf.writeDouble(msg.z);
            buf.writeResourceLocation(msg.dimension);
        }

        public static TeleportToPlayerPacket decode(FriendlyByteBuf buf) {
            return new TeleportToPlayerPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readResourceLocation());
        }

        public static void handle(TeleportToPlayerPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                TeleportHelper.teleportToCoordinates(player, msg.x, msg.y, msg.z, msg.dimension);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * Server → Client: syncs the set of hidden player UUIDs for compass filtering.
     */
    public static class SyncHiddenPlayersPacket {
        private final Set<UUID> hiddenUUIDs;

        public SyncHiddenPlayersPacket(Set<UUID> hiddenUUIDs) {
            this.hiddenUUIDs = hiddenUUIDs;
        }

        public static void encode(SyncHiddenPlayersPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.hiddenUUIDs.size());
            for (UUID uuid : msg.hiddenUUIDs) {
                buf.writeUUID(uuid);
            }
        }

        public static SyncHiddenPlayersPacket decode(FriendlyByteBuf buf) {
            int size = buf.readInt();
            Set<UUID> set = new HashSet<>();
            for (int i = 0; i < size; i++) {
                set.add(buf.readUUID());
            }
            return new SyncHiddenPlayersPacket(set);
        }

        public static void handle(SyncHiddenPlayersPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                com.example.playertp.render.CompassRenderer.updateHiddenPlayers(msg.hiddenUUIDs);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * Server → Client: periodic sync of ALL online players' positions for the compass.
     */
    public static class SyncAllPlayerPositionsPacket {
        // Each entry: UUID, name, x, y, z, dimension as ResourceLocation string
        private final List<String> data;

        public SyncAllPlayerPositionsPacket(List<String> data) {
            this.data = data;
        }

        public static void encode(SyncAllPlayerPositionsPacket msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.data.size());
            for (String s : msg.data) buf.writeUtf(s);
        }

        public static SyncAllPlayerPositionsPacket decode(FriendlyByteBuf buf) {
            int count = buf.readInt();
            List<String> list = new ArrayList<>();
            for (int i = 0; i < count; i++) list.add(buf.readUtf());
            return new SyncAllPlayerPositionsPacket(list);
        }

        public static void handle(SyncAllPlayerPositionsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                java.util.Set<UUID> alive = new java.util.HashSet<>();
                for (int i = 0; i + 5 < msg.data.size(); i += 6) {
                    try {
                        UUID uuid = UUID.fromString(msg.data.get(i));
                        String name = msg.data.get(i + 1);
                        double x = Double.parseDouble(msg.data.get(i + 2));
                        double y = Double.parseDouble(msg.data.get(i + 3));
                        double z = Double.parseDouble(msg.data.get(i + 4));
                        String dim = msg.data.get(i + 5);
                        alive.add(uuid);
                        com.example.playertp.render.CompassRenderer.updateCompassPlayer(
                                uuid, name, x, y, z, dim);
                    } catch (Exception ignored) {}
                }
                // Remove players who are no longer online
                com.example.playertp.render.CompassRenderer.clearStalePlayers(alive);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * Client → Server → All Clients: refresh all compass player skins.
     * Any player can trigger; server broadcasts to everyone.
     */
    public static class RefreshCompassSkinsPacket {
        public RefreshCompassSkinsPacket() {}

        public static void encode(RefreshCompassSkinsPacket msg, FriendlyByteBuf buf) {}

        public static RefreshCompassSkinsPacket decode(FriendlyByteBuf buf) {
            return new RefreshCompassSkinsPacket();
        }

        public static void handle(RefreshCompassSkinsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender != null) {
                    // Broadcast to ALL players so everyone refreshes
                    INSTANCE.send(PacketDistributor.ALL.noArg(), new RefreshCompassSkinsPacket());
                } else {
                    // Client-side: clear cached skins, reload next frames
                    com.example.playertp.render.CompassRenderer.resetAllSkins();
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * C→S: Move a point up or down in the list.
     */
    public static class MovePointPacket {
        private final UUID pointId;
        private final boolean isPublic;
        private final boolean up; // true=up, false=down

        public MovePointPacket(UUID pointId, boolean isPublic, boolean up) {
            this.pointId = pointId;
            this.isPublic = isPublic;
            this.up = up;
        }

        public static void encode(MovePointPacket msg, FriendlyByteBuf buf) {
            buf.writeUUID(msg.pointId);
            buf.writeBoolean(msg.isPublic);
            buf.writeBoolean(msg.up);
        }

        public static MovePointPacket decode(FriendlyByteBuf buf) {
            return new MovePointPacket(buf.readUUID(), buf.readBoolean(), buf.readBoolean());
        }

        public static void handle(MovePointPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                if (msg.isPublic) {
                    PublicTeleportPoints.get(player.server).movePoint(msg.pointId, msg.up);
                    // Sync updated public list to all
                    INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncPublicPointsPacket(
                            PublicTeleportPoints.get(player.server).getAllPoints()));
                } else {
                    PersonalTeleportPoints.get(player.server).movePoint(msg.pointId, player.getUUID(), msg.up);
                    // Sync updated personal list to sender only
                    INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPersonalPointsPacket(
                            PersonalTeleportPoints.get(player.server).getPointsByPlayer(player.getUUID())));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}