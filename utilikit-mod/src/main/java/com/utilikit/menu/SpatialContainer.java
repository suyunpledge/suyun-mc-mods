package com.utilikit.menu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 空间背包的虚拟容器：
 *  - 真实存储是 Map&lt;Item, Long&gt;（同类自动合并 = 自动分类；long 计数 ≈ 无限储存）
 *  - 对外呈现 54 个"虚拟格"：显示前 54 类物品，每格显示至多 64 个
 *  - 存入：setItem/deposit 直接并入计数；取出：removeItem 每次至多取 64
 *  - 每次变更回调 saveHandler（菜单负责写回物品 NBT + 刷新同步数据）
 */
public class SpatialContainer implements Container {
    public static final int SLOTS = 54;

    private final LinkedHashMap<Item, Long> entries = new LinkedHashMap<>();
    /** 与 entries 同步维护的顺序表：O(1) 按显示下标取条目（避免每次查询复制整个 keySet） */
    private final List<Item> order = new ArrayList<>();
    private final ItemStack[] view = new ItemStack[SLOTS];
    private Runnable saveHandler = () -> {
    };

    public SpatialContainer() {
        this.rebuild();
    }

    public void setSaveHandler(Runnable handler) {
        this.saveHandler = handler;
        this.saveHandler.run(); // 挂接后先同步一次
    }

    /** 存入任意物品（整组合并），返回存入数量 */
    public long deposit(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        this.entries.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
        this.rebuild();
        this.changed();
        return stack.getCount();
    }

    /** 某显示格的真实储量（用于 Tooltip / 数据同步） */
    public long storedCount(int viewIndex) {
        if (viewIndex < 0 || viewIndex >= this.order.size()) {
            return 0L;
        }
        return this.entries.get(this.order.get(viewIndex));
    }

    public Item entryAt(int viewIndex) {
        if (viewIndex < 0 || viewIndex >= this.order.size()) {
            return null;
        }
        return this.order.get(viewIndex);
    }

    // ---- NBT 读写 ----

    public static SpatialContainer load(ItemStack stack) {
        SpatialContainer container = new SpatialContainer();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Content", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Content", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(entry.getString("id")));
                if (item != null && item != Items.AIR) {
                    container.entries.put(item, Math.max(0L, entry.getLong("c")));
                }
            }
            container.rebuild();
        }
        return container;
    }

    public static void save(SpatialContainer container, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return; // 客户端镜像：不落盘
        }
        ListTag list = new ListTag();
        for (Map.Entry<Item, Long> e : container.entries.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", BuiltInRegistries.ITEM.getKey(e.getKey()).toString());
            entry.putLong("c", e.getValue());
            list.add(entry);
        }
        stack.getOrCreateTag().put("Content", list);
    }

    // ---- Container 实现 ----

    private void rebuild() {
        this.order.clear();
        this.order.addAll(this.entries.keySet());
        int i = 0;
        for (Map.Entry<Item, Long> e : this.entries.entrySet()) {
            if (i >= SLOTS) {
                break;
            }
            this.view[i] = new ItemStack(e.getKey(), (int) Math.min(64L, e.getValue()));
            i++;
        }
        for (; i < SLOTS; i++) {
            this.view[i] = ItemStack.EMPTY;
        }
    }

    private void changed() {
        this.saveHandler.run();
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        ItemStack display = this.view[index];
        return display.isEmpty() ? ItemStack.EMPTY : display.copy();
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        Item item = this.entryAt(index);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        long real = this.entries.get(item);
        int take = (int) Math.min(Math.min(amount, real), 64L);
        if (take <= 0) {
            return ItemStack.EMPTY;
        }
        long left = real - take;
        if (left <= 0L) {
            this.entries.remove(item);
        } else {
            this.entries.put(item, left);
        }
        this.rebuild();
        this.changed();
        return new ItemStack(item, take);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return this.removeItem(index, 64);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.deposit(stack);
    }

    @Override
    public void setChanged() {
        this.changed();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.entries.clear();
        this.rebuild();
        this.changed();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return true;
    }
}
