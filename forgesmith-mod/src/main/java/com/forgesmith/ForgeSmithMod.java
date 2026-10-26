package com.forgesmith;

import com.forgesmith.config.ForgeSmithConfig;
import com.forgesmith.event.CombatEvents;
import com.forgesmith.event.EquipmentTicker;
import com.forgesmith.forge.ForgeTableEvents;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ForgeSmith — 锻造强化
 *
 * 对锻造台右键，用"装备 + 材料"双手组合强化原版盔甲与工具（最多 3 层）：
 *  - 锭（铁/金/钻/下界合金，高级覆盖低级）：盔甲→护甲+击退抗性；剑→伤害；镐/铲→急促；斧→耐久
 *  - 红石：盔甲→生命上限；工具/武器→耐久附魔（大幅降低耐久消耗）
 *  - 青金石：盔甲→水抗（溺水/冻伤免疫、水中减伤）；工具/武器→潮涌能量（水下输出）
 *  - 绿宝石：盔甲→再生；工具/武器→命中吸血
 */
public class ForgeSmithMod implements ModInitializer {
    public static final String MODID = "forgesmith";
    public static final Logger LOGGER = LoggerFactory.getLogger("ForgeSmith");

    @Override
    public void onInitialize() {
        ForgeSmithConfig.load();
        ForgeTableEvents.register();
        EquipmentTicker.register();
        CombatEvents.register();
        LOGGER.info("[ForgeSmith] 锻造强化已加载：手持装备与材料，对锻造台右键进行强化。");
    }
}
