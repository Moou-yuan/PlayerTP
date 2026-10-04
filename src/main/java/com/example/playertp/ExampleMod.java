package com.example.playertp;

import com.example.playertp.config.ClientConfig;
import com.example.playertp.gui.PersonalTeleportScreen;
import com.example.playertp.gui.PlayerListScreen;
import com.example.playertp.gui.PublicTeleportScreen;
import com.example.playertp.network.ModNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ExampleMod.MODID)
public class ExampleMod
{
    public static final String MODID = "playertp";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::onRegisterKeyMappings);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerDeath);

        context.registerConfig(ModConfig.Type.COMMON, com.example.playertp.Config.COMMON_SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("PlayerTP Mod Common Setup");
        ModNetwork.register();
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        LOGGER.info("PlayerTP Mod Client Setup");
        // Key bindings are registered via KeyBindings.java and configurable
        // through Minecraft's built-in Controls → Key Binds menu.
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("PlayerTP Mod Server Starting");
    }

    private int tickCounter = 0;

    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter % 12 != 0) return; // every ~0.6 seconds

        net.minecraft.server.MinecraftServer server = event.getServer();
        if (server == null || server.getPlayerList() == null) return;

        java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        java.util.List<String> data = new java.util.ArrayList<>();
        for (net.minecraft.server.level.ServerPlayer sp : players) {
            data.add(sp.getUUID().toString());
            data.add(sp.getName().getString());
            data.add(String.valueOf(sp.getX()));
            data.add(String.valueOf(sp.getY()));
            data.add(String.valueOf(sp.getZ()));
            data.add(sp.level().dimension().location().toString());
        }
        ModNetwork.sendToAll(new ModNetwork.SyncAllPlayerPositionsPacket(data));
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            // Sync current hidden-players set to the joining player
            java.util.Set<java.util.UUID> hiddenSet = com.example.playertp.core.PlayerData.get(sp.server).getHiddenPlayers();
            ModNetwork.sendToPlayer(sp, new ModNetwork.SyncHiddenPlayersPacket(hiddenSet));
        }
    }

    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            // Create death point with timestamp name (e.g., "死亡点08121355")
            java.util.Calendar cal = java.util.Calendar.getInstance();
            String deathName = String.format("死亡点%02d%02d%02d%02d",
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH),
                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                    cal.get(java.util.Calendar.MINUTE));
            com.example.playertp.core.TeleportPoint deathPoint = com.example.playertp.core.TeleportPoint.create(
                    sp.getUUID(), sp.getName().getString(), deathName,
                    sp.getX(), sp.getY(), sp.getZ(), sp.level().dimension().location(), "💀");
            com.example.playertp.core.PersonalTeleportPoints.get(sp.server).addPoint(deathPoint);
            ModNetwork.sendToPlayer(sp, new ModNetwork.SyncPersonalPointsPacket(
                    com.example.playertp.core.PersonalTeleportPoints.get(sp.server).getPointsByPlayer(sp.getUUID())));
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        // Only open screens from here when no screen is currently shown.
        // Screen closing/cross-navigation is handled by each screen's keyPressed().

        if (KeyBindings.OPEN_PLAYER_LIST.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new PlayerListScreen());
            }
        }

        if (KeyBindings.OPEN_PERSONAL_POINTS.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new PersonalTeleportScreen());
            }
        }

        if (KeyBindings.OPEN_PUBLIC_POINTS.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new PublicTeleportScreen());
            }
        }

        if (KeyBindings.QUICK_ADD_POINT.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                PersonalTeleportScreen pts = new PersonalTeleportScreen();
                mc.setScreen(pts);
                int count = PersonalTeleportScreen.getPoints().size();
                mc.setScreen(new PersonalTeleportScreen.PointConfigScreen(pts,
                        net.minecraft.network.chat.Component.translatable("gui.playertp.point_default_name", count + 1).getString(),
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), null));
            }
        }

        // Quick teleport: ↓ teleports to the marked point
        if (KeyBindings.QUICK_TELEPORT.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                java.util.UUID quickId = PersonalTeleportScreen.getQuickTeleportId();
                if (quickId != null) {
                    ModNetwork.sendToServer(new ModNetwork.TeleportToPointPacket(quickId, false));
                }
            }
        }
    }

    @SubscribeEvent
    public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Only register keys that should be user-configurable in Controls menu
        event.register(KeyBindings.OPEN_PLAYER_LIST);
        event.register(KeyBindings.QUICK_ADD_POINT);
        event.register(KeyBindings.QUICK_TELEPORT);
    }
}