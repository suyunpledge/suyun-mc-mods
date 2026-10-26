package com.forgesmith.event;

import com.forgesmith.forge.ForgeData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;

/**
 * 周期效果（每 2 秒）：
 *  - 盔甲绿宝石层：常驻生命恢复（1-2 层 Regeneration I，3+ 层 II）
 *  - 主手工具青金石层：潮涌能量（水下输出 + 挖掘）
 *  - 主手镐/铲锭层：急迫（铁/金 I；钻石/下界合金 II）—— 挖掘速度
 */
public final class EquipmentTicker {
    private static int counter;

    private EquipmentTicker() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(EquipmentTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        if (++counter % 40 != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                tickPlayer(player);
            }
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        // 盔甲：绿宝石层 → 再生
        int emerald = ForgeData.armorLayerCount(player, "emerald");
        if (emerald > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80,
                    emerald >= 3 ? 1 : 0, false, false, true));
        }

        // 主手工具
        ItemStack main = player.getMainHandItem();
        if (ForgeData.count(main, "lapis") > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 60, 0, false, false, true));
        }
        if (main.getItem() instanceof PickaxeItem || main.getItem() instanceof ShovelItem) {
            String ingot = ingotLayer(main);
            int haste = switch (ingot) {
                case "iron", "gold" -> 0;
                case "diamond", "netherite" -> 1;
                default -> -1;
            };
            if (haste >= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, haste, false, false, true));
            }
        }
    }

    private static String ingotLayer(ItemStack stack) {
        var layers = ForgeData.layers(stack);
        int idx = ForgeData.firstIngotIndex(layers);
        return idx >= 0 ? layers.get(idx) : "";
    }
}
