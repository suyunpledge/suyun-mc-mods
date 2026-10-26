package com.forgesmith.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备上的锻造层数据（NBT: ForgeLayers）。
 */
public final class ForgeData {
    public static final int MAX_LAYERS = 3;

    private ForgeData() {
    }

    public static boolean isGear(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof ArmorItem
                || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof PickaxeItem
                || stack.getItem() instanceof ShovelItem
                || stack.getItem() instanceof AxeItem;
    }

    public static List<String> layers(ItemStack stack) {
        List<String> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("ForgeLayers", Tag.TAG_LIST)) {
            ListTag list = tag.getList("ForgeLayers", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                result.add(list.getString(i));
            }
        }
        return result;
    }

    public static void setLayers(ItemStack stack, List<String> layers) {
        ListTag list = new ListTag();
        for (String layer : layers) {
            list.add(StringTag.valueOf(layer));
        }
        stack.getOrCreateTag().put("ForgeLayers", list);
    }

    public static int count(ItemStack stack, String layerId) {
        int n = 0;
        for (String layer : layers(stack)) {
            if (layer.equals(layerId)) {
                n++;
            }
        }
        return n;
    }

    /** 第一处锭层下标；-1 = 无锭层 */
    public static int firstIngotIndex(List<String> layers) {
        for (int i = 0; i < layers.size(); i++) {
            if (ForgeMaterial.rankOf(layers.get(i)) > 0) {
                return i;
            }
        }
        return -1;
    }

    /** 玩家整套盔甲中某层总数 */
    public static int armorLayerCount(Player player, String layerId) {
        int total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                total += count(player.getItemBySlot(slot), layerId);
            }
        }
        return total;
    }
}
