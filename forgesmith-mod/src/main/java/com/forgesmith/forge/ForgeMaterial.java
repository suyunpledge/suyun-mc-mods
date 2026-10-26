package com.forgesmith.forge;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

/**
 * 锻造材料定义。
 *
 * 锭类（ingotRank > 0）遵循"高级覆盖低级"：铁/金 = 1 级，钻石 = 2 级，下界合金 = 3 级。
 * 非锭类（rank = 0）各自占一层，可重复叠加。
 */
public enum ForgeMaterial {
    IRON("iron", Items.IRON_INGOT, 1, 1),
    GOLD("gold", Items.GOLD_INGOT, 1, 1),
    DIAMOND("diamond", Items.DIAMOND, 2, 1),
    NETHERITE("netherite", Items.NETHERITE_INGOT, 3, 1),
    REDSTONE("redstone", Items.REDSTONE, 0, 4),
    LAPIS("lapis", Items.LAPIS_LAZULI, 0, 4),
    EMERALD("emerald", Items.EMERALD, 0, 1);

    public final String layerId;
    public final Item item;
    /** 锭等级；0 = 非锭 */
    public final int ingotRank;
    /** 每次强化消耗数量 */
    public final int cost;

    ForgeMaterial(String layerId, Item item, int ingotRank, int cost) {
        this.layerId = layerId;
        this.item = item;
        this.ingotRank = ingotRank;
        this.cost = cost;
    }

    public boolean isIngot() {
        return this.ingotRank > 0;
    }

    @Nullable
    public static ForgeMaterial fromItem(Item item) {
        for (ForgeMaterial material : values()) {
            if (material.item == item) {
                return material;
            }
        }
        return null;
    }

    public static int rankOf(String layerId) {
        for (ForgeMaterial material : values()) {
            if (material.layerId.equals(layerId)) {
                return material.ingotRank;
            }
        }
        return 0;
    }

    public String displayKey() {
        return "material.forgesmith." + this.layerId;
    }
}
