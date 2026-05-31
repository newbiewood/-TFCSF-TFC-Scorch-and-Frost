package com.newbiewood.nbw.weather;

import com.newbiewood.nbw.ScorchAndFrostConfig;
import net.minecraft.server.level.ServerLevel;

public class WeatherEvent
{
    private final WeatherEventType type;
    private final float peakOffset;
    private final int totalDays;
    private int remainingDays;
    private EventPhase currentPhase;
    private boolean phaseChanged;

    public WeatherEvent(WeatherEventType type, float peakOffset, int totalDays)
    {
        this.type = type;
        this.peakOffset = peakOffset;
        this.totalDays = totalDays;
        this.remainingDays = totalDays;
        this.currentPhase = EventPhase.ONSET;
        this.phaseChanged = true;
    }

    public boolean tickDay(ServerLevel level)
    {
        remainingDays--;
        if (remainingDays <= 0) return true;

        EventPhase newPhase = computePhase();
        if (newPhase != currentPhase)
        {
            currentPhase = newPhase;
            phaseChanged = true;
        }
        return false;
    }

    private EventPhase computePhase()
    {
        int elapsed = totalDays - remainingDays;
        if (elapsed < 0) return EventPhase.ONSET;

        float ratio = (float) elapsed / totalDays;

        if (ratio < 0.25f) return EventPhase.ONSET;
        else if (ratio < 0.75f) return EventPhase.PEAK;
        else return EventPhase.RECOVERY;
    }

    public float getCurrentOffset()
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

    public boolean hasPhaseChanged()
    {
        if (phaseChanged)
        {
            phaseChanged = false;
            return true;
        }
        return false;
    }

    public WeatherEventType getType() { return type; }
    public float getPeakOffset() { return peakOffset; }
    public int getTotalDays() { return totalDays; }
    public int getRemainingDays() { return remainingDays; }
    public EventPhase getCurrentPhase() { return currentPhase; }
}