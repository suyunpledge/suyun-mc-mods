package com.him.legend.world;

import com.him.legend.HimMod;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 经典召唤图腾（2010 年社区流传的仪式）：
 *
 *        [下界岩]        ← 用打火石点燃
 *     [红石火把 x4]       ← 贴在上层金块四个侧面
 *       [金 块]
 *       [金 块]
 *
 * 判定：对结构顶端的下界岩使用打火石/火焰弹时检查形状。
 * 点燃成功 → beginHaunt()，该玩家进入 stage 1。
 */
public final class TotemHandler {

    private TotemHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            if (!player.getMainHandItem().is(Items.FLINT_AND_STEEL)
                    && !player.getMainHandItem().is(Items.FIRE_CHARGE)) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hitResult.getBlockPos();
            if (!level.getBlockState(pos).is(Blocks.NETHERRACK)) {
                return InteractionResult.PASS;
            }
            if (checkTotem(serverLevel, pos)) {
                HimMod.LOGGER.info("[HIM] {} lit the totem.", serverPlayer.getGameProfile().getName());
                serverLevel.playSound(null, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.0F, 0.5F);
                HerobrineManager.beginHaunt(serverPlayer, serverLevel, pos);
            }
            return InteractionResult.PASS; // 不拦截：让 vanilla 正常点起永恒之火
        });
    }

    private static boolean checkTotem(ServerLevel level, BlockPos netherrackPos) {
        BlockPos gold1 = netherrackPos.below();
        BlockPos gold2 = netherrackPos.below(2);
        if (!level.getBlockState(gold1).is(Blocks.GOLD_BLOCK)
                || !level.getBlockState(gold2).is(Blocks.GOLD_BLOCK)) {
            return false;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos torchPos = gold1.relative(dir);
            BlockState torch = level.getBlockState(torchPos);
            if (!torch.is(Blocks.REDSTONE_WALL_TORCH)
                    || torch.getValue(RedstoneWallTorchBlock.FACING) != dir.getOpposite()) {
                return false;
            }
        }
        return true;
    }
}
