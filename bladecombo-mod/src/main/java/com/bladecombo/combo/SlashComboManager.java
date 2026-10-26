package com.bladecombo.combo;

import com.bladecombo.config.BladeConfig;
import com.bladecombo.item.SlashBladeItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 拔刀连斩核心：
 *  - AttackEntityCallback：接管拔刀剑攻击 → 固定四段连招（无限循环）
 *  - ServerTickEvents：空中连段期间持续清零摔落距离
 *  - ALLOW_DAMAGE：空中连段免疫摔落 / 格挡架势减伤 + 弹刀反伤
 */
public final class SlashComboManager {
    private static final Map<UUID, ComboState> STATES = new HashMap<>();
    /** 格挡手动补刀时的递归保护 */
    private static boolean applyingParryDamage = false;
    private static int purgeCounter = 0;

    private SlashComboManager() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register(SlashComboManager::onAttack);
        ServerTickEvents.END_SERVER_TICK.register(SlashComboManager::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(SlashComboManager::onAllowDamage);
    }

    public static ComboState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), uuid -> new ComboState());
    }

    // =====================================================================
    //  连招攻击
    // =====================================================================

    private static InteractionResult onAttack(Player player, Level level, InteractionHand hand,
                                              Entity target, EntityHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND || !(target instanceof LivingEntity livingTarget)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SlashBladeItem)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer player2)) {
            return InteractionResult.FAIL;
        }

        ComboState cs = state(player2);
        long now = level.getGameTime();
        if (now < cs.nextSwingAt) {
            cs.bufferedTarget = livingTarget.getUUID();
            return InteractionResult.FAIL;
        }
        performSlash(player2, livingTarget, stack);
        return InteractionResult.FAIL;
    }

    private static void performSlash(ServerPlayer player2, LivingEntity livingTarget, ItemStack stack) {
        Level level = player2.level();
        BladeConfig cfg = BladeConfig.get();
        ComboState cs = state(player2);
        long now = level.getGameTime();
        cs.bufferedTarget = null;
        if (now > cs.windowEnd) {
            cs.stage = 1;
            cs.totalHits = 0;
        } else {
            cs.stage = cs.stage % 4 + 1;
        }
        cs.windowEnd = now + cfg.comboWindowTicks;
        cs.nextSwingAt = now + cfg.swingIntervalTicks;
        cs.lastHitAt = now;
        cs.totalHits++;

        float mult = switch (cs.stage) {
            case 2 -> 1.2F;
            case 3 -> 1.5F;
            case 4 -> 2.0F;
            default -> 1.0F;
        };
        float base = (float) player2.getAttributeValue(Attributes.ATTACK_DAMAGE);

        // 拔刀剑手感：朝目标小幅突进
        Vec3 dir = new Vec3(livingTarget.getX() - player2.getX(), 0.0D, livingTarget.getZ() - player2.getZ());
        double len = dir.length();
        if (len > 0.3D) {
            double boost = cs.stage == 4 ? 0.35D : 0.2D;
            player2.setDeltaMovement(player2.getDeltaMovement()
                    .add(dir.x / len * boost, 0.0D, dir.z / len * boost));
            player2.hurtMarked = true;
        }

        boolean hurt = livingTarget.hurt(player2.damageSources().playerAttack(player2), base * mult);
        if (hurt && len > 0.01D) {
            double knock = cs.stage == 4 ? 1.2D : 0.45D;
            livingTarget.knockback(knock, -dir.x / len, -dir.z / len);
            if (cs.stage == 2) {   // 逆袈裟：挑空
                livingTarget.setDeltaMovement(livingTarget.getDeltaMovement().add(0.0D, 0.25D, 0.0D));
            }
        }

        // 三之太刀·回旋斩：360° 范围
        if (cs.stage == 3) {
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                    player2.getBoundingBox().inflate(2.5D),
                    e -> e != player2 && e != livingTarget && e.isAlive() && !(e instanceof Player))) {
                other.hurt(player2.damageSources().playerAttack(player2), base * 0.7F);
                other.knockback(0.4D, player2.getX() - other.getX(), player2.getZ() - other.getZ());
            }
            if (level instanceof ServerLevel serverLevel) {
                for (int a = 0; a < 8; a++) {
                    double ang = a * Math.PI / 4.0D;
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            player2.getX() + Math.cos(ang) * 1.6D,
                            player2.getY() + 0.9D,
                            player2.getZ() + Math.sin(ang) * 1.6D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        // 演出与音效
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() * 0.5D, livingTarget.getZ(),
                    cs.stage == 4 ? 12 : 4, 0.2D, 0.3D, 0.2D, 0.1D);
            if (cs.stage == 4) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        livingTarget.getX(), livingTarget.getY() + 0.8D, livingTarget.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.7F);
            } else {
                serverLevel.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F,
                        1.0F + cs.stage * 0.1F);
            }
        }

        // 空中连段：滞空斩击 → 免疫摔落
        if (!player2.onGround()) {
            cs.airComboUntil = now + cfg.airComboTicks;
            player2.fallDistance = 0.0F;
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        player2.getX(), player2.getY() + 0.2D, player2.getZ(), 4, 0.3D, 0.1D, 0.3D, 0.02D);
            }
        }

        // 击杀计数
        if (hurt && livingTarget.isDeadOrDying()) {
            SlashBladeItem.addKill(stack);
        }

        com.bladecombo.network.BladeComboNetworking.sendSlash(player2, cs.stage);
        player2.resetAttackStrengthTicker();
        player2.swing(InteractionHand.MAIN_HAND, true);
        player2.displayClientMessage(Component.translatable("message.bladecombo.combo",
                cs.totalHits, Component.translatable("combo.bladecombo.stage" + cs.stage)), true);
    }

    // =====================================================================
    //  周期结算
    // =====================================================================

    private static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            long now = level.getGameTime();
            for (ServerPlayer player : level.players()) {
                ComboState cs = STATES.get(player.getUUID());
                if (cs == null) {
                    continue;
                }
                if (!player.onGround() && now < cs.airComboUntil) {
                    player.fallDistance = 0.0F;
                }
                if (!(player.getMainHandItem().getItem() instanceof SlashBladeItem)) {
                    cs.bufferedTarget = null;
                }
                if (cs.bufferedTarget != null && now >= cs.nextSwingAt) {
                    UUID targetId = cs.bufferedTarget;
                    cs.bufferedTarget = null;
                    Entity buffered = level.getEntity(targetId);
                    if (player.isAlive() && player.getMainHandItem().getItem() instanceof SlashBladeItem
                            && buffered instanceof LivingEntity livingTarget && livingTarget.isAlive()
                            && player.distanceToSqr(livingTarget) <= 20.25D
                            && player.hasLineOfSight(livingTarget)) {
                        performSlash(player, livingTarget, player.getMainHandItem());
                    }
                }
            }
        }
        if (++purgeCounter > 1200) {
            purgeCounter = 0;
            Set<UUID> online = new HashSet<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                online.add(player.getUUID());
            }
            STATES.keySet().removeIf(uuid -> !online.contains(uuid));
        }
    }

    // =====================================================================
    //  伤害拦截：摔落免疫 / 格挡
    // =====================================================================

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        ComboState cs = STATES.get(player.getUUID());
        if (cs == null) {
            return true;
        }
        long now = player.level().getGameTime();

        // 空中连段：免疫摔落
        if (source.is(DamageTypes.FALL) && now < cs.airComboUntil) {
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.POOF,
                        player.getX(), player.getY() + 0.2D, player.getZ(), 8, 0.3D, 0.1D, 0.3D, 0.03D);
            }
            player.displayClientMessage(
                    Component.translatable("message.bladecombo.air"), true);
            return false;
        }

        // 格挡架势
        if (now < cs.parryUntil && !applyingParryDamage) {
            BladeConfig cfg = BladeConfig.get();
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.3F);
                level.sendParticles(ParticleTypes.CRIT,
                        player.getX(), player.getY() + 1.2D, player.getZ(), 10, 0.4D, 0.5D, 0.4D, 0.2D);
            }
            // 弹刀：近身攻击者被反伤 + 击退 + 迟缓
            if (source.getEntity() instanceof LivingEntity attacker
                    && attacker != player && player.distanceTo(attacker) <= 5.0F
                    && !(attacker instanceof Player)) {
                attacker.hurt(player.damageSources().playerAttack(player),
                        amount * (float) cfg.parryReflectFactor);
                Vec3 away = new Vec3(attacker.getX() - player.getX(), 0.0D, attacker.getZ() - player.getZ());
                double awayLen = away.length();
                if (awayLen > 0.01D) {
                    attacker.knockback(0.8D, -away.x / awayLen, -away.z / awayLen);
                }
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        60, 1, false, false, true));
                player.displayClientMessage(
                        Component.translatable("message.bladecombo.parry_hit"), true);
            }
            // 减伤：取消本次伤害，按比例补一击
            float reduced = amount * (float) cfg.parryDamageFactor;
            if (reduced >= 0.5F) {
                applyingParryDamage = true;
                try {
                    player.hurt(source, reduced);
                } finally {
                    applyingParryDamage = false;
                }
            }
            com.bladecombo.network.BladeComboNetworking.sendParryImpact(player);
            return false;
        }
        return true;
    }
}
