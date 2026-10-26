package com.him.legend.client.render;

import com.him.legend.HimMod;
import com.him.legend.entity.Entity303Entity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 实体 303 渲染器：黑兜帽苍白脸 + 红色发光眼。
 */
public class Entity303Renderer extends MobRenderer<Entity303Entity, HumanoidModel<Entity303Entity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(HimMod.MODID, "textures/entity/entity_303.png");

    public Entity303Renderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new GlowingEyesLayer<>(this,
                new ResourceLocation(HimMod.MODID, "textures/entity/entity_303_eyes.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(Entity303Entity entity) {
        return TEXTURE;
    }
}
