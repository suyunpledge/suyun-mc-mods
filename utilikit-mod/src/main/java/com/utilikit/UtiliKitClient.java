package com.utilikit;

import com.utilikit.client.ProspectorClient;
import com.utilikit.client.SpatialBackpackScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.screenhandler.v1.MenuRegistry;

public class UtiliKitClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 探测仪：周期扫描 + 世界渲染（红色虚线）
        ClientTickEvents.END_CLIENT_TICK.register(ProspectorClient::tick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ProspectorClient::render);

        // 空间背包 GUI
        MenuRegistry.registerScreenFactory(UtiliKitMod.SPATIAL_MENU, SpatialBackpackScreen::new);
    }
}
