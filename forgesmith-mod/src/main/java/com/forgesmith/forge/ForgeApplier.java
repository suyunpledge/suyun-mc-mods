package com.forgesmith.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;
import java.util.UUID;

/**
 * 锻造执行器：层数变更 + 属性/附魔重算 + 演出。
 *
 * 属性方案：把"基础默认属性 + 锻造加成"整体写入物品 NBT 的 AttributeModifiers，
 * 每次锻造幂等重算（基础部分沿用原 modifier UUID，与原版语义一致）。
 */
public final class ForgeApplier {
    private static final UUID HEALTH_BONUS_UUID = UUID.fromString("5c0a5f7e-2f4b-4b0e-9d3a-8f1c0a5b7e01");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ForgeApplier() {
    }

    /** 应用强化。返回 true = 成功（失败时已向玩家发送原因）。 */
    public static boolean apply(Player player, ItemStack gear, ForgeMaterial material, ItemStack materialStack) {
        List<String> layers = ForgeData.layers(gear);

        if (material.isIngot()) {
            int idx = ForgeData.firstIngotIndex(layers);
            if (idx >= 0) {
                int oldRank = ForgeMaterial.rankOf(layers.get(idx));
                if (material.ingotRank <= oldRank) {
                    fail(player, "message.forgesmith.rank_too_low");
                    return false;
                }
                layers.set(idx, material.layerId);   // 高级覆盖低级：不占新层
            } else {
                if (layers.size() >= ForgeData.MAX_LAYERS) {
                    fail(player, "message.forgesmith.max_layers");
                    return false;
                }
                layers.add(material.layerId);
            }
        } else {
            if (layers.size() >= ForgeData.MAX_LAYERS) {
                fail(player, "message.forgesmith.max_layers");
                return false;
            }
            layers.add(material.layerId);
        }

        materialStack.shrink(material.cost);
        ForgeData.setLayers(gear, layers);
        recalcGear(gear);

        if (player instanceof ServerPlayer serverPlayer
                && serverPlayer.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 0.8F);
            level.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.4D, player.getZ(), 16, 0.4D, 0.6D, 0.4D, 0.1D);
            level.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1.2D, player.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.02D);
            serverPlayer.displayClientMessage(Component.translatable("message.forgesmith.success",
                            Component.translatable(material.displayKey()).withStyle(ChatFormatting.GOLD),
                            layers.size())
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return true;
    }

    private static void fail(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.translatable(key).withStyle(ChatFormatting.RED), true);
        }
    }

    // =====================================================================
    //  属性 / 附魔重算
    // =====================================================================

    public static void recalcGear(ItemStack stack) {
        if (!ForgeData.isGear(stack)) {
            return;
        }
        if (stack.getItem() instanceof ArmorItem) {
            recalcArmor(stack);
        } else if (stack.getItem() instanceof SwordItem) {
            recalcSword(stack);
        }
        recalcUnbreaking(stack);
    }

    /** 每件盔甲的锭层加成：(护甲, 击退抗性) */
    private static double[] ingotArmorBonus(String ingotLayer) {
        return switch (ingotLayer) {
            case "iron", "gold" -> new double[]{2.0D, 0.10D};
            case "diamond" -> new double[]{3.0D, 0.15D};
            case "netherite" -> new double[]{4.0D, 0.20D};
            default -> new double[]{0.0D, 0.0D};
        };
    }

    private static String ingotLayerOf(ItemStack stack) {
        List<String> layers = ForgeData.layers(stack);
        int idx = ForgeData.firstIngotIndex(layers);
        return idx >= 0 ? layers.get(idx) : "";
    }

    private static void recalcArmor(ItemStack stack) {
        double[] bonus = ingotArmorBonus(ingotLayerOf(stack));
        double armorBonus = bonus[0];
        double kbBonus = bonus[1];
        int healthBonus = 4 * ForgeData.count(stack, "redstone");

        stack.getOrCreateTag().remove("AttributeModifiers");
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            var defaults = stack.getItem().getDefaultAttributeModifiers(slot);
            boolean ownSlot = !defaults.isEmpty();
            for (Attribute attribute : defaults.keySet()) {
                for (AttributeModifier mod : defaults.get(attribute)) {
                    double amount = mod.getAmount();
                    if (attribute == Attributes.GENERIC_ARMOR) {
                        amount += armorBonus;
                    } else if (attribute == Attributes.GENERIC_KNOCKBACK_RESISTANCE) {
                        amount += kbBonus;
                    }
                    stack.addAttributeModifier(attribute,
                            new AttributeModifier(mod.getId(), "forgesmith.base", amount, mod.getOperation()), slot);
                }
            }
            if (ownSlot && healthBonus > 0) {
                stack.addAttributeModifier(Attributes.GENERIC_MAX_HEALTH,
                        new AttributeModifier(HEALTH_BONUS_UUID, "forgesmith.health", healthBonus,
                                AttributeModifier.Operation.ADDITION), slot);
            }
        }
    }

    private static void recalcSword(ItemStack stack) {
        int swordBonus = switch (ingotLayerOf(stack)) {
            case "iron", "gold" -> 2;
            case "diamond" -> 3;
            case "netherite" -> 4;
            default -> 0;
        };
        var defaults = stack.getItem().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND);
        stack.getOrCreateTag().remove("AttributeModifiers");
        for (Attribute attribute : defaults.keySet()) {
            for (AttributeModifier mod : defaults.get(attribute)) {
                double amount = mod.getAmount();
                if (swordBonus > 0 && attribute == Attributes.GENERIC_ATTACK_DAMAGE) {
                    amount += swordBonus;
                }
                stack.addAttributeModifier(attribute,
                        new AttributeModifier(mod.getId(), "forgesmith.base", amount, mod.getOperation()),
                        EquipmentSlot.MAINHAND);
            }
        }
    }

    /** 耐久：斧的锭层 + 红石层，取最高 */
    private static void recalcUnbreaking(ItemStack stack) {
        int target = ForgeData.count(stack, "redstone");
        if (stack.getItem() instanceof AxeItem) {
            target = Math.max(target, switch (ingotLayerOf(stack)) {
                case "iron", "gold" -> 2;
                case "diamond", "netherite" -> 3;
                default -> 0;
            });
        }
        target = Math.min(3, target);
        if (target > stack.getEnchantmentLevel(Enchantments.UNBREAKING)) {
            stack.enchant(Enchantments.UNBREAKING, target);
        }
    }
}
