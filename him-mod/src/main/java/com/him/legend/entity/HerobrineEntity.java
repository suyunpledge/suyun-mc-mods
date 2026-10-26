package com.him.legend.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import com.him.legend.world.BattleManager;

/**
 * Herobrine 本体。
 *
 * 还原传说核心：他几乎从不直接攻击玩家，只是"出现 —— 凝视 —— 消失"。
 * MODE_STARE:    站定凝视最近的玩家（雾中目击）
 * MODE_WANDER:   附近徘徊（被召唤后偶尔游荡）
 * MODE_APPROACH: 缓慢逼近玩家（深夜事件；靠得太近会"切断直播"——黑暗+雷声）
 */
public class HerobrineEntity extends PathfinderMob {
    public static final int MODE_STARE = 0;
    public static final int MODE_WANDER = 1;
    public static final int MODE_APPROACH = 2;
    public static final int MODE_BATTLE = 3;

    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(HerobrineEntity.class, EntityDataSerializers.INT);

    private int wanderCooldown;
    private int battleCooldown;
    private int shockwaveCooldown;
    /** 逼近事件中被玩家注视时定身 */
    private boolean frozen;

    public HerobrineEntity(EntityType<? extends HerobrineEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.STEP_HEIGHT, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MODE, MODE_STARE);
    }

    public int getMode() {
        return this.entityData.get(DATA_MODE);
    }

    public void setMode(int mode) {
        this.entityData.set(DATA_MODE, mode);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Player target = this.level().getNearestPlayer(this, 64.0D);
        int mode = this.getMode();
        if (mode == MODE_STARE) {
            this.getNavigation().stop();
            if (target != null) {
                this.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
            }
        } else if (mode == MODE_WANDER) {
            if (--this.wanderCooldown <= 0 && this.getNavigation().isDone()) {
                this.wanderCooldown = 60 + this.random.nextInt(160);
                BlockPos around = this.blockPosition().offset(
                        this.random.nextInt(17) - 8, 0, this.random.nextInt(17) - 8);
                int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, around.getX(), around.getZ());
                this.getNavigation().moveTo(around.getX() + 0.5D, y, around.getZ() + 0.5D, 0.6D);
            }
        } else if (mode == MODE_APPROACH) {
            if (target != null) {
                this.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
                if (this.frozen) {
                    // 你看着他，他就停下 —— 移开视线，他才会继续逼近
                    this.getNavigation().stop();
                } else {
                    this.getNavigation().moveTo(target, 0.9D);
                }
            }
        } else if (mode == MODE_BATTLE) {
            // 双雄之战：宿敌（Entity 303 / Null）现身，Herobrine 拔剑应战
            LivingEntity foe = this.findFoe();
            if (foe == null) {
                this.setMode(MODE_STARE);
                return;
            }
            this.getLookControl().setLookAt(foe.getX(), foe.getEyeY(), foe.getZ());
            this.getNavigation().moveTo(foe, 1.0D);
            double d = this.distanceTo(foe);
            if (d < 2.3D && --this.battleCooldown <= 0) {
                this.battleCooldown = 18 + this.random.nextInt(10);
                this.swing(InteractionHand.MAIN_HAND);
                BattleManager.strike(this, foe, 9.0F);
            }
            // 冲击波：把宿敌轰退（传说动画中 Herobrine 的念力/剑压）
            if (d < 4.0D && --this.shockwaveCooldown <= 0) {
                this.shockwaveCooldown = 220 + this.random.nextInt(140);
                double dx = foe.getX() - this.getX();
                double dz = foe.getZ() - this.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.01D) {
                    foe.knockback(1.1D, -dx / len, -dz / len);
                }
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            foe.getX(), foe.getY() + 1.0D, foe.getZ(), 25, 0.4D, 0.5D, 0.4D, 0.25D);
                    serverLevel.playSound(null, foe.getX(), foe.getY(), foe.getZ(),
                            net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                            net.minecraft.sounds.SoundSource.HOSTILE, 1.2F, 0.6F);
                }
                BattleManager.strike(this, foe, 6.0F);
            }
        }
    }

    private LivingEntity findFoe() {
        return Legends.findRival(this, 48.0D);
    }

    /** 消失。smoke=true：黑烟 + 灵魂粒子 + 音效（被注视/被逼近时）；false：无声无息地不见（你转过头，他就不在了） */
    public void vanish(boolean smoke) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            if (smoke) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX(), this.getY() + 1.0D, this.getZ(), 60, 0.3D, 0.9D, 0.3D, 0.03D);
                serverLevel.sendParticles(ParticleTypes.SOUL,
                        this.getX(), this.getY() + 1.2D, this.getZ(), 10, 0.3D, 0.6D, 0.3D, 0.02D);
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PARTICLE_SOUL_ESCAPE, SoundSource.HOSTILE, 1.0F, 0.55F);
            }
        }
        this.discard();
    }

    // 他是沉默的 —— 没有任何环境音/受伤音
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    // 永不被系统自然清除：何时出现、何时消失由 HerobrineManager 决定
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    /** 攻击他？——他化作黑烟消失了。（双雄之战中为幻影之躯，无法被干预） */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!this.level().isClientSide && this.getMode() != MODE_BATTLE
                && source.getEntity() instanceof Player) {
            this.vanish(true);
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HimMode", this.getMode());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setMode(tag.getInt("HimMode"));
    }

    public static float yawToward(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) (Math.toDegrees(Math.atan2(-dx, dz)));
    }
}
