package com.bladecombo.client.mixin;

import com.bladecombo.client.BladeComboClient;
import com.bladecombo.client.SlashAnimation;
import com.bladecombo.item.SlashBladeItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void bladecombo$animateSlash(AbstractClientPlayer player, float partialTicks, float pitch,
                                         InteractionHand hand, float swingProgress, ItemStack stack,
                                         float equipProgress, PoseStack poseStack, MultiBufferSource buffer,
                                         int combinedLight, CallbackInfo ci) {
        if (!(stack.getItem() instanceof SlashBladeItem) || Minecraft.getInstance().level == null) {
            return;
        }
        SlashAnimation.Instance animation = BladeComboClient.get(player.getUUID());
        if (animation == null) {
            return;
        }
        boolean rightHand = (hand == InteractionHand.MAIN_HAND)
                == (player.getMainArm() == HumanoidArm.RIGHT);
        SlashAnimation.applyFirstPerson(poseStack, animation,
                Minecraft.getInstance().level.getGameTime(), partialTicks, rightHand);
    }
}
