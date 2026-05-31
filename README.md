# TFC Scorch and Frost

TFC Scorch and Frost 为 TerraFirmaCraft 添加动态热浪和寒潮天气事件，基于 Köppen 气候分类分区触发，使用高斯曲线模拟温度渐变。

## 特性

- **热浪 / 寒潮事件** — 根据 Köppen 气候分区独立触发，每个事件附带高斯曲线温度偏移
- **11 个气候分组** — 从 TFC 的 30 种 Köppen 子类映射为 11 个气候组，每组独立配置概率和偏移权重表
- **三阶段演化** — 事件经历 Onset（爆发）→ Peak（峰值）→ Recovery（消退）三个阶段，每个阶段推送聊天框通知
- **季节影响** — 夏季热浪概率翻倍，冬季寒潮概率翻倍，寒潮不会出现在夏季，反之亦然
- **高斯曲线** — 温度偏移从 Onset 开始平滑上升至 Peak 回落 Recovery，而非突变
- **多事件并行** — 最多 5 个事件同时在各自气候分区生效
- **服务端权威** — 所有事件逻辑在服务端计算，通过自定义 Payload 同步到客户端
- **纯环境温度层** — 直接注入 OverworldClimateModel.getInstantTemperature()，不干扰其他体温模组

## 依赖

| 依赖 | 版本 | 类型 |
|:-----|:-----|:----:|
| NeoForge | 21.1.231 | required |
| Minecraft | 1.21.1 | required |
| TerraFirmaCraft | 4.1.2+ | required |
| Patchouli | 1.21.1-93-NEOFORGE | required (TFC 依赖) |

## 配置

生成在 `world/serverconfig/tfc_scorch_and_frost-server.toml`：

| 配置项 | 默认值 | 说明 |
|:-------|:-------|:-----|
| `enableWeatherEvents` | `true` | 总开关 |
| `maxActiveEvents` | `5` | 最大并行事件数 |
| `minEventDurationDays` | `3` | 事件最短持续天数 |
| `maxEventDurationDays` | `7` | 事件最长持续天数 |
| `sigmaRatio` | `0.4` | 高斯曲线 σ = duration × sigmaRatio |
| `sendPhaseMessages` | `true` | 是否发送阶段推送消息 |

## 调试指令

需要权限等级 2（管理员）。

```
/tfcscorch trigger <heatwave|coldwave> <group> [offset] [duration]
/tfcscorch list
/tfcscorch clear [group]
/tfcscorch whereami
```

**示例**

```
/tfcscorch whereami                                  → 查看当前气候分组
/tfcscorch trigger coldwave CONTINENTAL_WARM_SUMMER  → 寒潮，随机偏移+天数
/tfcscorch trigger heatwave MEDITERRANEAN 5 7        → 热浪 +5°C，7 天
/tfcscorch list                                      → 列出活跃事件
/tfcscorch clear                                     → 清除所有事件
```

## 气候分组

| 分组名 | 对应 Köppen 类型 |
|--------|------------------|
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

## 技术架构

```
服务端每日 tick (24000 gt)
  └─ WeatherEventManager.tick()
       ├─ 过期事件清理
       ├─ 收集有玩家的气候分组
       ├─ 按概率 roll 事件（季节加成）
       │   └─ 命中 → 权重表选偏移 → 创建 WeatherEvent
       └─ 同步到客户端 (WeatherSyncPacket)
            └─ WeatherClientCache 缓存

温度查询
  OverworldClimateModel.getInstantTemperature()
    └─ [Mixin] → WeatherEventManager.getGlobalTemperatureOffset()
         ├─ 服务端: WeatherEventManager (ServerLevel)
         └─ 客户端: WeatherClientCache (ClientLevel)
```

## 许可证

MIT © newbiewood
