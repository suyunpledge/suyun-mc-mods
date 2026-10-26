package com.realcraft.event;

import com.realcraft.config.RealCraftConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 持续型现实化（每 2 秒结算一次）：
 *  - 负重系统：背包物品件数超过软/硬上限 → 缓慢 I / II
 *  - 深井高温：主世界 y 低于阈值 → 挖掘疲劳，且每 10 秒一次灼热伤害
 *    （抗火药水或泡在水里可以缓解 —— 现实矿井靠通风与降温）
 */
public final class PlayerTicker {
    private static int counter;

    private PlayerTicker() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerTicker::tick);
    }

    private static void tick(MinecraftServer server) {
        if (++counter % 40 != 0) {
            return;
        }
        RealCraftConfig cfg = RealCraftConfig.get();
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (player.getAbilities().instab) {
                    continue; // 创造/旁观者不受现实之苦
                }
                if (cfg.enableWeight) {
                    tickWeight(player, cfg);
                }
                if (cfg.enableDeepHeat && level.dimension() == Level.OVERWORLD) {
                    tickHeat(player, level, cfg);
                }
            }
        }
    }

    private static void tickWeight(ServerPlayer player, RealCraftConfig cfg) {
        int weight = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                weight += stack.getCount();
            }
        }
        if (weight > cfg.weightHard) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 1, false, false, false));
            if (counter % 120 == 0) {
                player.displayClientMessage(Component.translatable("message.realcraft.overweight"), true);
            }
        } else if (weight > cfg.weightSoft) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 0, false, false, false));
            if (counter % 120 == 0) {
                player.displayClientMessage(Component.translatable("message.realcraft.heavy"), true);
            }
        }
    }

    private static void tickHeat(ServerPlayer player, ServerLevel level, RealCraftConfig cfg) {
        if (player.getBlockY() >= cfg.deepHeatY) {
            return;
        }
        boolean cooled = player.isInWater() || player.hasEffect(MobEffects.FIRE_RESISTANCE);
        if (cooled) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0, false, false, false));
        if (counter % 200 == 0) {
            player.hurt(level.damageSources().hotFloor(), 1.0F);
            player.displayClientMessage(Component.translatable("message.realcraft.deep_heat"), true);
            level.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 6, 0.3D, 0.6D, 0.3D, 0.01D);
        }
    }
}
