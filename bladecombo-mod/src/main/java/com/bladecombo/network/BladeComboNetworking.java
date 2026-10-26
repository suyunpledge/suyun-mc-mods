package com.bladecombo.network;

import com.bladecombo.BladeComboMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public final class BladeComboNetworking {
    public static final ResourceLocation SLASH = new ResourceLocation(BladeComboMod.MODID, "slash");
    public static final ResourceLocation PARRY = new ResourceLocation(BladeComboMod.MODID, "parry");
    public static final ResourceLocation PARRY_IMPACT = new ResourceLocation(BladeComboMod.MODID, "parry_impact");

    private BladeComboNetworking() {
    }

    public static void sendSlash(ServerPlayer player, int stage) {
        broadcast(player, SLASH, buffer -> {
            buffer.writeUUID(player.getUUID());
            buffer.writeByte(stage);
            buffer.writeFloat(player.getYRot());
        });
    }

    public static void sendParry(ServerPlayer player, int durationTicks) {
        broadcast(player, PARRY, buffer -> {
            buffer.writeUUID(player.getUUID());
            buffer.writeVarInt(durationTicks);
            buffer.writeFloat(player.getYRot());
        });
    }

    public static void sendParryImpact(ServerPlayer player) {
        broadcast(player, PARRY_IMPACT, buffer -> buffer.writeUUID(player.getUUID()));
    }

    private static void broadcast(ServerPlayer source, ResourceLocation channel,
                                  Consumer<FriendlyByteBuf> writer) {
        for (ServerPlayer viewer : source.serverLevel().players()) {
            if (viewer != source && viewer.distanceToSqr(source) > 4096.0D) {
                continue;
            }
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            writer.accept(buffer);
            ServerPlayNetworking.send(viewer, channel, buffer);
        }
    }
}
