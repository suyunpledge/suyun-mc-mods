package com.him.legend.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 实体 Null：全黑人形，沉默，"损坏世界"者。
 *
 * 传说（约 2012-2013 年兴起）：存档损坏、方块被替换、沉默的黑影；
 * 视频系列中常被设定为 Herobrine 的宿敌。
 * 继承 Herobrine 的全部模式（凝视/逼近/战斗），事件与外观由管理器与渲染器区分。
 */
public class NullEntity extends HerobrineEntity {
    public NullEntity(EntityType<? extends NullEntity> type, Level level) {
        super(type, level);
    }
}
