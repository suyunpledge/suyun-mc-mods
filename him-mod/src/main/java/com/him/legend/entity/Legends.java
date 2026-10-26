package com.him.legend.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import javax.annotation.Nullable;

/**
 * 传说实体公共工具：战士级判定（可参战者）与宿敌查找。
 * 战士级 = Herobrine 家族（含 Null）与 Entity 303；404/白眼不参战。
 */
public final class Legends {
    private Legends() {
    }

    public static boolean isFighter(LivingEntity entity) {
        return entity instanceof HerobrineEntity || entity instanceof Entity303Entity;
    }

    /** 最近的"其他种类"战士级实体（排除同类与自身） */
    @Nullable
    public static LivingEntity findRival(PathfinderMob self, double radius) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity entity : self.level().getEntitiesOfClass(LivingEntity.class,
                self.getBoundingBox().inflate(radius), Legends::isFighter)) {
            if (entity == self || entity.getClass() == self.getClass() || !entity.isAlive()) {
                continue;
            }
            double d = self.distanceToSqr(entity);
            if (d < bestDist) {
                bestDist = d;
                best = entity;
            }
        }
        return best;
    }
}
