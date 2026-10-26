# ForgeSmith — 锻造强化

在**原版锻造台**上强化原版盔甲与工具的 Fabric 模组（MC **1.20.1**，需要 Fabric API）。无新增物品——纯强化系统。

## 用法

一手拿**装备**（盔甲/剑/镐/铲/斧），另一手拿**材料**，对**锻造台右键**即完成强化：
材料不足会红字提示；双手组合不匹配时正常打开原版锻造台界面。

- 每件装备最多 **3 层**强化（tooltip 可见：`锻造强化（2/3 层）`）
- **锭类高级覆盖低级**：铁/金（1 级）< 钻石（2 级）< 下界合金（3 级）。
  已有锭层时，用更高等级的锭会**替换**该层（不占新层数）；同级或降级会被拒绝

## 材料 × 效果一览

| 材料 | 消耗 | 盔甲 | 工具/武器 |
| --- | --- | --- | --- |
| 铁锭 / 金锭 | 1 | +2 护甲，+10% 击退抗性 | 剑 +2 伤害；镐/铲 急迫 I；斧 耐久 II |
| 钻石 | 1 | +3 护甲，+15% 击退抗性 | 剑 +3 伤害；镐/铲 急迫 II；斧 耐久 III |
| 下界合金锭 | 1 | +4 护甲，+20% 击退抗性 | 剑 +4 伤害；镐/铲 急迫 II；斧 耐久 III |
| 红石 | 4 | **生命上限 +4/层** | **耐久附魔 I/II/III**（按层数，大幅降低耐久消耗） |
| 青金石 | 4 | **水抗**：1 层免疫溺水；2 层再免疫冻伤；3 层水中受击回补 20% | **潮涌能量**（水下挖掘与攻击不减速） |
| 绿宝石 | 1 | **生命恢复**（1-2 层再生 I，3 层再生 II） | **吸血**：满蓄力命中回复攻击伤害 ×10%/层（最高 30%） |

说明：
- 盔甲加成是**每件**计算的——全套 4 件下界合金层 = +16 护甲 +80% 击退抗性
- 红石层可叠加（最多 3 层 = 生命上限 +12 或 耐久 III）；斧的"锭层耐久"与红石层取最高
- 剑的攻击速度等原版属性完整保留（基础属性 + 加成一并重写进物品 NBT）
- 锻造有音效与暴击粒子反馈；强化层数显示在物品 tooltip

## 构建与安装

```bash
./gradlew build          # Windows: gradlew.bat build
# 产物: build/libs/forgesmith-1.0.0.jar
```

安装：Fabric Loader 1.20.1 + Fabric API 0.92.x，jar 放入 `.minecraft/mods/`。

## 配置（`.minecraft/config/forgesmith.json`）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 总开关 |
| `lifestealPerLayer` | `0.10` | 每层绿宝石吸血比例 |
| `waterHealRatio` | `0.20` | 青金石 3 层水中受击回补比例 |

## 实现方式（零 Mixin）

- `UseBlockCallback`：锻造台右键识别双手"装备+材料"组合
- `ItemStack.addAttributeModifier`：把"原版基础属性 + 锻造加成"幂等重写入物品 NBT（盔甲护甲/击退抗性、剑伤害、红石生命上限）
- `ItemStack.enchant`：红石/斧的耐久附魔
- `ServerTickEvents`：再生（盔甲绿宝石）、潮涌能量（青金石）、急迫（镐铲锭层）周期刷新
- `AttackEntityCallback`：绿宝石吸血
- `ServerLivingEntityEvents.ALLOW_DAMAGE/AFTER_DAMAGE`：水抗（溺水/冻伤免疫、水中减伤）
- `ItemTooltipCallback`（客户端）：层数展示

```
src/main/java/com/forgesmith/
├── ForgeSmithMod.java / ForgeSmithClient.java
├── config/ForgeSmithConfig.java
├── forge/
│   ├── ForgeMaterial.java      # 材料：层 id、锭等级、消耗
│   ├── ForgeData.java          # NBT 层数据（ForgeLayers）
│   ├── ForgeApplier.java       # 层数变更 + 属性/附魔重算 + 演出
│   └── ForgeTableEvents.java   # 锻造台右键交互
└── event/
    ├── EquipmentTicker.java    # 再生/潮涌/急迫
    └── CombatEvents.java       # 吸血 / 水抗
```

## 许可

MIT（见 [LICENSE](LICENSE)）。
