package com.forgesmith.forge;

import com.forgesmith.config.ForgeSmithConfig;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 对锻造台右键：双手 = 装备 + 材料（任一顺序）→ 锻造强化。
 * 双手组合不匹配时放行，原版锻造台界面正常打开。
 */
public final class ForgeTableEvents {

    private ForgeTableEvents() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(ForgeTableEvents::onUseBlock);
    }

    private static InteractionResult onUseBlock(net.minecraft.world.entity.player.Player player,
                                                Level level, net.minecraft.world.InteractionHand hand,
                                                BlockHitResult hit) {
        if (!ForgeSmithConfig.get().enabled
                || !level.getBlockState(hit.getBlockPos()).is(Blocks.SMITHING_TABLE)) {
            return InteractionResult.PASS;
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ForgeMaterial matInOff = ForgeMaterial.fromItem(off.getItem());
        ForgeMaterial matInMain = ForgeMaterial.fromItem(main.getItem());

        ItemStack gear = null;
        ForgeMaterial material = null;
        ItemStack materialStack = null;
        if (ForgeData.isGear(main) && matInOff != null) {
            gear = main;
            material = matInOff;
            materialStack = off;
        } else if (ForgeData.isGear(off) && matInMain != null) {
            gear = off;
            material = matInMain;
            materialStack = main;
        }

        if (gear == null) {
            return InteractionResult.PASS;   // 正常打开锻造台 GUI
        }
        if (materialStack.getCount() < material.cost) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable(
                                "message.forgesmith.need_more", material.cost,
                                Component.translatable(material.displayKey()))
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ForgeApplier.apply(player, gear, material, materialStack);
        return InteractionResult.SUCCESS;
    }
}
