package com.bladecombo.item;

import com.bladecombo.combo.ComboState;
import com.bladecombo.combo.SlashComboManager;
import com.bladecombo.config.BladeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 拔刀剑。
 *
 * 连招（右键为格挡架势）：
 *  - 攻击键连点：一之太刀·横斩 → 二之太刀·逆袈裟 → 三之太刀·回旋斩 → 奥义·幻影连斩 → 循环（无限连）
 *  - 命中后 0.6 秒内右键（或潜行右键）：格挡架势——减伤 90%、弹刀反伤
 *  - 命中后 4 tick 内右键：完美衔接，格挡无冷却
 *  - 滞空中斩击：4 秒内免疫摔落伤害（空中连段）
 */
public class SlashBladeItem extends SwordItem {

    public SlashBladeItem(Tier tier, Properties properties) {
        super(tier, 0, -2.4F, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }
        BladeConfig cfg = BladeConfig.get();
        ComboState cs = SlashComboManager.state(serverPlayer);
        long now = level.getGameTime();

        long sinceHit = cs.lastHitAt == Long.MIN_VALUE ? Long.MAX_VALUE : now - cs.lastHitAt;
        boolean comboLink = sinceHit <= 12;
        if (!player.isShiftKeyDown() && !comboLink) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.bladecombo.need_link"), true);
            return InteractionResultHolder.consume(stack);
        }
        if (now < cs.parryReadyAt) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.bladecombo.parry_cd"), true);
            return InteractionResultHolder.consume(stack);
        }

        cs.parryUntil = now + cfg.parryDurationTicks;
        // 完美衔接（命中后 4 tick 内格挡）：不进入冷却
        cs.parryReadyAt = sinceHit <= 4 ? now : now + cfg.parryCooldownTicks;
        com.bladecombo.network.BladeComboNetworking.sendParry(serverPlayer, cfg.parryDurationTicks);

        serverPlayer.displayClientMessage(
                Component.translatable("message.bladecombo.parry").withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 1.2F);
        if (level instanceof ServerLevel serverLevel) {
            double r = 1.0D;
            for (int a = 0; a < 8; a++) {
                double ang = a * Math.PI / 4.0D;
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        player.getX() + Math.cos(ang) * r, player.getY() + 1.1D, player.getZ() + Math.sin(ang) * r,
                        1, 0.03D, 0.06D, 0.03D, 0.0D);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    public static int getKills(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt("Kills");
    }

    public static void addKill(ItemStack stack) {
        stack.getOrCreateTag().putInt("Kills", getKills(stack) + 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int kills = getKills(stack);
        if (kills > 0) {
            tooltip.add(Component.translatable("tooltip.bladecombo.kills", kills)
                    .withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.translatable("tooltip.bladecombo.hint1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bladecombo.hint2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bladecombo.hint3").withStyle(ChatFormatting.GRAY));
    }
}
