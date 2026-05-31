package com.newbiewood.nbw.weather;

import com.newbiewood.nbw.ScorchAndFrost;
import com.newbiewood.nbw.ScorchAndFrostConfig;
import net.dries007.tfc.client.overworld.SolarCalculator;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.calendar.Season;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherEventManager
{
    private static final Map<ServerLevel, WeatherEventManager> INSTANCES = new ConcurrentHashMap<>();

    private final Map<ClimateZoneGroup, WeatherEvent> activeEvents = new ConcurrentHashMap<>();
    private final ServerLevel level;
    private long lastTickGameDay = -1;
    private final Map<ClimateZoneGroup, ClimateZoneConfig> zoneConfigs;

    private WeatherEventManager(ServerLevel level)
    {
        this.level = level;
        this.zoneConfigs = new ConcurrentHashMap<>();
        for (ClimateZoneGroup group : ClimateZoneGroup.values())
        {
            zoneConfigs.put(group, ClimateZoneConfig.forGroup(group));
        }
    }

    public static WeatherEventManager get(ServerLevel level)
    {
        return INSTANCES.computeIfAbsent(level, WeatherEventManager::new);
    }

    public static void clear()
    {
        INSTANCES.clear();
    }

    public static float getGlobalTemperatureOffset(LevelReader level, BlockPos pos)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            WeatherEventManager manager = INSTANCES.get(serverLevel);
            return manager != null ? manager.getTemperatureOffset(pos) : 0f;
        }
        if (level instanceof Level clientLevel)
        {
            return WeatherClientCache.getTemperatureOffset(clientLevel, pos);
        }
        return 0f;
    }

    public void tick()
    {
        if (!ScorchAndFrostConfig.ENABLE_WEATHER_EVENTS.getAsBoolean()) return;

        long currentDay = level.getGameTime() / 24000;
        if (currentDay == lastTickGameDay) return;
        lastTickGameDay = currentDay;

        Set<ClimateZoneGroup> expired = new HashSet<>();
        for (var entry : activeEvents.entrySet())
        {
            boolean isExpired = entry.getValue().tickDay(level);
            if (isExpired) expired.add(entry.getKey());
        }
        expired.forEach(activeEvents::remove);

        if (activeEvents.size() < ScorchAndFrostConfig.MAX_ACTIVE_EVENTS.get())
        {
            Set<ClimateZoneGroup> populatedZones = getPopulatedZones();
            for (ClimateZoneGroup group : populatedZones)
            {
                if (activeEvents.containsKey(group)) continue;
                ClimateZoneConfig config = zoneConfigs.get(group);
                if (config != null) tryTriggerEvent(group, config);
            }
        }

        syncToClients();
    }

    private void tryTriggerEvent(ClimateZoneGroup group, ClimateZoneConfig config)
    {
        Season currentSeason = getCurrentSeason();
        RandomSource random = level.getRandom();

        if (config.canHeatWave && currentSeason != Season.WINTER)
        {
            float prob = config.heatWaveBaseProbability * (currentSeason == Season.SUMMER ? config.summerMultiplier : 1f);
            if (random.nextFloat() < prob)
            {
                float offset = config.selectHeatWaveOffset(level.getRandom());
                int duration = random.nextIntBetweenInclusive(
                        ScorchAndFrostConfig.MIN_EVENT_DURATION_DAYS.get(),
                        ScorchAndFrostConfig.MAX_EVENT_DURATION_DAYS.get());
                activeEvents.put(group, new WeatherEvent(WeatherEventType.HEAT_WAVE, offset, duration));
                return;
            }
        }

        if (config.canColdWave && currentSeason != Season.SUMMER)
        {
            float prob = config.coldWaveBaseProbability * (currentSeason == Season.WINTER ? config.winterMultiplier : 1f);
            if (random.nextFloat() < prob)
            {
                float offset = config.selectColdWaveOffset(level.getRandom());
                int duration = random.nextIntBetweenInclusive(
                        ScorchAndFrostConfig.MIN_EVENT_DURATION_DAYS.get(),
                        ScorchAndFrostConfig.MAX_EVENT_DURATION_DAYS.get());
                activeEvents.put(group, new WeatherEvent(WeatherEventType.COLD_WAVE, offset, duration));
            }
        }
    }

    public KoppenClimateClassification classifyAt(BlockPos pos)
    {
        return classifyInternal(pos);
    }

    public float getTemperatureOffset(BlockPos pos)
    {
        KoppenClimateClassification koppen = classifyAt(pos);
        ClimateZoneGroup group = ClimateZoneGroup.fromKoppen(koppen);
        WeatherEvent event = activeEvents.get(group);
        return event != null ? event.getCurrentOffset() : 0f;
    }

    public void forceEvent(ClimateZoneGroup group, WeatherEvent event)
    {
        activeEvents.put(group, event);
    }

    public void clearEvent(ClimateZoneGroup group)
    {
        activeEvents.remove(group);
    }

    public void clearEvents()
    {
        activeEvents.clear();
    }

    public Map<ClimateZoneGroup, WeatherEvent> getActiveEvents()
    {
        return Collections.unmodifiableMap(activeEvents);
    }

    private Season getCurrentSeason()
    {
        ICalendar calendar = Calendars.get(level);
        long calendarTicks = calendar.getCalendarTicks();
        int daysInMonth = calendar.getCalendarDaysInMonth();
        float hemisphereScale = Climate.get(level).hemisphereScale();
        boolean north = SolarCalculator.getInNorthernHemisphere(0, hemisphereScale);
        Month month = calendar.getHemispheralCalendarMonthOfYear(north, calendarTicks, daysInMonth);
        return month.getSeason();
    }

    private Set<ClimateZoneGroup> getPopulatedZones()
    {
        Set<ClimateZoneGroup> zones = new HashSet<>();
        for (ServerPlayer player : level.players())
        {
            zones.add(ClimateZoneGroup.fromKoppen(classifyAt(player.blockPosition())));
        }
        return zones;
    }

    private KoppenClimateClassification classifyInternal(BlockPos pos)
    {
        float avgTemp = Climate.getAverageTemperature(level, pos);
        float rainfall = Climate.getAverageRainfall(level, pos);
        float rainVar = Climate.getRainfallVariance(level, pos);
        boolean north = SolarCalculator.getInNorthernHemisphere(pos, level);
        return KoppenClimateClassification.classify(avgTemp, rainfall, rainVar, north);
    }

    private void syncToClients()
    {
        Map<ClimateZoneGroup, EventPhase> phaseChanges = new HashMap<>();
        for (var entry : activeEvents.entrySet())
        {
            if (entry.getValue().hasPhaseChanged())
            {
                phaseChanges.put(entry.getKey(), entry.getValue().getCurrentPhase());
            }
        }

        boolean sendMessages = ScorchAndFrostConfig.SEND_PHASE_MESSAGES.getAsBoolean();

        for (ServerPlayer player : level.players())
        {
            KoppenClimateClassification koppen = classifyAt(player.blockPosition());
            ClimateZoneGroup playerGroup = ClimateZoneGroup.fromKoppen(koppen);

            Map<ClimateZoneGroup, WeatherEvent> relevantEvents = new HashMap<>();
            for (var entry : activeEvents.entrySet())
            {
                if (entry.getKey() == playerGroup)
                {
                    relevantEvents.put(entry.getKey(), entry.getValue());
                }
            }
            WeatherSyncPacket.send(player, relevantEvents);

            if (sendMessages)
            {
                EventPhase phase = phaseChanges.get(playerGroup);
                if (phase != null)
                {
                    WeatherEvent event = activeEvents.get(playerGroup);
                    if (event != null)
                    {
                        String langKey = "event." + ScorchAndFrost.MODID + "."
                                + (event.getType() == WeatherEventType.HEAT_WAVE ? "heatwave" : "coldwave")
                                + "." + phase.name().toLowerCase();
                        player.sendSystemMessage(Component.translatable(langKey));
                    }
                }
            }
        }
    }
}