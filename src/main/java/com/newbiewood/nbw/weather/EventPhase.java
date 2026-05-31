package com.newbiewood.nbw.weather;

public enum EventPhase
{
    ONSET,
    PEAK,
    RECOVERY;

    public EventPhase next()
    {
        return values()[(ordinal() + 1) % values().length];
    }
}