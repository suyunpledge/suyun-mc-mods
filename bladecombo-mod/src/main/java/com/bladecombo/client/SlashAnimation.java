package com.bladecombo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public final class SlashAnimation {
    private SlashAnimation() {
    }

    public static int durationForStage(int stage) {
        return switch (stage) {
            case 2 -> 10;
            case 3 -> 14;
            case 4 -> 18;
            default -> 8;
        };
    }

    public static void applyThirdPerson(ModelPart arm, Instance animation, long now,
                                        float partialTicks, boolean mirror) {
        float progress = animation.progress(now, partialTicks);
        float side = mirror ? 1.0F : -1.0F;
        float arc = Mth.sin(progress * Mth.PI);
        float strike = smooth(0.08F, 0.42F, progress) * (1.0F - smooth(0.62F, 1.0F, progress));
        if (animation.stage == 0) {
            arm.xRot -= 1.25F + 0.18F * animation.impactPulse(now, partialTicks);
            arm.yRot += side * 0.25F;
            arm.zRot -= side * 0.35F;
            return;
        }
        switch (animation.stage) {
            case 2 -> {
                arm.xRot -= 1.85F * arc;
                arm.yRot -= side * 0.35F * arc;
                arm.zRot += side * (0.65F - 1.3F * progress) * arc;
            }
            case 3 -> {
                arm.xRot -= 0.95F * strike;
                arm.yRot += side * 2.4F * arc;
                arm.zRot += side * 0.7F * Mth.sin(progress * Mth.TWO_PI) * arc;
            }
            case 4 -> {
                arm.xRot -= 1.45F * arc;
                arm.yRot += side * 0.7F * Mth.sin(progress * Mth.TWO_PI) * arc;
                arm.zRot += side * 0.95F * Mth.sin(progress * Mth.TWO_PI * 2.0F) * arc;
            }
            default -> {
                arm.xRot -= 0.85F * strike;
                arm.yRot += side * (0.9F - 1.8F * progress) * arc;
                arm.zRot -= side * (0.7F - 1.4F * progress) * arc;
            }
        }
    }

    public static void applyFirstPerson(PoseStack poseStack, Instance animation, long now,
                                        float partialTicks, boolean rightHand) {
        float progress = animation.progress(now, partialTicks);
        float side = rightHand ? 1.0F : -1.0F;
        float arc = Mth.sin(progress * Mth.PI);
        if (animation.stage == 0) {
            poseStack.translate(side * 0.12D, -0.05D, -0.28D);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * -24.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * (-42.0F - 12.0F * animation.impactPulse(now, partialTicks))));
            return;
        }
        switch (animation.stage) {
            case 2 -> {
                poseStack.translate(side * 0.12D * arc, 0.18D * arc, -0.2D * arc);
                poseStack.mulPose(Axis.XP.rotationDegrees(-52.0F * arc));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * (58.0F - 116.0F * progress) * arc));
            }
            case 3 -> {
                poseStack.translate(side * 0.22D * arc, -0.12D * arc, -0.24D * arc);
                poseStack.mulPose(Axis.YP.rotationDegrees(side * 150.0F * arc));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * 46.0F * Mth.sin(progress * Mth.TWO_PI) * arc));
            }
            case 4 -> {
                float doubleCut = Mth.sin(progress * Mth.TWO_PI * 2.0F) * arc;
                poseStack.translate(side * 0.16D * doubleCut, -0.16D * arc, -0.34D * arc);
                poseStack.mulPose(Axis.XP.rotationDegrees(-48.0F * arc));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * 72.0F * doubleCut));
            }
            default -> {
                poseStack.translate(side * 0.2D * arc, -0.08D * arc, -0.24D * arc);
                poseStack.mulPose(Axis.YP.rotationDegrees(side * (55.0F - 110.0F * progress) * arc));
                poseStack.mulPose(Axis.ZP.rotationDegrees(side * (-52.0F + 104.0F * progress) * arc));
            }
        }
    }

    public static float smooth(float start, float end, float value) {
        float t = Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static final class Instance {
        public final int stage;
        public final long startTick;
        public final int duration;
        public final float yaw;
        public long impactTick = Long.MIN_VALUE;

        public Instance(int stage, long startTick, int duration, float yaw) {
            this.stage = stage;
            this.startTick = startTick;
            this.duration = duration;
            this.yaw = yaw;
        }

        public float progress(long now, float partialTicks) {
            return Mth.clamp((now - startTick + partialTicks) / duration, 0.0F, 1.0F);
        }

        public boolean expired(long now) {
            return now > startTick + duration + 2L;
        }

        public float impactPulse(long now, float partialTicks) {
            if (impactTick == Long.MIN_VALUE) {
                return 0.0F;
            }
            float progress = Mth.clamp((now - impactTick + partialTicks) / 6.0F, 0.0F, 1.0F);
            return Mth.sin(progress * Mth.PI);
        }
    }
}
