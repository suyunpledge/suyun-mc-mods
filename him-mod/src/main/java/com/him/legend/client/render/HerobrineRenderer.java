package com.him.legend.client.render;

import com.him.legend.HimMod;
import com.him.legend.entity.HerobrineEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Herobrine 渲染器：经典 Steve 玩家模型 + 无瞳孔的白眼贴图，
 * 附加一层全亮度发光眼睛（黑暗中那双眼睛仍在发光）。
 */
public class HerobrineRenderer extends MobRenderer<HerobrineEntity, HumanoidModel<HerobrineEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(HimMod.MODID, "textures/entity/herobrine.png");

    public HerobrineRenderer(EntityRendererProvider.Context context) {
        // 复用玩家模型层（基础四肢/身体/头部布局与 64x64 皮肤底图一致）
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new GlowingEyesLayer<>(this,
                new ResourceLocation(HimMod.MODID, "textures/entity/herobrine_eyes.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(HerobrineEntity entity) {
        return TEXTURE;
    }
}
