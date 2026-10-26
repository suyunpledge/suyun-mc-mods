package com.utilikit.item;

import com.utilikit.UtiliKitMod;
import com.utilikit.menu.SpatialBackpackMenu;
import com.utilikit.menu.SpatialContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 空间背包：右键打开虚拟 54 格容器。
 * 同类物品自动合并为一格，单格储量近乎无限（long 计数），取出 64/次。
 */
public class SpatialBackpackItem extends Item {

    public SpatialBackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        ItemStack stack = player.getItemInHand(hand);
        SpatialContainer container = SpatialContainer.load(stack);
        serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new SpatialBackpackMenu(id, inv, container, stack),
                Component.translatable("container.utilikit.spatial_backpack")));
        return InteractionResult.CONSUME;
    }
}
