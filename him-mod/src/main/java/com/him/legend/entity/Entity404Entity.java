package com.him.legend.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 实体 404：绿色故障体。不走路 —— 以"闪烁瞬移"逼近玩家，
 * 沿途向聊天栏刷出错误文本；贴身时触发"假断线"惊吓：
 * 标题【连接已断开 错误代码：404】+ 黑暗/反胃/迟缓 + 雷声，随后消散。
 */
public class Entity404Entity extends PathfinderMob {
    private int blinkCooldown = 40;
    private int errorCooldown = 60;
    private int climaxTicks = -1;

    public Entity404Entity(EntityType<? extends Entity404Entity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        // 不注册任何寻路目标：404 只闪烁，不行走
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 高潮演出进行中：静等结束（结束在本体 discard）
        if (this.climaxTicks >= 0) {
            this.climaxTicks++;
            if (this.climaxTicks == 8) {
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.8F, 1.6F);
            }
            if (this.climaxTicks > 45) {
                this.vanish();
            }
            return;
        }

        Player target = this.level().getNearestPlayer(this, 64.0D);
        if (target == null) {
            return;
        }
        this.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());

        // 沿途刷错误文本
        if (--this.errorCooldown <= 0) {
            this.errorCooldown = 70 + this.random.nextInt(80);
            if (target instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable(
                                "error.him.404." + (1 + this.random.nextInt(6)))
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            }
        }

        // 闪烁瞬移逼近
        if (--this.blinkCooldown <= 0) {
            this.blinkCooldown = 20 + this.random.nextInt(20);
            Vec3 toTarget = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
            double dist = toTarget.length();
            if (dist < 7.0D) {
                this.startClimax(serverLevel, target);
                return;
            }
            double step = Math.min(6.0D, dist - 7.0D);
            Vec3 dir = toTarget.normalize();
            double nx = this.getX() + dir.x * step + (this.random.nextDouble() - 0.5D) * 2.0D;
            double nz = this.getZ() + dir.z * step + (this.random.nextDouble() - 0.5D) * 2.0D;
            int ny = this.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) Math.floor(nx), (int) Math.floor(nz));
            if (Math.abs(ny - this.getBlockY()) > 6) {
                return;
            }
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 10, 0.25D, 0.5D, 0.25D, 0.02D);
            this.teleportTo(nx, ny, nz);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    nx, ny + 1.0D, nz, 10, 0.25D, 0.5D, 0.25D, 0.02D);
        }
    }

    /** 假断线惊吓 */
    private void startClimax(ServerLevel level, Player target) {
        this.climaxTicks = 0;
        this.getNavigation().stop();
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5, 35, 15));
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    Component.translatable("title.him.404.line1").withStyle(ChatFormatting.DARK_RED)));
            serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("title.him.404.line2").withStyle(ChatFormatting.GRAY)));
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false, false));
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, false));
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, false, false));
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.4F, 0.4F);
        level.sendParticles(ParticleTypes.ASH,
                this.getX(), this.getY() + 1.2D, this.getZ(), 60, 0.6D, 1.0D, 0.6D, 0.03D);
    }

    /** 故障消散 */
    public void vanish() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.ASH,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 40, 0.4D, 0.8D, 0.4D, 0.05D);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.4F);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 攻击他？——他只是"闪烁"到了别处（故障体不与你纠缠）
        if (!this.level().isClientSide && source.getEntity() instanceof Player) {
            this.vanish();
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
