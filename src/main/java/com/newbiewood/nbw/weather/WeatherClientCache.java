package com.newbiewood.nbw.weather;

import com.newbiewood.nbw.ScorchAndFrostConfig;
import net.dries007.tfc.client.overworld.SolarCalculator;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WeatherClientCache
{
    private static Map<ClimateZoneGroup, WeatherSyncPacket.SyncEvent> cachedEvents = Map.of();

    public static void update(Map<ClimateZoneGroup, WeatherSyncPacket.SyncEvent> events)
    {
        cachedEvents = events.isEmpty() ? Map.of() : Collections.unmodifiableMap(new HashMap<>(events));
    }

    public static Map<ClimateZoneGroup, WeatherSyncPacket.SyncEvent> getCachedEvents()
    {
        return cachedEvents;
    }

    public static boolean hasEvent(ClimateZoneGroup group)
    {
        return cachedEvents.containsKey(group);
    }

    public static WeatherSyncPacket.SyncEvent getEvent(ClimateZoneGroup group)
    {
        return cachedEvents.get(group);
    }

    public static float getTemperatureOffset(Level level, BlockPos pos)
    {
        if (cachedEvents.isEmpty()) return 0f;

        float avgTemp = Climate.getAverageTemperature(level, pos);
        float rainfall = Climate.getAverageRainfall(level, pos);
        float rainVar = Climate.getRainfallVariance(level, pos);
        boolean north = SolarCalculator.getInNorthernHemisphere(pos, level);
        KoppenClimateClassification koppen = KoppenClimateClassification.classify(avgTemp, rainfall, rainVar, north);
        ClimateZoneGroup group = ClimateZoneGroup.fromKoppen(koppen);

        WeatherSyncPacket.SyncEvent event = cachedEvents.get(group);
        if (event == null) return 0f;

        return computeGaussianOffset(event.peakOffset(), event.totalDays(), event.remainingDays());
    }

    public static float computeGaussianOffset(float peakOffset, int totalDays, int remainingDays)
    {
        int elapsed = totalDays - remainingDays;
        if (elapsed < 0) elapsed = 0;
        if (elapsed >= totalDays) elapsed = totalDays - 1;

        float mid = (totalDays - 1) / 2f;
        float sigma = totalDays * ScorchAndFrostConfig.SIGMA_RATIO.get().floatValue();

        if (sigma <= 0f) return peakOffset;

        float exponent = -(float) Math.pow(elapsed - mid, 2) / (2f * sigma * sigma);
        float factor = (float) Math.exp(exponent);

        return peakOffset * factor;
    }
}