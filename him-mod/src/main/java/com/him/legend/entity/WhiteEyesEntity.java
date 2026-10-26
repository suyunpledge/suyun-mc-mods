package com.him.legend.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 白眼（White Eyes）：2010 年 7 月 Herobrine 传说的"前身"。
 *
 * 原始帖子：听过 13 号唱片后，黑暗洞穴中出现一双发着白光的眼睛，
 * 远远注视，从不靠近。女性形象的描述（Endorphine 帖）。
 * 只在黑暗中现身，被火把照亮即无声消失。
 */
public class WhiteEyesEntity extends HerobrineEntity {
    public WhiteEyesEntity(EntityType<? extends WhiteEyesEntity> type, Level level) {
        super(type, level);
    }
}
