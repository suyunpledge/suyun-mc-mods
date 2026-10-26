package com.bladecombo.client;

import com.bladecombo.network.BladeComboNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BladeComboClient implements ClientModInitializer {
    private static final Map<UUID, SlashAnimation.Instance> ANIMATIONS = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel currentLevel;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BladeComboNetworking.SLASH,
                (client, handler, buffer, responseSender) -> {
                    UUID playerId = buffer.readUUID();
                    int stage = buffer.readByte();
                    float yaw = buffer.readFloat();
                    client.execute(() -> startSlash(client, playerId, stage, yaw));
                });
        ClientPlayNetworking.registerGlobalReceiver(BladeComboNetworking.PARRY,
                (client, handler, buffer, responseSender) -> {
                    UUID playerId = buffer.readUUID();
                    int duration = buffer.readVarInt();
                    float yaw = buffer.readFloat();
                    client.execute(() -> startParry(client, playerId, duration, yaw));
                });
        ClientPlayNetworking.registerGlobalReceiver(BladeComboNetworking.PARRY_IMPACT,
                (client, handler, buffer, responseSender) -> {
                    UUID playerId = buffer.readUUID();
                    client.execute(() -> startImpact(client, playerId));
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != currentLevel) {
                ANIMATIONS.clear();
                currentLevel = client.level;
            }
            if (client.level != null) {
                long now = client.level.getGameTime();
                ANIMATIONS.values().removeIf(animation -> animation.expired(now));
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ANIMATIONS.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ANIMATIONS.clear();
            currentLevel = null;
        });
        WorldRenderEvents.AFTER_ENTITIES.register(SlashTrailRenderer::render);
    }

    public static SlashAnimation.Instance get(UUID playerId) {
        return ANIMATIONS.get(playerId);
    }

    private static void startSlash(Minecraft client, UUID playerId, int stage, float yaw) {
        if (client.level != null && stage >= 1 && stage <= 4) {
            ANIMATIONS.put(playerId, new SlashAnimation.Instance(stage, client.level.getGameTime(),
                    SlashAnimation.durationForStage(stage), yaw));
        }
    }

    private static void startParry(Minecraft client, UUID playerId, int duration, float yaw) {
        if (client.level != null) {
            ANIMATIONS.put(playerId, new SlashAnimation.Instance(0, client.level.getGameTime(),
                    Math.max(1, Math.min(duration, 100)), yaw));
        }
    }

    private static void startImpact(Minecraft client, UUID playerId) {
        if (client.level == null) {
            return;
        }
        long now = client.level.getGameTime();
        SlashAnimation.Instance animation = ANIMATIONS.get(playerId);
        if (animation == null) {
            animation = new SlashAnimation.Instance(0, now, 6, 0.0F);
            ANIMATIONS.put(playerId, animation);
        }
        animation.impactTick = now;
    }
}
