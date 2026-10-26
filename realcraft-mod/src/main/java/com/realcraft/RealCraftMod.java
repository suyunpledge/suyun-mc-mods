package com.realcraft;

import com.realcraft.block.LitTorchBlock;
import com.realcraft.block.LitWallTorchBlock;
import com.realcraft.block.UnlitTorchBlock;
import com.realcraft.block.UnlitWallTorchBlock;
import com.realcraft.config.RealCraftConfig;
import com.realcraft.event.FallEvents;
import com.realcraft.event.PlayerTicker;
import com.realcraft.event.TorchEvents;
import com.realcraft.event.TreeEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RealCraft — 现实工艺
 *
 * 把 Minecraft 的一些游戏机制"现实化"：
 *  1. 火把有燃料：会烧尽熄灭、被雨浇熄，可用打火石重新点燃
 *  2. 现实伐木：砍断树干，整棵树倒下（原木掉落、树叶散落）
 *  3. 真实摔落：高坠骨折（缓慢+虚弱），中摔跛行
 *  4. 负重系统：背太多东西会走不动
 *  5. 深井高温：y 越深越热（挖掘疲劳+间歇灼热，抗火药水/泡水可缓解）
 */
public class RealCraftMod implements ModInitializer {
    public static final String MODID = "realcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("RealCraft");

    // ---- 火把体系 ----
    public static final LitTorchBlock LIT_TORCH = register("lit_torch",
            new LitTorchBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().randomTicks()
                    .lightLevel(state -> 14)
                    .sound(SoundType.WOOD)));

    public static final LitWallTorchBlock LIT_WALL_TORCH = register("lit_wall_torch",
            new LitWallTorchBlock(BlockBehaviour.Properties.of()
                    .noOcclusion().randomTicks()
                    .lightLevel(state -> 14)
                    .sound(SoundType.WOOD)));

    public static final UnlitTorchBlock UNLIT_TORCH = register("unlit_torch",
            new UnlitTorchBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .sound(SoundType.WOOD)));

    public static final UnlitWallTorchBlock UNLIT_WALL_TORCH = register("unlit_wall_torch",
            new UnlitWallTorchBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .sound(SoundType.WOOD)));

    public static final Item UNLIT_TORCH_ITEM = Registry.register(Registries.ITEM,
            new ResourceLocation(MODID, "unlit_torch"),
            new StandingAndWallBlockItem(UNLIT_TORCH, UNLIT_WALL_TORCH,
                    new Item.Properties(), Direction.DOWN));

    private static <T extends Block> T register(String name, T block) {
        return Registry.register(Registries.BLOCK, new ResourceLocation(MODID, name), block);
    }

    @Override
    public void onInitialize() {
        RealCraftConfig.load();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.add(UNLIT_TORCH_ITEM));

        TorchEvents.register();
        TreeEvents.register();
        FallEvents.register();
        PlayerTicker.register();

        LOGGER.info("[RealCraft] 现实工艺已加载：火把会烧尽、树会倒、高处会摔骨折。");
    }
}
