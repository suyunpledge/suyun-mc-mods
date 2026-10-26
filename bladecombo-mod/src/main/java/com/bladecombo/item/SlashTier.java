package com.bladecombo.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 拔刀剑品质（竹光 → 铁刃 → 钻石刃 → 合金刃）。
 * 攻击加成计入攻击伤害属性，连招倍率在此基础上结算。
 */
public final class SlashTier implements Tier {
    public static final Tier BAMBOO = new SlashTier(512, 3, 15, Items.BAMBOO);
    public static final Tier IRON = new SlashTier(1024, 5, 16, Items.IRON_INGOT);
    public static final Tier DIAMOND = new SlashTier(2048, 6, 18, Items.DIAMOND);
    public static final Tier NETHERITE = new SlashTier(3072, 8, 20, Items.NETHERITE_INGOT);

    private final int uses;
    private final int attackBonus;
    private final int enchantValue;
    private final Item repairItem;

    private SlashTier(int uses, int attackBonus, int enchantValue, Item repairItem) {
        this.uses = uses;
        this.attackBonus = attackBonus;
        this.enchantValue = enchantValue;
        this.repairItem = repairItem;
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return 8.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.attackBonus;
    }

    @Override
    public int getLevel() {
        return 3;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(this.repairItem);
    }
}
