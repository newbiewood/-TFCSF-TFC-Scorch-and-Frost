package com.newbiewood.nbw.weather;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

public class ClimateZoneConfig
{
    public final float heatWaveBaseProbability;
    public final float coldWaveBaseProbability;
    public final float summerMultiplier;
    public final float winterMultiplier;
    public final boolean canHeatWave;
    public final boolean canColdWave;
    public final List<WeightedOffset> heatWaveTable;
    public final List<WeightedOffset> coldWaveTable;

    private final int heatWaveTotalWeight;
    private final int coldWaveTotalWeight;

    public record WeightedOffset(float offset, int weight) {}

    private ClimateZoneConfig(
            float heatWaveBaseProbability, float coldWaveBaseProbability,
            float summerMultiplier, float winterMultiplier,
            boolean canHeatWave, boolean canColdWave,
            List<WeightedOffset> heatWaveTable, List<WeightedOffset> coldWaveTable)
    {
        this.heatWaveBaseProbability = heatWaveBaseProbability;
        this.coldWaveBaseProbability = coldWaveBaseProbability;
        this.summerMultiplier = summerMultiplier;
        this.winterMultiplier = winterMultiplier;
        this.canHeatWave = canHeatWave;
        this.canColdWave = canColdWave;
        this.heatWaveTable = List.copyOf(heatWaveTable);
        this.coldWaveTable = List.copyOf(coldWaveTable);
        this.heatWaveTotalWeight = heatWaveTable.stream().mapToInt(WeightedOffset::weight).sum();
        this.coldWaveTotalWeight = coldWaveTable.stream().mapToInt(WeightedOffset::weight).sum();
    }

    public float selectHeatWaveOffset(RandomSource random)
    {
        return selectWeightedOffset(random, heatWaveTable, heatWaveTotalWeight);
    }

    public float selectColdWaveOffset(RandomSource random)
    {
        return selectWeightedOffset(random, coldWaveTable, coldWaveTotalWeight);
    }

    private float selectWeightedOffset(RandomSource random, List<WeightedOffset> table, int totalWeight)
    {
        if (table.isEmpty() || totalWeight <= 0) return 0f;

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedOffset entry : table)
        {
            cumulative += entry.weight;
            if (roll < cumulative) return entry.offset;
        }
        return table.get(table.size() - 1).offset;
    }

    public static ClimateZoneConfig forGroup(ClimateZoneGroup group)
    {
        return switch (group)
        {
            case TROPICAL_RAINFOREST -> new Builder()
                    .canHeatWave().heatWaveBaseProbability(0.01f).summerMultiplier(2f)
                    .heatWaveTable(List.of(
                            new WeightedOffset(1f, 60),
                            new WeightedOffset(2f, 30),
                            new WeightedOffset(3f, 10)))
                    .build();

            case TROPICAL -> new Builder()
                    .canHeatWave().heatWaveBaseProbability(0.02f).summerMultiplier(2f)
                    .heatWaveTable(List.of(
                            new WeightedOffset(2f, 50),
                            new WeightedOffset(3f, 30),
                            new WeightedOffset(5f, 15),
                            new WeightedOffset(7f, 5)))
                    .build();

            case HOT_ARID -> new Builder()
                    .canHeatWave().heatWaveBaseProbability(0.03f).summerMultiplier(2f)
                    .heatWaveTable(List.of(
                            new WeightedOffset(5f, 55),
                            new WeightedOffset(8f, 28),
                            new WeightedOffset(12f, 13),
                            new WeightedOffset(15f, 4)))
                    .build();

            case COLD_ARID -> new Builder()
                    .canHeatWave().heatWaveBaseProbability(0.02f).summerMultiplier(2f)
                    .heatWaveTable(List.of(
                            new WeightedOffset(3f, 60),
                            new WeightedOffset(5f, 28),
                            new WeightedOffset(8f, 12)))
                    .canColdWave().coldWaveBaseProbability(0.02f).winterMultiplier(2f)
                    .coldWaveTable(List.of(
                            new WeightedOffset(-3f, 55),
                            new WeightedOffset(-5f, 28),
                            new WeightedOffset(-8f, 13),
                            new WeightedOffset(-10f, 4)))
                    .build();

            case TEMPERATE -> new Builder()
                    .canHeatWave().heatWaveBaseProbability(0.02f).summerMultiplier(2f)
                    .heatWaveTable(List.of(
                            new WeightedOffset(3f, 50),
                            new WeightedOffset(5f, 28),
                            new WeightedOffset(8f, 15),
                            new WeightedOffset(10f, 5),
                            new WeightedOffset(12f, 2)))
                    .canColdWave().coldWaveBaseProbability(0.02f).winterMultiplier(2f)
                    .coldWaveTable(List.of(
                            new WeightedOffset(-3f, 50),
                            new WeightedOffset(-5f, 28),
                            new WeightedOffset(-8f, 14),
                            new WeightedOffset(-10f, 5),
                            new WeightedOffset(-12f, 3)))
                    .build();

            case CONTINENTAL_HOT_SUMMER -> new Builder()
                    .canHeatWave().heatWaveBaseProbability(0.02f).summerMultiplier(2f)
                    .heatWaveTable(List.of(
                            new WeightedOffset(4f, 45),
                            new WeightedOffset(6f, 30),
                            new WeightedOffset(9f, 18),
                            new WeightedOffset(12f, 7)))
                    .canColdWave().coldWaveBaseProbability(0.02f).winterMultiplier(2f)
                    .coldWaveTable(List.of(
                            new WeightedOffset(-5f, 42),
                            new WeightedOffset(-8f, 28),
                            new WeightedOffset(-10f, 20),
                            new WeightedOffset(-15f, 10)))
                    .build();

            case CONTINENTAL_WARM_SUMMER -> new Builder()
                    .canHeatWave().heatWaveBaseProbability(0.01f).summerMultiplier(2f)
                    .heatWaveTable(List.of(
                            new WeightedOffset(3f, 60),
                            new WeightedOffset(5f, 28),
                            new WeightedOffset(8f, 12)))
                    .canColdWave().coldWaveBaseProbability(0.02f).winterMultiplier(2f)
                    .coldWaveTable(List.of(
                            new WeightedOffset(-5f, 42),
                            new WeightedOffset(-8f, 28),
                            new WeightedOffset(-12f, 20),
                            new WeightedOffset(-15f, 10)))
                    .build();

            case CONTINENTAL_COOL_SUMMER -> new Builder()
                    .canColdWave().coldWaveBaseProbability(0.02f).winterMultiplier(2f)
                    .coldWaveTable(List.of(
                            new WeightedOffset(-8f, 40),
                            new WeightedOffset(-10f, 25),
                            new WeightedOffset(-12f, 18),
                            new WeightedOffset(-15f, 12),
                            new WeightedOffset(-18f, 5)))
                    .build();

            case CONTINENTAL_EXTREME -> new Builder()
                    .canColdWave().coldWaveBaseProbability(0.02f).winterMultiplier(2f)
                    .coldWaveTable(List.of(
                            new WeightedOffset(-10f, 38),
                            new WeightedOffset(-12f, 25),
                            new WeightedOffset(-15f, 18),
                            new WeightedOffset(-18f, 13),
                            new WeightedOffset(-20f, 6)))
                    .build();

            case TUNDRA -> new Builder()
                    .canColdWave().coldWaveBaseProbability(0.02f).winterMultiplier(2f)
                    .coldWaveTable(List.of(
                            new WeightedOffset(-8f, 45),
                            new WeightedOffset(-10f, 25),
                            new WeightedOffset(-12f, 17),
                            new WeightedOffset(-15f, 10),
                            new WeightedOffset(-18f, 3)))
                    .build();

            case ICE_CAP -> new Builder()
                    .canColdWave().coldWaveBaseProbability(0.01f).winterMultiplier(2f)
                    .coldWaveTable(List.of(
                            new WeightedOffset(-5f, 50),
                            new WeightedOffset(-8f, 35),
                            new WeightedOffset(-10f, 15)))
                    .build();
        };
    }

    public static class Builder
    {
        private float heatWaveBaseProbability;
        private float coldWaveBaseProbability;
        private float summerMultiplier = 1f;
        private float winterMultiplier = 1f;
        private boolean canHeatWave;
        private boolean canColdWave;
        private final List<WeightedOffset> heatWaveTable = new ArrayList<>();
        private final List<WeightedOffset> coldWaveTable = new ArrayList<>();

        public Builder canHeatWave()
        {
            this.canHeatWave = true;
            return this;
        }

        public Builder canColdWave()
        {
            this.canColdWave = true;
            return this;
        }

        public Builder heatWaveBaseProbability(float value)
        {
            this.heatWaveBaseProbability = value;
            return this;
        }

        public Builder coldWaveBaseProbability(float value)
        {
            this.coldWaveBaseProbability = value;
            return this;
        }

        public Builder summerMultiplier(float value)
        {
            this.summerMultiplier = value;
            return this;
        }

        public Builder winterMultiplier(float value)
        {
            this.winterMultiplier = value;
            return this;
        }

        public Builder heatWaveTable(List<WeightedOffset> table)
        {
            this.heatWaveTable.addAll(table);
            return this;
        }

        public Builder coldWaveTable(List<WeightedOffset> table)
        {
            this.coldWaveTable.addAll(table);
            return this;
        }

        public ClimateZoneConfig build()
        {
            return new ClimateZoneConfig(
                    heatWaveBaseProbability, coldWaveBaseProbability,
                    summerMultiplier, winterMultiplier,
                    canHeatWave, canColdWave,
                    heatWaveTable, coldWaveTable);
        }
    }
}