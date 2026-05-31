# TFC Scorch and Frost

TFC Scorch and Frost adds dynamic heat wave and cold wave weather events to TerraFirmaCraft, triggered per Köppen climate zone with Gaussian-curve temperature offsets.

## Features

- **Heat Wave / Cold Wave Events** — Independently triggered per Köppen climate zone, each with a Gaussian temperature curve
- **11 Climate Groups** — Mapped from TFC's 30 Köppen subtypes, each with independent probability and weighted offset tables
- **Three-Phase Evolution** — Events progress through Onset → Peak → Recovery, with chat notifications at each phase
- **Seasonal Influence** — Summer doubles heat wave chance, winter doubles cold wave chance; cold waves blocked in summer, heat waves blocked in winter
- **Gaussian Curve** — Temperature offset ramps up smoothly through Onset, peaks, then fades through Recovery
- **Concurrent Events** — Up to 5 events active simultaneously across different climate zones
- **Server Authoritative** — All event logic runs server-side, synced to clients via custom payload
- **Pure Environment Layer** — Injects directly into `OverworldClimateModel.getInstantTemperature()`, fully compatible with other temperature/body mods

## Dependencies

| Dependency | Version | Type |
|:-----------|:--------|:-----|
| NeoForge | 21.1.231 | required |
| Minecraft | 1.21.1 | required |
| TerraFirmaCraft | 4.1.2+ | required |
| Patchouli | 1.21.1-93-NEOFORGE | required (TFC dependency) |

## Configuration

Generated at `world/serverconfig/tfc_scorch_and_frost-server.toml`:

| Option | Default | Description |
|:-------|:--------|:------------|
| `enableWeatherEvents` | `true` | Master toggle |
| `maxActiveEvents` | `5` | Maximum concurrent events |
| `minEventDurationDays` | `3` | Minimum event duration in days |
| `maxEventDurationDays` | `7` | Maximum event duration in days |
| `sigmaRatio` | `0.4` | Gaussian σ = duration × sigmaRatio |
| `sendPhaseMessages` | `true` | Whether to send phase change messages |

## Debug Commands

Requires permission level 2 (operator).

```
/tfcscorch trigger <heatwave|coldwave> <group> [offset] [duration]
/tfcscorch list
/tfcscorch clear [group]
/tfcscorch whereami
```

**Examples**

```
/tfcscorch whereami                                  → Check your current climate group
/tfcscorch trigger coldwave CONTINENTAL_WARM_SUMMER  → Random offset & duration
/tfcscorch trigger heatwave MEDITERRANEAN 5 7        → +5°C, 7 days
/tfcscorch list                                      → List active events
/tfcscorch clear                                     → Clear all events
```

## Climate Groups

| Group | Köppen Types |
|-------|--------------|
| `TROPICAL_RAINFOREST` | Af |
| `TROPICAL_MONSOON` | Am |
| `TROPICAL_SAVANNA` | Aw |
| `ARID` | BWh, BWk, BSh, BSk |
| `MEDITERRANEAN` | Csa, Csb |
| `SUBTROPICAL` | Cfa, Cwa |
| `TEMPERATE_OCEANIC` | Cfb, Cfc, Cwb, Cwc |
| `CONTINENTAL` | Dfa, Dfb, Dwa, Dwb, Dsa, Dsb |
| `BOREAL_FOREST` | Dfc, Dfd, Dwc, Dwd, Dsc, Dsd |
| `COLD_ARID` | ET |
| `ICE_CAP` | EF |

## Architecture

```
Server daily tick (24000 gt)
  └─ WeatherEventManager.tick()
       ├─ Cleanup expired events
       ├─ Collect populated climate zones
       ├─ Roll probability per zone (season-scaled)
       │   └─ Hit → pick offset from weight table → create WeatherEvent
       └─ Sync to client (WeatherSyncPacket)
            └─ WeatherClientCache caches events

Temperature query
  OverworldClimateModel.getInstantTemperature()
    └─ [Mixin] → WeatherEventManager.getGlobalTemperatureOffset()
         ├─ Server: WeatherEventManager (ServerLevel)
         └─ Client: WeatherClientCache (ClientLevel)
```

## License

MIT © newbiewood
