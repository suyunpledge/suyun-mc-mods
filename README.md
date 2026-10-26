# Suyun MC Mods

Minecraft Java **1.20.1** Fabric 模组合集，全部为原创玩法模组，需要 [Fabric Loader](https://fabricmc.net/) 0.14.21+ 与 [Fabric API](https://modrinth.com/mod/fabric-api)。

| 模组 | 版本 | 简介 |
| --- | --- | --- |
| [BladeCombo 拔刀连斩](bladecombo-mod/) | 1.1.0 | 固定连招的拔刀剑：四段无限连斩、挑空、360° 回旋，为手残党设计 |
| [ForgeSmith 锻造强化](forgesmith-mod/) | 1.0.0 | 在原版锻造台上用材料强化盔甲与工具，最多 3 层，无新增物品 |
| [HIM — The Haunting of Herobrine](him-mod/) | 1.2.0 | 高强度还原都市传说全家桶：白眼 Herobrine、实体 303、Null、404，含宿敌对决 |
| [RealCraft 现实工艺](realcraft-mod/) | 1.0.0 | 火把会烧尽、树木会倒下、真实摔伤、负重系统等现实化机制 |
| [UtiliKit 实用工具](utilikit-mod/) | 1.0.0 | 矿物探测仪、传送仪、空间背包三件实用工具，均带合成表 |

## 构建

每个子目录都是独立的 Gradle 工程：

```bash
cd bladecombo-mod   # 或其他模组目录
./gradlew build     # Windows: gradlew.bat build
```

产物在 `build/libs/<mod>-<version>.jar`，需要 JDK 17。首次构建会通过 Loom 拉取 Minecraft 与映射文件，请保持网络畅通。

## 目录说明

- `*-mod/` 各模组完整源码（含各自的 README 与 LICENSE）
- `him-mod/RESEARCH.md` 为 Herobrine 都市传说考证资料
