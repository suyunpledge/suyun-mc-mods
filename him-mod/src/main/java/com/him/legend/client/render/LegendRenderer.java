package com.him.legend.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;

/**
 * 通用传说实体渲染器：玩家模型 + 各自皮肤 + 发光眼睛层。
 * 供 Null / Entity 404 / White Eyes 共用。
 */
public class LegendRenderer<T extends PathfinderMob> extends MobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;

    public LegendRenderer(EntityRendererProvider.Context context,
                          ResourceLocation texture, ResourceLocation eyesTexture) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.texture = texture;
        this.addLayer(new GlowingEyesLayer<>(this, eyesTexture));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
