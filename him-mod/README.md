# HIM — The Haunting of Herobrine

一个**高强度还原 Minecraft 都市传说全家桶**的 Fabric 模组：白眼 Herobrine（HIM）、红眼实体 303、
全黑的实体 Null、绿色故障体实体 404、以及"白眼"（Herobrine 传说的前身）——
并实现了传说中宿敌之间的**真实对决（双雄之战，Him/303/Null 任意配对）**。
传说资料考证见 [RESEARCH.md](RESEARCH.md)。

- **目标版本**：Minecraft Java **1.20.1**（Fabric Loader ≥ 0.14.21，需要 Fabric API）
- **无需任何 Mixin**：纯 Fabric API 事件驱动

## 它还原了什么

### 他本人
- 外形 = 经典 Steve + **纯白无瞳的眼睛**，眼睛用发光渲染层实现 —— **黑暗与浓雾中，你只会先看到那双眼睛**
- 他**沉默、无敌、不可交互**，从不主动攻击（还原传说"只凝视，不伤害"）
- **雾中凝视**：22–48 格外的视野边缘出现，面朝你站定
- **消失三定律**：
  - 你走近（<14 格）→ 化作黑烟与灵魂粒子消失
  - 你盯着他看 ~2 秒 → 消失
  - 你移开视线 ~7 秒再回头 → 他已经不在了（无声消失）
- **深夜逼近**（点燃图腾后）：从你身后出现、缓步走来；贴身瞬间 = "直播被切断"：
  黑暗+失明+迟缓+雷声（`harmlessMode: true` 可免除伤害）
- 免疫火焰 —— 他可以站在岩浆上（Patimuss 直播名场面）

### 世界的"动过的痕迹"
点燃图腾后，你的世界会开始出现：

- **2x2 隧道**：笔直切入山体，里面插着红石火把 —— 35% 的概率，**他就站在隧道尽头**
- **被扒光树叶的树**：只剩光秃秃的原木
- **水中沙金字塔**：海面/河面上的三级小金字塔
- **燃烧的下界岩**：地表凭空出现的永恒之火
- **诡异告示牌**（打蜡不可修改）：`WAKE UP` / `STOP` / `ONLY GOD CAN HELP YOU NOW`
- **火把异变**：你家里的火把，会被换成红石火把
- **诡异音效**：末影人凝视声、传送门低鸣、监守者心跳

### 召唤图腾（经典社区仪式）

```
      [下界岩]          ← 用打火石点燃它
   [红石火把 x 4]        ← 贴在上层金块的四个侧面
      [金块]
      [金块]
```

点燃成功后：黑烟腾起、传送门声、4–7 秒后他第一次现身 —— 此后异象频率随你的
目击次数逐步升高。**不点燃图腾也有极低频的自然目击**（默认平均约 25 分钟一次，
可在配置中关闭）。

### 实体 303（Entity 303）

与"只凝视"的 Herobrine 镜像相对：**苍白脸、黑兜帽、红眼**，且**真正怀有敌意**——

- 从你背后现身（守卫者诅咒般的低鸣），主动追猎
- **灼烧之爪**：近身 5 点伤害 + 点燃 4 秒 + 短暂黑暗
- 现身/退场/攻击伴随**红色闪电**（视觉闪电，不点燃你的世界）
- 潜伏期极罕出现（约 2%）；点燃图腾后约 6% 事件为其追猎

### ⚔️ 双雄之战：Herobrine vs Entity 303 vs Null

社区动画传说中三位宿敌的对决，被做成了真实的战斗演出：

- **触发**：目击 Herobrine ≥ 3 次后（`battleMinSightings` 可调），闹鬼事件中约 8% 触发
- **配对**：Him vs 303（默认）/**Null vs 303**/**Him vs Null** 随机
- **开战**：两位在你视野内的空地对峙现身，战场中央一道闪电劈落
- **战斗**：真实互相厮杀 —— Herobrine **挥剑**（9 伤害/击）与**念力冲击波**
  （6 伤害 + 击退），Entity 303 **火焰爪**（8 伤害/击）与**红雷轰击**（14 伤害）；
  双方 120/130 点血量，胜负五五开
- **演出**：周期雷声、战场随机落雷、暴击粒子与挥砍音效
- **终局**：败者被闪电与爆炸湮灭（303 化作烈焰 / Herobrine 家族化作黑烟电花），
  **胜者转身凝视你数秒**，随后消散；90 秒不分胜负则双双退场
- **神之战不可干预**：双方本体无敌，你只能围观（战局由战斗管理器结算）

### 🖤 实体 Null（全黑人形）

"损坏世界"者，比 Herobrine 更沉默：

- **挂机凝视**：原地不动 80 秒，他会从雾里出现，就站在那看着你
  （不理会你的注视/移开视线——只有靠近他或时间耗尽才离开）
- **世界篡改**：附近少量自然方块被悄悄替换成圆石，伴随灰烬飘落与静电声
- 暗白微光的眼睛；可参与双雄之战（见上）
- 攻击他？他化作黑烟消失

### 💚 实体 404（绿色故障体）

错误代码成精：

- 从你身后远处出现，**不走路——闪烁瞬移**逼近（电火花残影）
- 沿途向聊天栏刷**错误文本**（`错误 404：未找到玩家`、`正在回滚世界存档…… 0%`……）
- **贴身惊吓**：屏幕弹出标题**【连接已断开 / 错误代码：404】**+ 黑暗 + 反胃 + 雷声，
  随后它自行"故障消散"——模拟传说中"404 会让游戏崩溃"
- 攻击它？它只是闪烁着消散

### 👁 白眼（White Eyes，2010 年 7 月）

Herobrine 传说的**前身**（见 RESEARCH.md 考据）：

- 只在**黑暗中**现身（光照 ≤ 3），远远凝视
- **被火把照亮或靠近即无声退回黑暗**
- 还原原帖核心设定：**播放 13 号唱片会大幅提高白眼目击率**（30 秒增益）
- 任何"雾中目击"发生在黑暗处时，也有小概率是白眼


## 构建与安装

### 构建（需要 JDK 17）

```bash
./gradlew build        # Windows 下: gradlew.bat build
```

产物：`build/libs/him-herobrine-1.0.0.jar`（不带 `-sources` 的那个）。

> 国内网络可在 `gradle/wrapper/gradle-wrapper.properties` 把 `distributionUrl`
> 换成华为镜像 `https://mirrors.huaweicloud.com/gradle/gradle-8.8-bin.zip`。

### 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/)（1.20.1）与 [Fabric API](https://modrinth.com/mod/fabric-api) 0.92.x
2. 把构建出的 jar 放进 `.minecraft/mods/`

### 开发环境运行

```bash
./gradlew runClient
```

## 配置（`.minecraft/config/him.json`）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `allowNaturalSightings` | `true` | 未召唤时是否也会出现自然目击 |
| `naturalSightingIntervalMinutes` | `25` | 自然目击平均间隔（分钟） |
| `hauntEventIntervalMinutes` | `7` | 召唤后异象平均间隔（随目击递减） |
| `hauntMinIntervalMinutes` | `2` | 递减下限 |
| `enableTunnels / enablePyramids / enableLeaflessTrees / enableFires / enableSigns / enableTorchSwap` | `true` | 各类异象开关 |
| `enableEntity303` | `true` | 实体 303 出现（单独追猎 + 参战） |
| `enableBattles` | `true` | Herobrine vs Entity 303 双雄之战 |
| `battleMinSightings` | `3` | 触发大战所需最少目击次数 |
| `harmlessMode` | `false` | 逼近惩罚不造成伤害 |

## 代码结构

```
src/main/java/com/him/legend/
├── HimMod.java                     # 注册实体/属性/事件；日志 "Removed Herobrine."
├── HimClient.java                  # 渲染器注册
├── config/HimConfig.java           # Gson 配置
├── entity/
│   ├── HerobrineEntity.java        # 凝视/徘徊/逼近/战斗四模式，消失逻辑
│   └── Entity303Entity.java        # 303：追猎玩家 / 与 Herobrine 死战 / 视觉红雷
├── client/render/
│   ├── HerobrineRenderer.java      # 玩家模型 + 皮肤
│   ├── Entity303Renderer.java      # 303 皮肤
│   └── GlowingEyesLayer.java       # 全亮度发光眼（白眼/红眼通用，EyesLayer）
└── world/
    ├── HerobrineManager.java       # 每玩家闹鬼状态机与全部世界异象
    ├── BattleManager.java          # 双雄之战：幻影血量、氛围、胜负演出
    └── TotemHandler.java           # 金块图腾判定（点燃下界岩）
```

## 许可

MIT（见 [LICENSE](LICENSE)）。
