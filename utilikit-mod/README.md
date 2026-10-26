# UtiliKit — 实用工具系统

三个高质量实用工具的 Fabric 模组（MC **1.20.1**，需要 Fabric API）：探测仪、传送仪、空间背包，均带合成表。

## 工具一览

### 1. 🔍 矿物探测仪（Ore Prospector）
**手持即生效（主手或副手/左手均可——左手持有时你可以正常挥镐挖掘）**：

- 每 1 秒扫描周围 **12 格**（立方体，可配置）内的全部矿石（煤/铁/铜/金/红石/青金石/钻石/绿宝石 + 下界石英/下界金/远古残骸）
- 相邻矿石自动聚合为**矿脉**（2.5 格内同算一脉），从你的视线发出**红色虚线**指向矿脉质心（透明度脉冲呼吸），矿脉处画红/绿/蓝三色十字标记
- 最多同时标记 24 处（按距离取最近），矿脉数量变化时动作栏提示 + 轻微"叮"声
- 侦查方向：虚线从你眼部出发，跟着走就能挖到

### 2. 🌀 标记传送仪（Mark Teleporter）
- **潜行 + 右键**：标记当前位置（坐标 + 维度 + 朝向，写入物品 NBT，tooltip 可见）
- **右键**：定向传送回标记点——**支持跨维度**（主世界/下界/末地任一标记点）
- 3 秒冷却（防刷），每次传送消耗 1 点耐久（共 128 次）；两端均有传送门粒子与音效，落地重置摔落高度

### 3. 🎒 空间背包（Spatial Backpack）
右键打开 54 格（6 行 × 9，原版大箱子界面布局）的**虚拟容器**：

- **自动分类**：同类物品自动合并进同一格（按首次存入顺序排列）
- **无限储存**：内部以 `Map<Item, Long>` 计数，单格储量 92 亿亿级——存 10 万个圆石也只占一格
- 操作：**Shift 点击**背包物品整组存入 / **点击虚拟格**取出 64 个 / 右键取 32
- **悬停虚拟格显示真实储量**（"已储存：12345"）
- 关闭界面自动写回物品 NBT，随身携带；最多同时显示 54 类物品（更多类型仍会储存）

## 合成表

**矿物探测仪**（4 铁锭 + 5 钻石 + 1 指南针）：
```
铁 钻 铁
钻 针 钻     针 = 指南针
铁 钻 铁
```

**标记传送仪**（4 末影珍珠 + 4 钻石 + 1 紫水晶碎片）：
```
珠 钻 珠
钻 晶 钻     晶 = 紫水晶碎片
珠 钻 珠
```

**空间背包**（4 潜影壳 + 4 末影之眼 + 1 潜影盒）：
```
壳 眼 壳
眼 盒 眼     盒 = 潜影盒
壳 眼 壳
```

## 构建与安装

```bash
./gradlew build          # Windows: gradlew.bat build
# 产物: build/libs/utilikit-1.0.0.jar
```

安装：Fabric Loader 1.20.1 + Fabric API 0.92.x，jar 放入 `.minecraft/mods/`。

## 配置（`.minecraft/config/utilikit.json`）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `prospectorRadius` | `12` | 探测半径（4–24 格） |
| `prospectorMaxVeins` | `24` | 最多同时标记的矿脉数 |
| `teleportCooldownTicks` | `60` | 传送仪冷却（tick） |

## 代码结构

```
src/main/java/com/utilikit/
├── UtiliKitMod.java               # 物品 + 菜单类型注册
├── UtiliKitClient.java            # 客户端入口（tick/渲染/GUI 注册）
├── config/UtiliKitConfig.java
├── item/
│   ├── TeleporterItem.java        # 标记/传送（NBT、跨维度、冷却、耐久）
│   └── SpatialBackpackItem.java   # 打开虚拟容器
├── menu/
│   ├── SpatialContainer.java      # 虚拟容器：Map<Item,Long> + 54 格视图 + NBT
│   └── SpatialBackpackMenu.java   # 菜单：快速转移语义 + 储量同步（ContainerData）
└── client/
    ├── ProspectorClient.java      # 扫描聚类 + 红色虚线渲染（RenderType.lines）
    └── SpatialBackpackScreen.java # 大箱子布局 GUI + 储量 Tooltip
```

零 Mixin：基于 Fabric API 事件（`ClientTickEvents`、`WorldRenderEvents`、`MenuRegistry`）。

## 许可

MIT（见 [LICENSE](LICENSE)）。
