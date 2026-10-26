package com.bladecombo.combo;

import java.util.UUID;

/**
 * 玩家连招状态（服务端内存）。
 */
public class ComboState {
    /** 当前连招段（1-4 循环） */
    public int stage;
    /** 连招窗口截止（超过则回到第一段） */
    public long windowEnd;
    /** 下一次允许斩击的时间（防双击双判） */
    public long nextSwingAt;
    /** 攻击间隔内缓存的一次目标点击 */
    public UUID bufferedTarget;
    /** 最近一次命中的时间（格挡衔接判定用） */
    public long lastHitAt = Long.MIN_VALUE;
    /** 格挡架势截止 */
    public long parryUntil;
    /** 格挡冷却截止 */
    public long parryReadyAt;
    /** 空中连段截止（期间摔落伤害免疫） */
    public long airComboUntil;
    /** 本轮连击总数 */
    public int totalHits;
}
