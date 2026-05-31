package com.newbiewood.nbw.weather;

import net.dries007.tfc.util.climate.KoppenClimateClassification;

public enum ClimateZoneGroup
{
    TROPICAL_RAINFOREST,
    TROPICAL,
    HOT_ARID,
    COLD_ARID,
    TEMPERATE,
    CONTINENTAL_HOT_SUMMER,
    CONTINENTAL_WARM_SUMMER,
    CONTINENTAL_COOL_SUMMER,
    CONTINENTAL_EXTREME,
    TUNDRA,
    ICE_CAP;

    public static ClimateZoneGroup fromKoppen(KoppenClimateClassification koppen)
    {
        return switch (koppen)
        {
            case AF -> TROPICAL_RAINFOREST;
            case AM, AW, AS -> TROPICAL;
            case BWH, BSH -> HOT_ARID;
            case BWK, BSK -> COLD_ARID;
            case CSA, CSB, CSC, CWA, CWB, CWC, CFA, CFB, CFC -> TEMPERATE;
            case DFA, DWA, DSA -> CONTINENTAL_HOT_SUMMER;
            case DFB, DWB, DSB -> CONTINENTAL_WARM_SUMMER;
            case DFC, DWC, DSC -> CONTINENTAL_COOL_SUMMER;
            case DFD, DWD, DSD -> CONTINENTAL_EXTREME;
            case ET -> TUNDRA;
            case EF -> ICE_CAP;
        };
    }
}