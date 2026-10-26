package com.utilikit;

import com.utilikit.config.UtiliKitConfig;
import com.utilikit.item.SpatialBackpackItem;
import com.utilikit.item.TeleporterItem;
import com.utilikit.menu.SpatialBackpackMenu;
import com.utilikit.menu.SpatialContainer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.MenuRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UtiliKit — 实用工具系统
 *
 *  1. 探测仪 Prospector：手持（主手或副手）扫描周围矿脉，红色虚线指引
 *  2. 传送仪 Teleporter：潜行右键标记地点，右键定向传送（跨维度）
 *  3. 空间背包 Spatial Backpack：自动分类合并的近无限储存容器
 */
public class UtiliKitMod implements ModInitializer {
    public static final String MODID = "utilikit";
    public static final Logger LOGGER = LoggerFactory.getLogger("UtiliKit");

    public static final Item PROSPECTOR = registerItem("prospector",
            new Item(new Item.Properties().stacksTo(1)));

    public static final TeleporterItem TELEPORTER = registerItem("teleporter",
            new TeleporterItem(new Item.Properties().stacksTo(1).durability(128)));

    public static final SpatialBackpackItem SPATIAL_BACKPACK = registerItem("spatial_backpack",
            new SpatialBackpackItem(new Item.Properties().stacksTo(1)));

    public static final MenuType<SpatialBackpackMenu> SPATIAL_MENU = MenuRegistry.of(
            (id, inv) -> new SpatialBackpackMenu(id, inv, new SpatialContainer(), ItemStack.EMPTY));

    private static <T extends Item> T registerItem(String name, T item) {
        return Registry.register(Registries.ITEM, new ResourceLocation(MODID, name), item);
    }

    @Override
    public void onInitialize() {
        UtiliKitConfig.load();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> {
                    entries.accept(PROSPECTOR);
                    entries.accept(TELEPORTER);
                    entries.accept(SPATIAL_BACKPACK);
                });
        LOGGER.info("[UtiliKit] 实用工具系统已加载：探测仪、传送仪、空间背包。");
    }
}
