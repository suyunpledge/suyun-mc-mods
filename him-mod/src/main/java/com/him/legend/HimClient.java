package com.him.legend;

import com.him.legend.client.render.Entity303Renderer;
import com.him.legend.client.render.HerobrineRenderer;
import com.him.legend.client.render.LegendRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.resources.ResourceLocation;

public class HimClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(HimMod.HEROBRINE, HerobrineRenderer::new);
        EntityRendererRegistry.register(HimMod.ENTITY_303, Entity303Renderer::new);
        EntityRendererRegistry.register(HimMod.NULL, context -> new LegendRenderer<>(
                context,
                new ResourceLocation(HimMod.MODID, "textures/entity/null.png"),
                new ResourceLocation(HimMod.MODID, "textures/entity/null_eyes.png")));
        EntityRendererRegistry.register(HimMod.ENTITY_404, context -> new LegendRenderer<>(
                context,
                new ResourceLocation(HimMod.MODID, "textures/entity/entity_404.png"),
                new ResourceLocation(HimMod.MODID, "textures/entity/entity_404_eyes.png")));
        EntityRendererRegistry.register(HimMod.WHITE_EYES, context -> new LegendRenderer<>(
                context,
                new ResourceLocation(HimMod.MODID, "textures/entity/white_eyes.png"),
                new ResourceLocation(HimMod.MODID, "textures/entity/white_eyes_eyes.png")));
    }
}
