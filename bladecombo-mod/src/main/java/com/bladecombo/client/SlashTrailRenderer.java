package com.bladecombo.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class SlashTrailRenderer {
    private SlashTrailRenderer() {
    }

    public static void render(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack poseStack = context.matrixStack();
        if (client.level == null || poseStack == null) {
            return;
        }
        float partialTicks = context.tickDelta();
        long now = client.level.getGameTime();
        Vec3 camera = context.camera().getPosition();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            for (Player player : client.level.players()) {
                SlashAnimation.Instance animation = BladeComboClient.get(player.getUUID());
                if (animation == null || animation.expired(now)) {
                    continue;
                }
                if (player == client.player && client.options.getCameraType().isFirstPerson()) {
                    continue;
                }
                float progress = animation.progress(now, partialTicks);
                if (progress >= 1.0F) {
                    continue;
                }
                poseStack.pushPose();
                double x = lerp(partialTicks, player.xOld, player.getX()) - camera.x;
                double y = lerp(partialTicks, player.yOld, player.getY()) - camera.y + 0.95D;
                double z = lerp(partialTicks, player.zOld, player.getZ()) - camera.z;
                poseStack.translate(x, y, z);
                poseStack.mulPose(Axis.YP.rotationDegrees(-animation.yaw));
                if (animation.stage == 0) {
                    renderParry(poseStack, animation, now, partialTicks);
                } else {
                    renderSlash(poseStack, animation.stage, progress);
                }
                poseStack.popPose();
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void renderSlash(PoseStack poseStack, int stage, float progress) {
        float reveal = SlashAnimation.smooth(0.0F, 0.68F, progress);
        float alpha = 0.78F * (1.0F - SlashAnimation.smooth(0.45F, 1.0F, progress));
        if (alpha <= 0.01F) {
            return;
        }
        float red = stage == 4 ? 1.0F : 0.38F;
        float green = stage == 4 ? 0.74F : stage == 2 ? 1.0F : 0.82F;
        float blue = stage == 4 ? 0.3F : 1.0F;
        if (stage == 2) {
            diagonal(poseStack, reveal, 1.0F, alpha, red, green, blue);
        } else if (stage == 4) {
            diagonal(poseStack, reveal, 1.0F, alpha, red, green, blue);
            diagonal(poseStack, reveal, -1.0F, alpha * 0.85F, 0.5F, 0.85F, 1.0F);
        } else {
            arc(poseStack, reveal, stage == 3, alpha, red, green, blue);
        }
    }

    private static void arc(PoseStack poseStack, float reveal, boolean fullCircle,
                            float alpha, float red, float green, float blue) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        int segments = fullCircle ? 40 : 24;
        float start = fullCircle ? -180.0F : -115.0F;
        float sweep = (fullCircle ? 360.0F : 230.0F) * reveal;
        float radius = fullCircle ? 1.75F : 1.55F;
        for (int i = 0; i <= segments; i++) {
            float fraction = (float) i / segments;
            float angle = (float) Math.toRadians(start + sweep * fraction);
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            float y = fullCircle ? 0.18F * (float) Math.sin(angle * 2.0F) : 0.08F * sin;
            float edgeAlpha = alpha * (0.3F + 0.7F * (float) Math.sin(fraction * Math.PI));
            vertex(builder, matrix, sin * (radius - 0.13F), y - 0.035F, cos * (radius - 0.13F),
                    red, green, blue, 0.0F);
            vertex(builder, matrix, sin * radius, y, cos * radius, red, green, blue, edgeAlpha);
            vertex(builder, matrix, sin * (radius + 0.13F), y + 0.035F, cos * (radius + 0.13F),
                    red, green, blue, 0.0F);
        }
        BufferUploader.drawWithShader(builder.end());
    }

    private static void diagonal(PoseStack poseStack, float reveal, float direction,
                                 float alpha, float red, float green, float blue) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        int segments = 24;
        for (int i = 0; i <= segments; i++) {
            float fraction = (float) i / segments;
            float t = (fraction * 2.0F - 1.0F) * reveal;
            float x = t * 1.4F * direction;
            float y = t * 1.25F;
            float z = 1.05F + 0.1F * (1.0F - Math.abs(t));
            float edgeAlpha = alpha * (0.25F + 0.75F * (float) Math.sin(fraction * Math.PI));
            vertex(builder, matrix, x - 0.11F, y + 0.11F * direction, z,
                    red, green, blue, 0.0F);
            vertex(builder, matrix, x, y, z, red, green, blue, edgeAlpha);
            vertex(builder, matrix, x + 0.11F, y - 0.11F * direction, z,
                    red, green, blue, 0.0F);
        }
        BufferUploader.drawWithShader(builder.end());
    }

    private static void renderParry(PoseStack poseStack, SlashAnimation.Instance animation,
                                    long now, float partialTicks) {
        float pulse = animation.impactPulse(now, partialTicks);
        float alpha = 0.2F + 0.65F * pulse;
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        int segments = 28;
        float radius = 1.15F + 0.25F * pulse;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(-125.0F + 250.0F * i / segments);
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            float z = 0.65F;
            vertex(builder, matrix, sin * (radius - 0.055F), cos * (radius - 0.055F), z,
                    0.3F, 0.9F, 1.0F, 0.0F);
            vertex(builder, matrix, sin * radius, cos * radius, z, 0.3F, 0.9F, 1.0F, alpha);
            vertex(builder, matrix, sin * (radius + 0.055F), cos * (radius + 0.055F), z,
                    0.3F, 0.9F, 1.0F, 0.0F);
        }
        BufferUploader.drawWithShader(builder.end());
    }

    private static void vertex(BufferBuilder builder, Matrix4f matrix, float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        builder.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static double lerp(float delta, double start, double end) {
        return start + (end - start) * delta;
    }
}
