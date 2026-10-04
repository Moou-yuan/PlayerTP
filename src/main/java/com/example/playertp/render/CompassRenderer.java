package com.example.playertp.render;

import com.example.playertp.config.ClientConfig;
import com.example.playertp.core.TeleportPoint;
import com.example.playertp.gui.PersonalTeleportScreen;
import com.example.playertp.gui.PublicTeleportScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "playertp", value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class CompassRenderer {

    private static final int BAR_Y = 0;
    private static final int ACCENT_COLOR = 0x404A90D9; // subtle accent
    private static final int TICK_COLOR = 0x44FFFFFF;
    private static final int HEADING_COLOR = 0xFFFF4444;
    private static final int PERSONAL_COLOR = 0xFF4A90D9;
    private static final int PUBLIC_COLOR = 0xFFFF8C00;
    private static final int CARDINAL_TEXT_COLOR = 0xFFFFFFFF;
    private static final int PLAYER_COLOR = 0xFF44FF44; // green for other players
    // Cardinal direction letter colors (used for small dot below each letter)
    private static final int[] CARD_DOT_COLORS = {0xFFCC4444, 0xFF44CCCC, 0xFFCCCC44, 0xFF4444CC};
    // Distance text color presets (index from config)
    private static final int[] DIST_TEXT_COLORS = {0x88CCCCCC, 0xFFFFFFFF, 0xFFFFFF55, 0xFFFF5555, 0xFF55FF55, 0xFF5555FF};

    private static final double FOV_DEGREES = 45.0;

    // Runtime values loaded from config each frame
    private static int barHeight() { return ClientConfig.compassHeight.get(); }
    private static int barWidth(int screenW) { return (int) (screenW * ClientConfig.compassWidthPercent.get() / 100.0); }
    private static int barBg() {
        int alpha = ClientConfig.compassOpacity.get() * 255 / 100;
        return (alpha << 24) | 0x000000;
    }
    private static int distTextColor() { return DIST_TEXT_COLORS[ClientConfig.compassDistTextColorIdx.get()]; }

    // Hidden player set — synced from server, used to filter compass player markers
    private static Set<UUID> hiddenPlayers = new HashSet<>();
    private static boolean dataRequested = false;

    // All-player position cache — synced every 2s from server via SyncAllPlayerPositionsPacket
    private static final Map<UUID, CompassPlayerData> allPlayers = new ConcurrentHashMap<>();

    public static void updateHiddenPlayers(Set<UUID> hidden) {
        hiddenPlayers = hidden != null ? hidden : new HashSet<>();
    }

    /** Called from SyncAllPlayerPositionsPacket handler */
    public static void updateCompassPlayer(UUID uuid, String name, double x, double y, double z, String dim) {
        CompassPlayerData old = allPlayers.get(uuid);
        CompassPlayerData pd = new CompassPlayerData(name, x, y, z, dim);
        if (old != null) {
            pd.skin = old.skin;
            pd.ticksSeen = old.ticksSeen;
        }
        allPlayers.put(uuid, pd);
    }

    /** Called at start of each position sync to remove stale entries */
    public static void clearStalePlayers(Set<UUID> aliveUUIDs) {
        allPlayers.keySet().removeIf(uuid -> !aliveUUIDs.contains(uuid));
    }

    /** Clear all cached skins — triggers reload next frames */
    public static void resetAllSkins() {
        for (CompassPlayerData pd : allPlayers.values()) {
            pd.skin = null;
            pd.ticksSeen = 0;
        }
    }

    // ── Per-point render toggle (client-side) ───────────────────────
    private static final Set<UUID> hiddenPointIds = new HashSet<>();

    /** Toggle whether a point renders on the compass; returns new state (true = visible) */
    public static boolean togglePointRender(UUID pointId) {
        if (hiddenPointIds.contains(pointId)) {
            hiddenPointIds.remove(pointId);
            return true;
        } else {
            hiddenPointIds.add(pointId);
            return false;
        }
    }

    public static boolean isPointVisible(UUID pointId) {
        return !hiddenPointIds.contains(pointId);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.renderDebug) return;
        if (!ClientConfig.renderCompass.get()) return;
        if (mc.screen != null && mc.screen.isPauseScreen()) return;

        // Request sync data from server on first render (not dependent on GUI screens)
        if (!dataRequested) {
            dataRequested = true;
            com.example.playertp.network.ModNetwork.sendToServer(
                    new com.example.playertp.network.ModNetwork.RequestSyncPointsPacket());
        }

        Player player = mc.player;
        Level level = mc.level;
        GuiGraphics g = event.getGuiGraphics();

        int screenW = mc.getWindow().getGuiScaledWidth();
        int centerX = screenW / 2;

        // Gather visible points
        List<ColoredPoint> visible = new ArrayList<>();
        if (ClientConfig.renderPersonalPoints.get()) {
            for (TeleportPoint pt : PersonalTeleportScreen.getPoints()) {
                if (!isPointVisible(pt.id)) continue; // per-point toggle
                if (isInDimension(pt, level) && isInRange(player, pt)) {
                    double diff = getAngleDiff(player, pt);
                    if (Math.abs(diff) <= FOV_DEGREES) {
                        double dist = getDist(player, pt);
                        visible.add(new ColoredPoint(pt, PERSONAL_COLOR, diff, dist));
                    }
                }
            }
        }
        if (ClientConfig.renderPublicPoints.get()) {
            for (TeleportPoint pt : PublicTeleportScreen.getPoints()) {
                if (!isPointVisible(pt.id)) continue; // per-point toggle
                if (isInDimension(pt, level) && isInRange(player, pt)) {
                    double diff = getAngleDiff(player, pt);
                    if (Math.abs(diff) <= FOV_DEGREES) {
                        double dist = getDist(player, pt);
                        visible.add(new ColoredPoint(pt, PUBLIC_COLOR, diff, dist));
                    }
                }
            }
        }

        // Gather other players (from synced data, NOT mc.level.players() which is distance-limited)
        List<PlayerMarker> playerMarkers = new ArrayList<>();
        String myDim = player.level().dimension().location().toString();
        for (Map.Entry<UUID, CompassPlayerData> entry : allPlayers.entrySet()) {
            if (entry.getKey().equals(player.getUUID())) continue;
            if (hiddenPlayers.contains(entry.getKey())) continue;

            CompassPlayerData pd = entry.getValue();
            if (!pd.dimension.equals(myDim)) continue;

            double dx = pd.x - player.getX();
            double dz = pd.z - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            double targetAngle = Math.toDegrees(Math.atan2(-dx, dz));
            double diff = normAngle(targetAngle - player.getYHeadRot());
            if (Math.abs(diff) > FOV_DEGREES) continue;

            // Skin: delay caching to avoid default Ari/Skin before real skin loads
            ResourceLocation skin = pd.skin;
            pd.ticksSeen++;
            if (skin == null || pd.ticksSeen < 60) {
                Player loadedPlayer = mc.level.getPlayerByUUID(entry.getKey());
                if (loadedPlayer instanceof net.minecraft.client.player.AbstractClientPlayer acp) {
                    skin = acp.getSkinTextureLocation();
                    // Only cache after 3+ seconds (60 frames) — real skin is loaded by then
                    if (pd.ticksSeen >= 60 && skin != null) {
                        pd.skin = skin;
                    }
                }
            }

            playerMarkers.add(new PlayerMarker(dist, diff, skin));
        }

        float yaw = player.getYHeadRot();
        int barW = barWidth(screenW);
        int barH = barHeight();
        drawBar(g, barW, BAR_Y, barH, yaw, centerX);

        // Draw teleport point markers
        for (ColoredPoint cp : visible) {
            int mx = centerX + (int) (Math.sin(Math.toRadians(cp.angleDiff)) * (barW / 2));
            mx = clamp(mx, centerX - barW / 2 + 8, centerX + barW / 2 - 8);
            drawPointMarker(g, mx, BAR_Y, barH, cp.color, cp.point.iconText, cp.point.getCompassName(), cp.distance);
        }

        // Draw player markers (above the bar, head only, no name)
        for (PlayerMarker pm : playerMarkers) {
            int mx = centerX + (int) (Math.sin(Math.toRadians(pm.angleDiff)) * (barW / 2));
            mx = clamp(mx, centerX - barW / 2 + 14, centerX + barW / 2 - 14);
            drawPlayerMarker(g, mx, BAR_Y, pm.skin, pm.distance, barH);
        }
    }

    // ── Bar ────────────────────────────────────────────────────────

    private static void drawBar(GuiGraphics g, int w, int y, int h, float yaw, int cx) {
        // Centered semi-transparent background
        int left = cx - w / 2;
        int right = cx + w / 2;
        int bg = barBg();
        g.fill(left, y, right, y + h, bg);

        // Subtle bottom accent line
        g.fill(left, y + h - 1, right, y + h, ACCENT_COLOR);

        // Tick marks
        int[] ticks = {-180, -135, -90, -45, 0, 45, 90, 135, 180};
        for (int tw : ticks) {
            double diff = normAngle(tw - yaw);
            if (Math.abs(diff) <= FOV_DEGREES + 10) {
                int tx = cx + (int) (Math.sin(Math.toRadians(diff)) * (w / 2));
                tx = clamp(tx, 2, w - 2);
                int th = (tw % 90 == 0) ? 6 : 3;
                g.fill(tx, y + 1, tx + 1, y + 1 + th, TICK_COLOR);
            }
        }

        // Cardinal direction: E/S/W/N letters with colored dot underneath
        int[] cardAngles = {-180, -90, 0, 90}; // N, E, S, W in MC yaw space
        String[] cardLabels = {"N", "E", "S", "W"};
        for (int i = 0; i < cardAngles.length; i++) {
            double diff = normAngle(cardAngles[i] - yaw);
            if (Math.abs(diff) <= FOV_DEGREES + 5) {
                int cx2 = cx + (int) (Math.sin(Math.toRadians(diff)) * (w / 2));
                cx2 = clamp(cx2, 10, w - 10);
                drawCardinal(g, cx2, y + 1, cardLabels[i], CARD_DOT_COLORS[i]);
            }
        }

        // Center heading indicator
        int cs = 2;
        g.fill(cx - cs, y + h, cx + cs + 1, y + h + 3, HEADING_COLOR);  // red caret below bar
        g.fill(cx, y + 1, cx + 1, y + h - 1, 0x22FF4444);              // subtle center hairline
    }

    private static void drawCardinal(GuiGraphics g, int x, int top, String letter, int dotColor) {
        // Colored dot below the letter to distinguish from point markers
        g.fill(x - 2, top + 7, x + 2, top + 9, dotColor);
        // Letter centered above the dot
        int tw = Minecraft.getInstance().font.width(letter);
        g.drawString(Minecraft.getInstance().font, letter, x - tw / 2, top, CARDINAL_TEXT_COLOR);
    }

    // ── Point Marker ───────────────────────────────────────────────

    private static void drawPointMarker(GuiGraphics g, int x, int barY, int barH, int color, String iconText, String renderName, double distance) {
        int cy = barY + barH / 2;

        if (iconText == null || iconText.isEmpty()) {
            // Default: colored diamond ON the bar
            int s = ClientConfig.compassIconSize.get();
            g.fill(x - s,     cy - 1, x + s + 1, cy,     color);
            g.fill(x - s + 1, cy,     x + s,     cy + 1, color);
            g.fill(x - s + 2, cy + 1, x + s - 1, cy + 2, color);
        } else {
            // Custom icon replaces the diamond, rendered centered on the bar
            String icon = iconText.length() > 3 ? iconText.substring(0, 3) : iconText;
            int tw = Minecraft.getInstance().font.width(icon);
            g.drawString(Minecraft.getInstance().font, icon, x - tw / 2, cy - 4, color);
        }

        // Render name below
        if (renderName != null && !renderName.isEmpty()) {
            String rn = renderName.length() > 6 ? renderName.substring(0, 5) + "…" : renderName;
            int tw = Minecraft.getInstance().font.width(rn);
            g.drawString(Minecraft.getInstance().font, rn, x - tw / 2, barY + barH + 1, color);
        }

        // Distance below name (config-sized)
        String distStr = fmtDist(distance);
        int distY = barY + barH + ((renderName != null && !renderName.isEmpty()) ? 12 : 3);
        drawSmallText(g, distStr, x, distY, distTextColor());
    }

    // ── Player Marker (head only, + distance) ───────────────────────

    private static void drawPlayerMarker(GuiGraphics g, int x, int barY, ResourceLocation skin, double distance, int barH) {
        int headSize = 8;
        // Center head vertically on the compass bar
        int top = barY + (barH - headSize) / 2;

        if (skin != null) {
            RenderSystem.setShaderTexture(0, skin);
            g.blit(skin, x - headSize / 2, top, headSize, headSize, 8, 8, 8, 8, 64, 64);
            g.blit(skin, x - headSize / 2, top, headSize, headSize, 40, 8, 8, 8, 64, 64);
        }

        // Distance below the bar (config-sized)
        String distStr = fmtDist(distance);
        drawSmallText(g, distStr, x, barY + barH + 2, distTextColor());
    }

    // ── helpers ────────────────────────────────────────────────────

    private static double getDist(Player player, TeleportPoint pt) {
        double dx = pt.x - player.getX();
        double dz = pt.z - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static String fmtDist(double d) {
        if (d >= 1000) return String.format("%.1fkm", d / 1000);
        return String.format("%.0fm", d);
    }

    /** Draw distance text at configurable scale, centered on x */
    private static void drawSmallText(GuiGraphics g, String text, int cx, int y, int color) {
        float scale = (float) (double) ClientConfig.compassDistTextSize.get();
        int tw = Minecraft.getInstance().font.width(text);
        g.pose().pushPose();
        g.pose().translate(cx - tw * scale / 2f, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(Minecraft.getInstance().font, text, 0, 0, color);
        g.pose().popPose();
    }

    private static boolean isInDimension(TeleportPoint pt, Level level) {
        return pt.dimension != null && pt.dimension.equals(level.dimension().location());
    }

    private static boolean isInRange(Player player, TeleportPoint pt) {
        double dx = pt.x - player.getX();
        double dz = pt.z - player.getZ();
        int max = ClientConfig.beaconMaxDistance.get();
        return (dx * dx + dz * dz) <= (long) max * max;
    }

    private static double getAngleDiff(Player player, TeleportPoint pt) {
        double dx = pt.x - player.getX();
        double dz = pt.z - player.getZ();
        return normAngle(Math.toDegrees(Math.atan2(-dx, dz)) - player.getYHeadRot());
    }

    private static double normAngle(double a) {
        while (a < -180) a += 360;
        while (a > 180) a -= 360;
        return a;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ── data classes ────────────────────────────────────────────────

    private static class CompassPlayerData {
        final String name;
        final double x, y, z;
        final String dimension;
        ResourceLocation skin;    // cached only after real skin is loaded
        int ticksSeen;            // incremented each frame; cache skin after 60+
        CompassPlayerData(String n, double x, double y, double z, String d) {
            this.name = n; this.x = x; this.y = y; this.z = z; this.dimension = d;
        }
    }

    private static class ColoredPoint {
        final TeleportPoint point;
        final int color;
        final double angleDiff;
        final double distance;
        ColoredPoint(TeleportPoint pt, int c, double ad, double d) {
            this.point = pt; this.color = c; this.angleDiff = ad; this.distance = d;
        }
    }

    private static class PlayerMarker {
        final double distance;
        final double angleDiff;
        final ResourceLocation skin;
        PlayerMarker(double d, double ad, ResourceLocation s) {
            this.distance = d; this.angleDiff = ad; this.skin = s;
        }
    }
}
