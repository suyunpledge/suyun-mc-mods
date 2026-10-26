package com.realcraft.event;

import com.realcraft.RealCraftMod;
import com.realcraft.block.LitWallTorchBlock;
import com.realcraft.block.UnlitWallTorchBlock;
import com.realcraft.config.RealCraftConfig;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 火把现实化的事件层：
 *  - 放置原版火把（主手或副手）→ 替换为"点燃的火把"（有燃料，会烧尽）
 *  - 对熄灭的火把使用打火石/火焰弹 → 重新点燃（消耗 1 点耐久/1 发）
 *
 * 客户端与服务器跑同一套纯判定（不产生副作用），
 * 客户端返回 SUCCESS 提前拦截原版放置/交互预测，避免"闪块"。
 */
public final class TorchEvents {

    private TorchEvents() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(TorchEvents::onUseBlock);
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
                                                BlockHitResult hit) {
        RealCraftConfig cfg = RealCraftConfig.get();
        if (!cfg.enableTorchBurnout) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);

        // ---- 1) 打火石 / 火焰弹：重燃熄灭的火把 ----
        boolean igniter = stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
        if (igniter) {
            BlockPos target = hit.getBlockPos();
            BlockState state = level.getBlockState(target);
            BlockState lit = relightState(state);
            if (lit != null) {
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;   // 预测：拦截原版点燃
                }
                level.setBlock(target, lit, 3);
                level.playSound(null, target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                        SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instab) {
                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;   // 目标不是熄灭的火把：交给原版（可正常点炉子等）
        }

        // ---- 2) 放置原版火把 → 换成"点燃的火把" ----
        if (player.getItemInHand(hand).is(Items.TORCH)) {
            Direction face = hit.getDirection();
            BlockPos placePos = hit.getBlockPos().relative(face);
            BlockState placeState;
            if (face == Direction.UP) {
                placeState = RealCraftMod.LIT_TORCH.defaultBlockState();
            } else if (face.getAxis().isHorizontal()) {
                placeState = RealCraftMod.LIT_WALL_TORCH.defaultBlockState()
                        .setValue(LitWallTorchBlock.FACING, face);
            } else {
                return InteractionResult.PASS;   // 底面等情形交给原版（会失败）
            }
            if (!level.getBlockState(placePos).canBeReplaced()
                    || !placeState.canSurvive(level, placePos)) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;   // 预测：阻止客户端放置原版火把
            }
            level.setBlock(placePos, placeState, 3);
            level.playSound(null, placePos.getX() + 0.5D, placePos.getY() + 0.5D, placePos.getZ() + 0.5D,
                    SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getAbilities().instab) {
                player.getItemInHand(hand).shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static BlockState relightState(BlockState state) {
        if (state.is(RealCraftMod.UNLIT_TORCH)) {
            return RealCraftMod.LIT_TORCH.defaultBlockState();
        }
        if (state.is(RealCraftMod.UNLIT_WALL_TORCH)) {
            return RealCraftMod.LIT_WALL_TORCH.defaultBlockState()
                    .setValue(LitWallTorchBlock.FACING, state.getValue(UnlitWallTorchBlock.FACING));
        }
        return null;
    }

}
