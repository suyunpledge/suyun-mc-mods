package com.bladecombo;

import com.bladecombo.combo.SlashComboManager;
import com.bladecombo.config.BladeConfig;
import com.bladecombo.item.SlashBladeItem;
import com.bladecombo.item.SlashTier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BladeCombo — 拔刀连斩
 *
 * 固定连招的拔刀剑：
 *  - 连点攻击 = 四段固定连招无限循环（每次命中重置蓄力，手残党友好）
 *      一之太刀·横斩(×1) → 二之太刀·逆袈裟(×1.2, 挑空) → 三之太刀·回旋斩(×1.5, 360°) → 奥义·幻影连斩(×2, 大击退) → 循环
 *  - 特定连招格挡：命中后 0.6 秒内右键（或潜行右键）= 格挡架势（减伤 90% + 弹刀反伤）；
 *    命中后 4 tick 内衔接 = 完美格挡，无冷却
 *  - 空中连招：滞空斩击后 4 秒内免疫摔落伤害（空中连段·软着陆）
 */
public class BladeComboMod implements ModInitializer {
    public static final String MODID = "bladecombo";
    public static final Logger LOGGER = LoggerFactory.getLogger("BladeCombo");

    public static final Item BAMBOO_SLASH_BLADE = register("bamboo_slash_blade",
            new SlashBladeItem(SlashTier.BAMBOO, new Item.Properties()));
    public static final Item IRON_SLASH_BLADE = register("iron_slash_blade",
            new SlashBladeItem(SlashTier.IRON, new Item.Properties()));
    public static final Item DIAMOND_SLASH_BLADE = register("diamond_slash_blade",
            new SlashBladeItem(SlashTier.DIAMOND, new Item.Properties()));
    public static final Item NETHERITE_SLASH_BLADE = register("netherite_slash_blade",
            new SlashBladeItem(SlashTier.NETHERITE, new Item.Properties()));

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MODID, name), item);
    }

    @Override
    public void onInitialize() {
        BladeConfig.load();
        SlashComboManager.register();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT)
                .register(entries -> {
                    entries.accept(new ItemStack(BAMBOO_SLASH_BLADE));
                    entries.accept(new ItemStack(IRON_SLASH_BLADE));
                    entries.accept(new ItemStack(DIAMOND_SLASH_BLADE));
                    entries.accept(new ItemStack(NETHERITE_SLASH_BLADE));
                });
        LOGGER.info("[BladeCombo] 拔刀连斩已出鞘。");
    }
}
