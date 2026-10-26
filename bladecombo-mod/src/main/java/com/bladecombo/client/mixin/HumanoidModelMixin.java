package com.bladecombo.client.mixin;

import com.bladecombo.client.BladeComboClient;
import com.bladecombo.client.SlashAnimation;
import com.bladecombo.item.SlashBladeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void bladecombo$animateSlash(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                         float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player) || Minecraft.getInstance().level == null) {
            return;
        }
        InteractionHand hand = player.getMainHandItem().getItem() instanceof SlashBladeItem
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!(player.getItemInHand(hand).getItem() instanceof SlashBladeItem)) {
            return;
        }
        SlashAnimation.Instance animation = BladeComboClient.get(player.getUUID());
        if (animation == null) {
            return;
        }
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        boolean rightArm = (hand == InteractionHand.MAIN_HAND)
                == (player.getMainArm() == HumanoidArm.RIGHT);
        ModelPart arm = rightArm ? model.rightArm : model.leftArm;
        SlashAnimation.applyThirdPerson(arm, animation, Minecraft.getInstance().level.getGameTime(),
                Minecraft.getInstance().getFrameTime(), rightArm);
    }
}
