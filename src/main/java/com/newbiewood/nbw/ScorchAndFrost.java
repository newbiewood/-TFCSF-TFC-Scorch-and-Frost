package com.newbiewood.nbw;

import com.mojang.logging.LogUtils;
import com.newbiewood.nbw.weather.ModCommands;
import com.newbiewood.nbw.weather.WeatherEventManager;
import com.newbiewood.nbw.weather.WeatherSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(ScorchAndFrost.MODID)
public class ScorchAndFrost
{
    public static final String MODID = "tfc_scorch_and_frost";
    public static final Logger LOGGER = LogUtils.getLogger();

    private boolean serverStarted = false;

    public ScorchAndFrost(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerNetwork);

        modContainer.registerConfig(ModConfig.Type.SERVER, ScorchAndFrostConfig.SPEC);

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        ModCommands.register(event.getDispatcher());
    }

    private void commonSetup(FMLCommonSetupEvent event)
    {
        LOGGER.info("TFC Scorch and Frost initialized");
    }

    private void registerNetwork(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1");
        registrar.playToClient(
                WeatherSyncPacket.TYPE,
                WeatherSyncPacket.STREAM_CODEC,
                WeatherSyncPacket::handleClient
        );
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event)
    {
        serverStarted = true;
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event)
    {
        WeatherEventManager.clear();
        serverStarted = false;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event)
    {
        if (!serverStarted) return;
        if (event.getServer().getPlayerCount() == 0) return;
        if (event.getServer().overworld() == null) return;

        ServerLevel level = event.getServer().overworld();
        WeatherEventManager.get(level).tick();
    }
}