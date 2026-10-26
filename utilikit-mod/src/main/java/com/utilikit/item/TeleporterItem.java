package com.utilikit.item;

import com.utilikit.config.UtiliKitConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 传送仪：潜行 + 右键 = 标记当前位置（含维度/朝向）；右键 = 定向传送回标记点。
 * 支持跨维度传送；3 秒冷却；每次传送消耗 1 点耐久。
 */
public class TeleporterItem extends Item {

    public TeleporterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        ServerLevel serverLevel = (ServerLevel) level;

        // ---- 标记模式 ----
        if (player.isShiftKeyDown()) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("MarkerDim", level.dimension().location().toString());
            tag.putDouble("MarkerX", player.getX());
            tag.putDouble("MarkerY", player.getY());
            tag.putDouble("MarkerZ", player.getZ());
            tag.putFloat("MarkerYaw", player.getYRot());
            serverPlayer.displayClientMessage(Component.translatable("message.utilikit.marker_set"), true);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.4F);
            return InteractionResult.CONSUME;
        }

        // ---- 传送模式 ----
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("MarkerDim")) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.utilikit.marker_missing"), true);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5F, 1.2F);
            return InteractionResult.CONSUME;
        }
        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.CONSUME;
        }
        ResourceLocation dimId = new ResourceLocation(tag.getString("MarkerDim"));
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
        ServerLevel targetLevel = serverPlayer.getServer().getLevel(dimKey);
        if (targetLevel == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.utilikit.marker_invalid"), true);
            return InteractionResult.CONSUME;
        }
        double x = tag.getDouble("MarkerX");
        double y = tag.getDouble("MarkerY");
        double z = tag.getDouble("MarkerZ");
        float yaw = tag.getFloat("MarkerYaw");
        double safeY = findSafeY(targetLevel, x, y, z);   // 标记点可能已被改造，寻找安全落点

        // 出发演出
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.3D, 0.8D, 0.3D, 0.25D);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        int cooldown = Math.max(20, UtiliKitConfig.get().teleportCooldownTicks);
        serverPlayer.getCooldowns().addCooldown(this, cooldown);
        serverPlayer.fallDistance = 0.0F;
        serverPlayer.teleportTo(targetLevel, x, safeY, z, yaw, 0.0F);

        // 到达演出
        targetLevel.sendParticles(ParticleTypes.PORTAL, x, safeY + 1.0D, z, 40, 0.3D, 0.8D, 0.3D, 0.25D);
        targetLevel.playSound(null, x, safeY, z,
                SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
        stack.hurtAndBreak(1, serverPlayer, p -> p.broadcastBreakEvent(hand));
        serverPlayer.displayClientMessage(Component.translatable("message.utilikit.teleported"), true);
        return InteractionResult.CONSUME;
    }

    /** 在标记点附近找安全落点：两格空气 + 脚下实心（向上 8 格、向下 4 格扫描），找不到则原样落点 */
    private static double findSafeY(ServerLevel level, double x, double y, double z) {
        BlockPos base = BlockPos.containing(x, Math.floor(y), z);
        for (int dy = 8; dy >= -4; dy--) {
            BlockPos feet = base.above(dy);
            if (level.getBlockState(feet).isAir()
                    && level.getBlockState(feet.above()).isAir()
                    && level.getBlockState(feet.below()).isSolidRender(level, feet.below())) {
                return feet.getY();
            }
        }
        return y;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("MarkerDim")) {
            String dimId = tag.getString("MarkerDim");
            Component dimName = dimId.startsWith("minecraft:")
                    ? Component.translatable("dimension." + dimId)
                    : Component.literal(dimId);
            tooltip.add(Component.translatable("tooltip.utilikit.marker",
                            dimName,
                            String.format("%.1f", tag.getDouble("MarkerX")),
                            String.format("%.1f", tag.getDouble("MarkerY")),
                            String.format("%.1f", tag.getDouble("MarkerZ")))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.utilikit.no_marker")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
