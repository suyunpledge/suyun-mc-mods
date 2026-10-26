package com.realcraft.event;

import com.realcraft.config.RealCraftConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 真实摔落：现实中从高处跳下不会只掉血条 —— 会扭伤、会骨折。
 *  - 超过"跛行"阈值（默认 5 格）：短暂缓慢（跛行）
 *  - 超过"骨折"阈值（默认 10 格）：缓慢 II + 虚弱（骨折）
 */
public final class FallEvents {

    private FallEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(FallEvents::onDamage);
    }

    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        RealCraftConfig cfg = RealCraftConfig.get();
        if (!cfg.enableFractures || !(entity instanceof ServerPlayer player)) {
            return true;
        }
        if (!source.is(DamageTypes.FALL)) {
            return true;
        }

        // 直接取真实摔落距离（不受摔落保护/轻灵附魔造成的伤害减免影响）
        float fallBlocks = player.fallDistance;
        if (fallBlocks >= cfg.fractureFallBlocks) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 0, false, false, true));
            player.displayClientMessage(Component.translatable("message.realcraft.fracture"), true);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.POOF,
                        player.getX(), player.getY() + 0.6D, player.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.03D);
            }
        } else if (fallBlocks >= cfg.limpFallBlocks) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0, false, false, true));
            player.displayClientMessage(Component.translatable("message.realcraft.limp"), true);
        }
        return true;
    }
}
