package com.realcraft.event;

import com.realcraft.config.RealCraftConfig;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 现实伐木：砍断树干后，上方的原木整段"倒下"（作为掉落物落地），
 * 树冠的树叶随之散落消失。现实里树不会悬空。
 */
public final class TreeEvents {

    private TreeEvents() {
    }

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(TreeEvents::afterBreak);
    }

    private static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        RealCraftConfig cfg = RealCraftConfig.get();
        if (!cfg.enableTreeFelling || !state.is(BlockTags.LOGS)) {
            return;
        }

        // 向上收集同种原木（同一棵树干）
        int maxLogs = Math.max(1, cfg.treeFellMaxLogs);
        List<BlockPos> logs = new ArrayList<>();
        BlockPos p = pos.above();
        while (logs.size() < maxLogs) {
            BlockState s = serverLevel.getBlockState(p);
            if (!s.is(state.getBlock())) {
                break;
            }
            logs.add(p);
            p = p.above();
        }
        if (logs.isEmpty()) {
            return;
        }

        BlockPos lowest = pos.above();
        BlockPos highest = logs.get(logs.size() - 1);
        for (BlockPos logPos : logs) {
            // 创造模式与原版一致：不产生掉落物
            serverLevel.destroyBlock(logPos, !player.isCreative(), null);
        }
        // 树冠散落（半径 2，树叶直接消散）
        BlockPos min = lowest.offset(-2, 0, -2);
        BlockPos max = highest.offset(2, 3, 2);
        for (BlockPos lp : BlockPos.betweenClosed(min, max)) {
            if (serverLevel.getBlockState(lp).is(BlockTags.LEAVES)) {
                serverLevel.destroyBlock(lp.immutable(), false);
            }
        }

        serverLevel.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.2F, 0.7F);
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                40, 1.0D, Math.min(4.0D, logs.size() * 0.5D + 1.0D), 1.0D, 0.08D);
    }
}
