package com.utilikit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.utilikit.UtiliKitMod;
import com.utilikit.config.UtiliKitConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 探测仪客户端逻辑：
 *  - 每 20 tick 扫描一次周围矿石（主手或副手持有时生效，副手 = 左手）
 *  - 相邻矿石聚合为"矿脉"（质心标记）
 *  - 世界渲染后期阶段，用 RenderType.lines() 绘制从眼部到矿脉的红色虚线 + 端点十字
 */
public final class ProspectorClient {
    private static final List<Vec3> VEINS = new ArrayList<>();
    private static int scanCounter = 0;
    private static int lastCount = -1;

    private ProspectorClient() {
    }

    private static boolean holding(Minecraft mc) {
        return mc.player != null
                && (mc.player.getMainHandItem().is(UtiliKitMod.PROSPECTOR)
                || mc.player.getOffhandItem().is(UtiliKitMod.PROSPECTOR));
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            VEINS.clear();
            lastCount = -1;
            return;
        }
        if (!holding(mc)) {
            VEINS.clear();
            lastCount = -1;
            scanCounter = 0;
            return;
        }
        if (++scanCounter < 20) {
            return;
        }
        scanCounter = 0;
        scan(mc);
    }

    private static boolean isOre(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(Blocks.NETHER_QUARTZ_ORE)
                || state.is(Blocks.NETHER_GOLD_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS);
    }

    private static void scan(Minecraft mc) {
        UtiliKitConfig cfg = UtiliKitConfig.get();
        int r = Math.max(4, Math.min(24, cfg.prospectorRadius));
        BlockPos center = mc.player.blockPosition();

        // [sx, sy, sz, n] —— 质心累加
        List<double[]> veins = new ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!isOre(mc.level.getBlockState(pos))) {
                        continue;
                    }
                    boolean merged = false;
                    for (double[] vein : veins) {
                        double cx = vein[0] / vein[3], cy = vein[1] / vein[3], cz = vein[2] / vein[3];
                        double dx = pos.getX() + 0.5D - cx, dy = pos.getY() + 0.5D - cy, dz = pos.getZ() + 0.5D - cz;
                        if (dx * dx + dy * dy + dz * dz <= 6.25D) {   // 2.5 格内视为同一矿脉
                            vein[0] += pos.getX() + 0.5D;
                            vein[1] += pos.getY() + 0.5D;
                            vein[2] += pos.getZ() + 0.5D;
                            vein[3]++;
                            merged = true;
                            break;
                        }
                    }
                    if (!merged && veins.size() < 64) {
                        veins.add(new double[]{pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 1.0D});
                    }
                }
            }
        }

        Vec3 player = mc.player.position();
        veins.sort(Comparator.comparingDouble(v -> distSqr(v, player)));
        VEINS.clear();
        int max = Math.max(1, cfg.prospectorMaxVeins);
        for (double[] vein : veins) {
            if (VEINS.size() >= max) {
                break;
            }
            VEINS.add(new Vec3(vein[0] / vein[3], vein[1] / vein[3], vein[2] / vein[3]));
        }

        if (VEINS.size() != lastCount) {
            lastCount = VEINS.size();
            mc.player.displayClientMessage(
                    Component.translatable("message.utilikit.prospector_detected", lastCount), true);
            if (lastCount > 0) {
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.25F, 0.5F);
            }
        }
    }

    private static double distSqr(double[] vein, Vec3 player) {
        double dx = vein[0] / vein[3] - player.x;
        double dy = vein[1] / vein[3] - player.y;
        double dz = vein[2] / vein[3] - player.z;
        return dx * dx + dy * dy + dz * dz;
    }

    // =====================================================================
    //  渲染：红色虚线 + 端点十字
    // =====================================================================

    public static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (VEINS.isEmpty() || !holding(mc)) {
            return;
        }
        MultiBufferSource consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        PoseStack poseStack = context.matrixStack();
        if (poseStack == null) {
            return;
        }
        Vec3 cam = context.camera().getPosition();
        VertexConsumer buffer = consumers.getBuffer(RenderType.lines());
        PoseStack.Pose entry = poseStack.last();
        Matrix4f pose = entry.pose();
        Matrix3f normal = entry.normal();

        Vec3 eye = mc.player.getEyePosition();
        float ex = (float) (eye.x - cam.x), ey = (float) (eye.y - cam.y), ez = (float) (eye.z - cam.z);
        float pulse = 0.5F + 0.35F * (float) Math.sin(Util.getMillis() / 240.0D);

        for (Vec3 vein : VEINS) {
            float vx = (float) (vein.x - cam.x), vy = (float) (vein.y - cam.y), vz = (float) (vein.z - cam.z);
            drawDashed(buffer, pose, normal, ex, ey, ez, vx, vy, vz, pulse);
            drawMarker(buffer, pose, normal, vx, vy, vz);
        }
    }

    private static void drawDashed(VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
                                   float x1, float y1, float z1, float x2, float y2, float z2, float alpha) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 2.0F) {
            return;
        }
        float ux = dx / len, uy = dy / len, uz = dz / len;
        float t = 1.2F;                      // 起点留白：不要糊在脸上
        while (t < len - 0.6F) {
            float t2 = Math.min(t + 0.7F, len - 0.4F);
            if (t2 - t < 0.15F) {
                break;
            }
            line(buffer, pose, normal,
                    x1 + ux * t, y1 + uy * t, z1 + uz * t,
                    x1 + ux * t2, y1 + uy * t2, z1 + uz * t2,
                    ux, uy, uz, alpha);
            t = t2 + 0.55F;                  // 虚线的"空段"
        }
    }

    private static void drawMarker(VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
                                   float x, float y, float z) {
        float s = 0.30F;
        line(buffer, pose, normal, x - s, y, z, x + s, y, z, 1.0F, 0.0F, 0.0F, 0.95F);
        line(buffer, pose, normal, x, y - s, z, x, y + s, z, 0.0F, 1.0F, 0.0F, 0.95F);
        line(buffer, pose, normal, x, y, z - s, x, y, z + s, 0.0F, 0.0F, 1.0F, 0.95F);
    }

    private static void line(VertexConsumer buffer, Matrix4f pose, Matrix3f normal,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float nx, float ny, float nz, float alpha) {
        buffer.vertex(pose, x1, y1, z1).color(1.0F, 0.16F, 0.12F, alpha).normal(normal, nx, ny, nz).next();
        buffer.vertex(pose, x2, y2, z2).color(1.0F, 0.16F, 0.12F, alpha).normal(normal, nx, ny, nz).next();
    }
}
