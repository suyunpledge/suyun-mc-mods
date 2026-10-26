# RealCraft — 现实工艺

把 Minecraft 的一些游戏机制**模拟成现实**的 Fabric 模组（MC **1.20.1**，需要 Fabric API）。

## 现实化的机制

### 1. 🔥 火把会烧尽
现实中火把不是永动机。放置火把时会被替换为**点燃的火把**（外观与原版一致、亮度 14）：

- **燃料时长**：默认 8 分钟（±1 分钟随机），烧尽后变为**熄灭的火把**（不发光、冒烟熄灭声）
- **雨水浇熄**：露天火把在雨天会被淋灭
- **打火石重燃**：对熄灭的火把使用打火石即可重新点燃（消耗 1 点耐久）
- 熄灭的火把可正常放置（立式/壁挂均支持），也会自然出现在创造物品栏"功能方块"页
- 原版火把物品照常合成；世界里已存在的原版火把不受影响

### 2. 🌳 现实伐木（树会倒）
砍断树干后，**上方整段原木"倒下"**（作为掉落物落地），树冠树叶随之散落——
现实中树不会悬空等你一格一格撸。连锁上限默认 32 根（巨型树需砍多刀）。

### 3. 🩹 真实摔落（扭伤/骨折）
从高处跌落不再只是掉血：

- 摔落 ≥5 格：**扭伤**——短暂缓慢（跛行）
- 摔落 ≥10 格：**骨折**——缓慢 II + 虚弱 20 秒，并弹出提示

### 4. 🎒 负重系统
背包物品件数超过**软上限**（默认 1296，约 20 组）→ 缓慢 I；
超过**硬上限**（默认 1984，约 31 组）→ 缓慢 II。创造模式不受影响。

### 5. ♨️ 深井高温
主世界 y < -24 越挖越热：**挖掘疲劳**，且每 10 秒 1 点灼热伤害（附火焰粒子与提示）。
缓解方式（现实矿井靠通风降温）：**抗火药水**或**泡在水里**。

## 构建与安装

```bash
./gradlew build          # Windows: gradlew.bat build
# 产物: build/libs/realcraft-1.0.0.jar
```

安装：Fabric Loader 1.20.1 + Fabric API 0.92.x，把 jar 放进 `.minecraft/mods/`。

## 配置（`.minecraft/config/realcraft.json`）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `enableTorchBurnout` | `true` | 火把燃烧系统总开关 |
| `torchFuelTicks` | `9600` | 火把燃料时长（tick，9600=8 分钟） |
| `enableRainExtinguish` | `true` | 雨水浇熄 |
| `enableTreeFelling` | `true` | 现实伐木 |
| `treeFellMaxLogs` | `32` | 一刀最多倒下的原木数 |
| `enableFractures` | `true` | 摔伤系统 |
| `limpFallBlocks` / `fractureFallBlocks` | `5` / `10` | 扭伤/骨折阈值（格） |
| `enableWeight` | `true` | 负重系统 |
| `weightSoft` / `weightHard` | `1296` / `1984` | 软/硬上限（件数） |
| `enableDeepHeat` | `true` | 深井高温 |
| `deepHeatY` | `-24` | 高温起始 y（主世界） |

## 代码结构

```
src/main/java/com/realcraft/
├── RealCraftMod.java          # 方块/物品注册，事件挂载
├── config/RealCraftConfig.java
├── block/
│   ├── LitTorchBlock.java         # 点燃的火把（立式）：燃料+雨熄+粒子
│   ├── LitWallTorchBlock.java     # 点燃的火把（壁挂）
│   ├── UnlitTorchBlock.java       # 熄灭的火把（立式）
│   └── UnlitWallTorchBlock.java   # 熄灭的火把（壁挂）
└── event/
    ├── TorchEvents.java       # 放置替换 + 打火石重燃
    ├── TreeEvents.java        # 现实伐木
    ├── FallEvents.java        # 扭伤/骨折
    └── PlayerTicker.java      # 负重 + 深井高温（每 2 秒）
```

零 Mixin：全部基于 Fabric API 事件（`UseBlockCallback`、`PlayerBlockBreakEvents`、
`ServerLivingEntityEvents`、`ServerTickEvents`）。

## 许可

MIT（见 [LICENSE](LICENSE)）。
