package com.him.legend.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * 发光眼睛层：RenderType.eyes() 不受环境光照影响 ——
 * Herobrine 的白眼 / Entity 303 的红眼，在黑暗与浓雾中依然发光。
 */
public class GlowingEyesLayer<T extends Mob, M extends EntityModel<T>> extends EyesLayer<T, M> {
    private final RenderType renderType;

    public GlowingEyesLayer(RenderLayerParent<T, M> parent, ResourceLocation eyesTexture) {
        super(parent);
        this.renderType = RenderType.eyes(eyesTexture);
    }

    @Override
    protected RenderType renderType() {
        return this.renderType;
    }
}
