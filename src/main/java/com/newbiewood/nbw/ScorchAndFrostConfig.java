package com.newbiewood.nbw;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ScorchAndFrostConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_WEATHER_EVENTS = BUILDER
            .comment("Enable the heat wave / cold wave weather event system")
            .define("enableWeatherEvents", true);

    public static final ModConfigSpec.IntValue MAX_ACTIVE_EVENTS = BUILDER
            .comment("Maximum number of climate zones that can have an active weather event simultaneously")
            .defineInRange("maxActiveEvents", 5, 1, 20);

    public static final ModConfigSpec.IntValue MIN_EVENT_DURATION_DAYS = BUILDER
            .comment("Minimum duration of a weather event in Minecraft days")
            .defineInRange("minEventDurationDays", 3, 1, 30);

    public static final ModConfigSpec.IntValue MAX_EVENT_DURATION_DAYS = BUILDER
            .comment("Maximum duration of a weather event in Minecraft days")
            .defineInRange("maxEventDurationDays", 7, 1, 30);

    public static final ModConfigSpec.DoubleValue SIGMA_RATIO = BUILDER
            .comment("Controls the width of the Gaussian peak (sigma / totalDays). Lower = sharper peak")
            .defineInRange("sigmaRatio", 0.4, 0.1, 1.0);

    public static final ModConfigSpec.BooleanValue SEND_PHASE_MESSAGES = BUILDER
            .comment("Send chat messages to players when weather event phase changes")
            .define("sendPhaseMessages", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}