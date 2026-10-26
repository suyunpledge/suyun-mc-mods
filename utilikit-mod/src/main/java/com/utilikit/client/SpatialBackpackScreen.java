package com.utilikit.client;

import com.utilikit.menu.SpatialBackpackMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 空间背包界面：复用原版大箱子贴图（6 行布局），
 * 悬停虚拟格时在 Tooltip 中显示该格的真实储量。
 */
public class SpatialBackpackScreen extends AbstractContainerScreen<SpatialBackpackMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    public SpatialBackpackScreen(SpatialBackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;                 // 6 行容器
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()
                && this.hoveredSlot.index < 54) {
            ItemStack display = this.hoveredSlot.getItem();
            long stored = this.getMenu().storedCountOf(this.hoveredSlot.index);
            List<Component> lines = new ArrayList<>();
            lines.add(display.getHoverName());
            lines.add(Component.translatable("tooltip.utilikit.stored", stored)
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.utilikit.withdraw_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            guiGraphics.renderComponentTooltip(this.font, lines, x, y);
            return;
        }
        super.renderTooltip(guiGraphics, x, y);
    }
}
