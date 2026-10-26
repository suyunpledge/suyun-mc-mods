package com.forgesmith.event;

import com.forgesmith.config.ForgeSmithConfig;
import com.forgesmith.forge.ForgeData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import javax.annotation.Nullable;

/**
 * 战斗事件：
 *  - 工具/武器绿宝石层：满蓄力命中 → 吸血（伤害 x 比例 x 层数）
 *  - 盔甲青金石层（水抗）：1 层免疫溺水；2 层再免疫冻伤；3 层在水中受击回补 20%
 */
public final class CombatEvents {

    private CombatEvents() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register(CombatEvents::onAttack);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CombatEvents::onAllowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CombatEvents::onAfterDamage);
    }

    private static InteractionResult onAttack(Player player, Level level, net.minecraft.world.InteractionHand hand,
                                              Entity target, @Nullable EntityHitResult hitResult) {
        if (level.isClientSide || !(player instanceof ServerPlayer attacker)) {
            return InteractionResult.PASS;
        }
        ItemStack weapon = attacker.getMainHandItem();
        int emerald = ForgeData.count(weapon, "emerald");
        if (emerald <= 0 || attacker.getAttackStrengthScale(0.5F) < 0.9F) {
            return InteractionResult.PASS;
        }
        double damage = attacker.getAttributeValue(Attributes.GENERIC_ATTACK_DAMAGE);
        float heal = (float) (damage * ForgeSmithConfig.get().lifestealPerLayer * emerald);
        if (heal > 0.0F && attacker.getHealth() < attacker.getMaxHealth()) {
            attacker.heal(heal);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        attacker.getX(), attacker.getY() + 1.6D, attacker.getZ(), 3, 0.25D, 0.3D, 0.25D, 0.0D);
            }
        }
        return InteractionResult.PASS;
    }

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity instanceof ServerPlayer player) {
            int lapis = ForgeData.armorLayerCount(player, "lapis");
            if (lapis >= 1 && source.is(DamageTypes.DROWN)) {
                return false;   // 水抗 I：免疫溺水
            }
            if (lapis >= 2 && source.is(DamageTypes.FREEZE)) {
                return false;   // 水抗 II：免疫冻伤（细雪）
            }
        }
        return true;
    }

    private static void onAfterDamage(LivingEntity entity, DamageSource source, float amount, boolean handled) {
        if (entity instanceof ServerPlayer player && amount > 0.0F
                && ForgeData.armorLayerCount(player, "lapis") >= 3
                && player.isInWater() && !source.is(DamageTypes.DROWN)) {
            player.heal(amount * ForgeSmithConfig.get().waterHealRatio);
        }
    }
}
