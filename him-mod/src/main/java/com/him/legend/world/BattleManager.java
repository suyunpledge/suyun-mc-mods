package com.him.legend.world;

import com.him.legend.HimMod;
import com.him.legend.entity.Entity303Entity;
import com.him.legend.entity.HerobrineEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 双雄之战：任意两位"战士级"传说实体（Herobrine / Entity 303 / Null）捉对厮杀。
 *
 * 社区传说（大量动画/同人作品）中，白眼 Herobrine、红眼 303 与全黑的 Null 彼此皆为宿敌，
 * 相遇必有一战。战斗演出：
 *  - 实体以 BATTLE 模式互相追杀：挥击（暴击粒子+挥砍音效）、冲击波、303 的红雷
 *  - 血量为管理器侧"幻影血量"（实体本体无敌，玩家无法干涉战局 —— 神之战只能围观）
 *  - 终局：败者被闪电+爆炸湮灭（303 化烈焰 / Herobrine 家族化黑烟电花），
 *    胜者凝视"观众"数秒后离去
 */
public final class BattleManager {
    private static final List<Battle> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Battle> BY_ENTITY = new HashMap<>();

    private BattleManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BattleManager::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Battle> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Battle battle = it.next();
            battle.tick();
            if (battle.finished) {
                it.remove();
            }
        }
    }

    public static Battle start(ServerPlayer audience, ServerLevel level, LivingEntity a, LivingEntity b) {
        Battle battle = new Battle(audience, level, a, b);
        ACTIVE.add(battle);
        BY_ENTITY.put(a.getUUID(), battle);
        BY_ENTITY.put(b.getUUID(), battle);
        setMode(a, true);
        setMode(b, true);
        Entity303Entity.spawnVisualLightning(level,
                (a.getX() + b.getX()) / 2.0D, (a.getY() + b.getY()) / 2.0D, (a.getZ() + b.getZ()) / 2.0D);
        level.playSound(null, (a.getX() + b.getX()) / 2.0D, a.getY(), (a.getZ() + b.getZ()) / 2.0D,
                SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.7F, 0.6F);
        HimMod.LOGGER.info("[HIM] The battle has begun. {} vs {} (audience: {}).",
                name(a), name(b), audience.getGameProfile().getName());
        return battle;
    }

    private static String name(LivingEntity entity) {
        if (entity instanceof Entity303Entity) {
            return "Entity 303";
        }
        if (entity != null) {
            return entity.getClass().getSimpleName();
        }
        return "?";
    }

    private static void setMode(LivingEntity entity, boolean battle) {
        if (entity instanceof HerobrineEntity hb) {
            hb.setMode(battle ? HerobrineEntity.MODE_BATTLE : HerobrineEntity.MODE_STARE);
        } else if (entity instanceof Entity303Entity e303) {
            e303.setMode(battle ? Entity303Entity.MODE_BATTLE : Entity303Entity.MODE_STARE);
        }
    }

    private static void vanishDramatic(LivingEntity entity) {
        if (entity instanceof Entity303Entity e303) {
            e303.vanish(true);
        } else if (entity instanceof HerobrineEntity hb) {
            hb.vanish(true);
        } else if (entity != null) {
            entity.discard();
        }
    }

    private static void vanishSilent(LivingEntity entity) {
        if (entity instanceof Entity303Entity e303) {
            e303.vanish(false);
        } else if (entity instanceof HerobrineEntity hb) {
            hb.vanish(false);
        } else if (entity != null) {
            entity.discard();
        }
    }

    /** 一次挥击/落雷命中：扣幻影血量并结算 */
    public static void strike(LivingEntity attacker, LivingEntity target, float amount) {
        Battle battle = BY_ENTITY.get(target.getUUID());
        if (battle == null || battle.finished) {
            return;
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1.2D, target.getZ(), 14, 0.25D, 0.4D, 0.25D, 0.15D);
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.9F);
        }
        battle.damage(target, amount);
    }

    public static void cleanup(Battle battle) {
        BY_ENTITY.remove(battle.a.getUUID());
        BY_ENTITY.remove(battle.b.getUUID());
        battle.finished = true;
    }

    public static class Battle {
        final ServerPlayer audience;
        final ServerLevel level;
        final LivingEntity a;
        final LivingEntity b;
        float aHp = 120.0F;
        float bHp = 130.0F;   // 挑战方略肉
        int ticks;
        int winnerVanishAt = -1;
        LivingEntity winner;
        boolean finished;

        Battle(ServerPlayer audience, ServerLevel level, LivingEntity a, LivingEntity b) {
            this.audience = audience;
            this.level = level;
            this.a = a;
            this.b = b;
        }

        void tick() {
            if (this.finished) {
                return;
            }
            this.ticks++;

            if (this.winnerVanishAt > 0) {
                if (this.ticks >= this.winnerVanishAt) {
                    vanishSilent(this.winner);
                    BattleManager.cleanup(this);
                }
                return;
            }

            if (this.a.isRemoved() || this.b.isRemoved()) {
                this.finishTimeout();
                return;
            }
            if (this.ticks > 1800) {
                this.finishTimeout();   // 平局：两位神明彼此忌惮，各自离去
                return;
            }

            double mx = (this.a.getX() + this.b.getX()) / 2.0D;
            double my = (this.a.getY() + this.b.getY()) / 2.0D;
            double mz = (this.a.getZ() + this.b.getZ()) / 2.0D;
            if (this.ticks % 50 == 0) {
                this.level.playSound(null, mx, my, mz,
                        SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.4F, 0.7F);
            }
            if (this.ticks % 130 == 0) {
                Entity303Entity.spawnVisualLightning(this.level,
                        mx + this.level.random.nextInt(11) - 5, my, mz + this.level.random.nextInt(11) - 5);
            }
        }

        void damage(LivingEntity target, float amount) {
            if (this.finished || this.winnerVanishAt > 0) {
                return;
            }
            if (target == this.a) {
                this.aHp -= amount;
                if (this.aHp <= 0.0F) {
                    this.end(this.b, this.a);
                }
            } else {
                this.bHp -= amount;
                if (this.bHp <= 0.0F) {
                    this.end(this.a, this.b);
                }
            }
        }

        private void end(LivingEntity winner, LivingEntity loser) {
            Entity303Entity.spawnVisualLightning(this.level, loser.getX(), loser.getY(), loser.getZ());
            this.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    loser.getX(), loser.getY() + 1.0D, loser.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (loser instanceof Entity303Entity) {
                this.level.sendParticles(ParticleTypes.FLAME,
                        loser.getX(), loser.getY() + 1.0D, loser.getZ(), 80, 0.5D, 0.9D, 0.5D, 0.05D);
                this.level.sendParticles(ParticleTypes.LAVA,
                        loser.getX(), loser.getY() + 0.8D, loser.getZ(), 8, 0.4D, 0.4D, 0.4D, 0.0D);
            } else {
                this.level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        loser.getX(), loser.getY() + 1.0D, loser.getZ(), 70, 0.5D, 0.9D, 0.5D, 0.04D);
                this.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        loser.getX(), loser.getY() + 1.2D, loser.getZ(), 50, 0.5D, 0.8D, 0.5D, 0.1D);
            }
            this.level.playSound(null, loser.getX(), loser.getY(), loser.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.2F, 0.6F);
            loser.discard();

            setMode(winner, false);   // 胜者凝视"观众"
            this.winner = winner;
            this.winnerVanishAt = this.ticks + 70;
            HimMod.LOGGER.info("[HIM] The battle is over. Winner: {}", name(winner));
        }

        private void finishTimeout() {
            vanishSilent(this.a);
            vanishSilent(this.b);
            BattleManager.cleanup(this);
        }
    }
}
