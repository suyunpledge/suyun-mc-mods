package com.him.legend;

import com.him.legend.config.HimConfig;
import com.him.legend.entity.Entity303Entity;
import com.him.legend.entity.Entity404Entity;
import com.him.legend.entity.HerobrineEntity;
import com.him.legend.entity.NullEntity;
import com.him.legend.entity.WhiteEyesEntity;
import com.him.legend.world.BattleManager;
import com.him.legend.world.HerobrineManager;
import com.him.legend.world.TotemHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HIM — The Haunting of Herobrine
 *
 * 高强度还原 2010 年 Herobrine 都市传说的模组：
 *  - 白眼、雾中凝视、被注视/接近即消失
 *  - 2x2 隧道、红石火把、被扒光树叶的树、水中沙金字塔、燃烧的下界岩
 *  - 金块图腾召唤（2 金块 + 下界岩 + 4 个红石火把，点燃）
 */
public class HimMod implements ModInitializer {
    public static final String MODID = "him";
    public static final Logger LOGGER = LoggerFactory.getLogger("HIM");

    public static final EntityType<HerobrineEntity> HEROBRINE = Registry.register(
            Registries.ENTITY_TYPE,
            new ResourceLocation(MODID, "herobrine"),
            EntityType.Builder.of(HerobrineEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()          // 传说：他走在岩浆上（Patimuss 直播）
                    .build(MODID + ":herobrine")
    );

    /** 实体 303：Herobrine 的宿敌（2013 年"被开除的 Mojang 员工"传说） */
    public static final EntityType<Entity303Entity> ENTITY_303 = Registry.register(
            Registries.ENTITY_TYPE,
            new ResourceLocation(MODID, "entity_303"),
            EntityType.Builder.of(Entity303Entity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .build(MODID + ":entity_303")
    );

    /** 实体 Null：全黑人形，"损坏世界"者（可与 Herobrine / 303 开战） */
    public static final EntityType<NullEntity> NULL = Registry.register(
            Registries.ENTITY_TYPE,
            new ResourceLocation(MODID, "null"),
            EntityType.Builder.of(NullEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .build(MODID + ":null")
    );

    /** 实体 404：绿色故障体（错误文本 / 闪烁瞬移 / 假断线惊吓） */
    public static final EntityType<Entity404Entity> ENTITY_404 = Registry.register(
            Registries.ENTITY_TYPE,
            new ResourceLocation(MODID, "entity_404"),
            EntityType.Builder.of(Entity404Entity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .build(MODID + ":entity_404")
    );

    /** 白眼（White Eyes）：2010-07 Herobrine 传说的前身，只在黑暗中现身 */
    public static final EntityType<WhiteEyesEntity> WHITE_EYES = Registry.register(
            Registries.ENTITY_TYPE,
            new ResourceLocation(MODID, "white_eyes"),
            EntityType.Builder.of(WhiteEyesEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .build(MODID + ":white_eyes")
    );

    @Override
    public void onInitialize() {
        HimConfig.load();
        FabricDefaultAttributeRegistry.register(HEROBRINE, HerobrineEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ENTITY_303, Entity303Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(NULL, HerobrineEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ENTITY_404, Entity404Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(WHITE_EYES, HerobrineEntity.createAttributes());
        HerobrineManager.register();
        BattleManager.register();
        TotemHandler.register();

        // Mojang 官方玩笑：每个版本更新日志都写着 "Removed Herobrine."
        LOGGER.info("Removed Herobrine.");
    }
}
