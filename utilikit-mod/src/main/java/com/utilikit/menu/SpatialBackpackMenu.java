package com.utilikit.menu;

import com.utilikit.UtiliKitMod;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 空间背包菜单：54 虚拟格 + 玩家背包，双排横向 6 行（同原版大箱子布局）。
 *
 * 快速转移语义：
 *  - 玩家背包 → 虚拟格（shift）：整组存入计数（自动合并）
 *  - 虚拟格 → 玩家背包（shift）：一次取出 64
 *  - 手动点击：左键拿 64 / 放整组；右键拿 32
 *
 * 真实储量通过 ContainerData（54 int）同步给客户端，供 Tooltip 显示。
 */
public class SpatialBackpackMenu extends AbstractContainerMenu {
    private final SpatialContainer backpack;
    private final ItemStack stack;
    private final SimpleContainerData stored;

    public SpatialBackpackMenu(int id, Inventory playerInventory,
                               SpatialContainer container, ItemStack stack) {
        super(UtiliKitMod.SPATIAL_MENU, id);
        this.backpack = container;
        this.stack = stack;
        this.stored = new SimpleContainerData(SpatialContainer.SLOTS);
        this.refreshStored();

        container.setSaveHandler(() -> {
            SpatialContainer.save(container, SpatialBackpackMenu.this.stack);
            SpatialBackpackMenu.this.refreshStored();
        });

        // 虚拟格：6 行 x 9
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(container, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }
        // 玩家背包（与原版大箱子一致的坐标）
        int k = (6 - 4) * 18;   // 36
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + k + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + k));
        }

        this.addDataSlots(this.stored);
    }

    private void refreshStored() {
        for (int i = 0; i < SpatialContainer.SLOTS; i++) {
            this.stored.set(i, (int) Math.min(Integer.MAX_VALUE, this.backpack.storedCount(i)));
        }
    }

    /** 客户端 Tooltip 用：某虚拟格的真实储量 */
    public long storedCountOf(int viewIndex) {
        if (viewIndex >= 0 && viewIndex < SpatialContainer.SLOTS) {
            return this.stored.get(viewIndex);
        }
        return 0L;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        if (index < SpatialContainer.SLOTS) {
            // 虚拟格 → 玩家：真实取出 64，再走常规合并
            ItemStack taken = this.backpack.removeItem(index, 64);
            if (taken.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack original = taken.copy();
            if (!this.moveItemStackTo(taken, SpatialContainer.SLOTS, this.slots.size(), true)) {
                this.backpack.deposit(taken);   // 放不下：退回
                return ItemStack.EMPTY;
            }
            if (!taken.isEmpty()) {
                this.backpack.deposit(taken);   // 剩余存回
            }
            return original;
        }

        // 玩家 → 虚拟格：整组存入
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = stack.copy();
        this.backpack.deposit(stack);
        slot.set(ItemStack.EMPTY);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        SpatialContainer.save(this.backpack, this.stack);
        super.removed(player);
    }
}
