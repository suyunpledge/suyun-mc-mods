package com.him.legend.entity;

import com.him.legend.world.BattleManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;


/**
 * 实体 303（Entity 303）。
 *
 * 传说：被 Mojang 开除的员工（工号 303）的怨灵，苍白脸、黑兜帽、发着红光的眼睛。
 * 与"只凝视"的 Herobrine 不同 —— 303 是真正怀有敌意的：
 *  - MODE_STALK:   追猎玩家，近身火焰之爪（伤害 + 点燃）
 *  - MODE_BATTLE:  与 Herobrine 死战（BattleManager 安排）
 *  - MODE_STARE:   战胜后短暂凝视"观众"
 */
public class Entity303Entity extends PathfinderMob {
    public static final int MODE_STALK = 0;
    public static final int MODE_BATTLE = 1;
    public static final int MODE_STARE = 2;

    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(Entity303Entity.class, EntityDataSerializers.INT);

    private int clawCooldown;
    private int lightningCooldown = 140;

    public Entity303Entity(EntityType<? extends Entity303Entity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)   // 比 Herobrine 更快
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.STEP_HEIGHT, 0.6D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MODE, MODE_STALK);
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
        int mode = this.getMode();

        if (mode == MODE_BATTLE) {
            LivingEntity foe = this.findRival();
            if (foe == null) {
                this.setMode(MODE_STALK);
                return;
            }
            this.getLookControl().setLookAt(foe.getX(), foe.getEyeY(), foe.getZ());
            this.getNavigation().moveTo(foe, 1.05D);
            double d = this.distanceTo(foe);
            if (d < 2.4D && --this.clawCooldown <= 0) {
                this.clawCooldown = 16 + this.random.nextInt(10);
                this.swing(InteractionHand.MAIN_HAND);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            foe.getX(), foe.getY() + 1.2D, foe.getZ(), 10, 0.3D, 0.4D, 0.3D, 0.03D);
                    serverLevel.playSound(null, foe.getX(), foe.getY(), foe.getZ(),
                            SoundEvents.ENTITY_BLAZE_SHOOT, SoundSource.HOSTILE, 0.8F, 0.7F);
                }
                BattleManager.strike(this, foe, 8.0F);
            }
            // 红雷：定期一道视觉闪电劈向 Herobrine，并造成真实（幻影血量）伤害
            if (--this.lightningCooldown <= 0 && d < 40.0D) {
                this.lightningCooldown = 170 + this.random.nextInt(120);
                spawnVisualLightning((ServerLevel) this.level(), foe.getX(), foe.getY(), foe.getZ());
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                            foe.getX(), foe.getY() + 1.0D, foe.getZ(), 2, 0.2D, 0.3D, 0.2D, 0.0D);
                    serverLevel.playSound(null, foe.getX(), foe.getY(), foe.getZ(),
                            SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.0F, 0.65F);
                }
                BattleManager.strike(this, foe, 14.0F);
            }
        } else if (mode == MODE_STALK) {
            Player target = this.level().getNearestPlayer(this, 48.0D);
            if (target != null) {
                this.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
                this.getNavigation().moveTo(target, 1.15D);
                double d = this.distanceTo(target);
                if (d < 1.9D && --this.clawCooldown <= 0) {
                    this.clawCooldown = 24 + this.random.nextInt(12);
                    this.swing(InteractionHand.MAIN_HAND);
                    target.hurt(this.level().damageSources().mobAttack(this), 5.0F);
                    target.setSecondsOnFire(4);
                    // 灼烧之爪：短暂黑暗恐惧
                    target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.FLAME,
                                target.getX(), target.getY() + 1.0D, target.getZ(), 14, 0.3D, 0.5D, 0.3D, 0.04D);
                        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.ENTITY_BLAZE_SHOOT, SoundSource.HOSTILE, 0.9F, 0.6F);
                    }
                }
            }
        } else if (mode == MODE_STARE) {
            this.getNavigation().stop();
            Player target = this.level().getNearestPlayer(this, 48.0D);
            if (target != null) {
                this.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
            }
        }
    }

    private LivingEntity findRival() {
        return Legends.findRival(this, 48.0D);
    }

    /** 303 的退场：烈焰散去，可附一道劈在自己身上的红雷 */
    public void vanish(boolean redLightning) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 60, 0.4D, 0.9D, 0.4D, 0.03D);
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    this.getX(), this.getY() + 0.8D, this.getZ(), 6, 0.3D, 0.4D, 0.3D, 0.0D);
            if (redLightning) {
                spawnVisualLightning(serverLevel, this.getX(), this.getY(), this.getZ());
            }
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.4F);
        }
        this.discard();
    }

    public static void spawnVisualLightning(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(x, y, z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    // 同样沉默
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** 攻击他？——他在烈焰中散去。（双雄之战中为幻影之躯，无法被干预） */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.getMode() != MODE_BATTLE
                && source.getEntity() instanceof Player) {
            this.vanish(false);
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
}
