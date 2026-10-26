package com.forgesmith;

import com.forgesmith.forge.ForgeData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ForgeSmithClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipFlag, lines) -> {
            if (!ForgeData.isGear(stack)) {
                return;
            }
            List<String> layers = ForgeData.layers(stack);
            if (layers.isEmpty()) {
                return;
            }
            lines.add(Component.translatable("tooltip.forgesmith.layers", layers.size())
                    .withStyle(ChatFormatting.DARK_AQUA));
            boolean armor = stack.getItem() instanceof ArmorItem;
            for (String layer : layers) {
                lines.add(Component.literal("  ▪ ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable("material.forgesmith." + layer)
                                .withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" — ")
                                .withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.translatable("effect.forgesmith." + layer
                                        + (armor ? ".armor" : ".tool"))
                                .withStyle(ChatFormatting.GRAY)));
            }
        });
    }
}
