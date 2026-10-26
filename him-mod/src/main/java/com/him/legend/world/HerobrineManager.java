package com.him.legend.world;

import com.him.legend.config.HimConfig;
import com.him.legend.HimMod;
import com.him.legend.entity.Entity303Entity;
import com.him.legend.entity.Entity404Entity;
import com.him.legend.entity.HerobrineEntity;
import com.him.legend.entity.NullEntity;
import com.him.legend.entity.WhiteEyesEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 服务端"闹鬼"状态机。
 *
 * 两条主线：
 *  stage 0（潜伏）：不点燃图腾时，只有极低频的自然目击——雾中远处那个白眼的身影。
 *  stage 1（被召唤）：点燃金块图腾后，异象频率大幅上升：逼近、隧道、无叶树、
 *                    水中沙金字塔、下界岩火、告示牌、火把调包……且随目击次数递增。
 *
 * Herobrine 的行为铁律（还原传说）：
 *  - 他不主动伤人，只凝视；
 *  - 你靠近他，他化作黑烟消失；
 *  - 你盯着他看几秒，他消失；
 *  - 你移开视线，再回头他就不在了；
 *  - 唯一的例外：深夜逼近事件——靠得太近，你会遭遇"直播被切断"般的黑暗。
 */
public final class HerobrineManager {
    private static final Map<UUID, Haunt> HAUNTS = new HashMap<>();
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final String[][] SIGN_MESSAGES = {
            {"WAKE UP", "", "", ""},
            {"STOP", "", "", ""},
            {"ONLY GOD", "CAN HELP", "YOU NOW", ""}
    };
    private static int cleanupCounter = 0;

    private HerobrineManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HerobrineManager::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        Set<UUID> online = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension() != Level.OVERWORLD) {
                continue; // Herobrine 是主世界（Overworld）的传说
            }
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                online.add(player.getUUID());
                haunt(player).tick(player, level);
            }
        }
        if (++cleanupCounter > 6000) {
            cleanupCounter = 0;
            HAUNTS.keySet().removeIf(uuid -> !online.contains(uuid));
        }
    }

    static Haunt haunt(ServerPlayer player) {
        return HAUNTS.computeIfAbsent(player.getUUID(), uuid -> new Haunt());
    }

    /** 点燃图腾 —— "召唤"成功，进入 stage 1 */
    public static void beginHaunt(ServerPlayer player, ServerLevel level, BlockPos totemPos) {
        Haunt h = haunt(player);
        if (h.stage < 1) {
            h.stage = 1;
            HimMod.LOGGER.info("[HIM] The totem has been lit. {} is now being haunted.", player.getGameProfile().getName());
        }
        h.nextEventAt = level.getGameTime() + 80L + player.getRandom().nextInt(60);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                totemPos.getX() + 0.5D, totemPos.getY() + 1.2D, totemPos.getZ() + 0.5D,
                50, 0.25D, 0.6D, 0.25D, 0.02D);
        level.playSound(null, totemPos.getX() + 0.5D, totemPos.getY() + 1.0D, totemPos.getZ() + 0.5D,
                SoundEvents.BLOCK_PORTAL_AMBIENT, SoundSource.AMBIENT, 0.9F, 0.45F);
        // 点燃图腾：他立刻现身一次（在雾的边缘，面朝你）
        if (h.active == null) {
            h.trySighting(player, level, 18, 34, HerobrineEntity.MODE_STARE);
        }
    }

    private static boolean isNatural(BlockState state) {
        return state.isAir()
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.COAL_ORES)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE);
    }

    // =====================================================================
    //  每个玩家的闹鬼状态
    // =====================================================================

    static class Haunt {
        int stage;          // 0 潜伏 / 1 已被召唤
        int sightings;      // 累计目击，越多越频繁
        long nextEventAt = -1L;
        HerobrineEntity active;
        int lifeTicks;
        int stareTicks;
        int awayTicks;
        BattleManager.Battle battle;    // 双雄之战（进行中暂停其他事件）
        Entity303Entity active303;      // 实体 303 单独追猎
        int lifeTicks303;
        Entity404Entity active404;      // 实体 404 故障猎杀
        int lifeTicks404;
        int idleTicks;                  // 挂机计时（Null 触发）
        double lastX = Double.NaN;
        double lastZ = Double.NaN;
        long disc13BoostUntil = Long.MIN_VALUE;   // 13 号唱片 → 白眼传说增益

        void tick(ServerPlayer player, ServerLevel level) {
            HimConfig cfg = HimConfig.get();
            long now = level.getGameTime();

            // 双雄之战进行中：围观即可，别的事件都停
            if (this.battle != null) {
                if (this.battle.finished) {
                    this.battle = null;
                } else {
                    return;
                }
            }

            if (this.active != null) {
                if (this.active.isRemoved()) {
                    this.active = null;
                } else {
                    this.trackActive(player, level, cfg);
                    return;
                }
            }

            if (this.active303 != null) {
                if (this.active303.isRemoved()) {
                    this.active303 = null;
                } else {
                    if (++this.lifeTicks303 > 900) {
                        this.active303.vanish(true);
                        this.active303 = null;
                    }
                    return;
                }
            }

            if (this.active404 != null) {
                if (this.active404.isRemoved()) {
                    this.active404 = null;
                } else {
                    if (++this.lifeTicks404 > 1200) {
                        this.active404.vanish();   // 超时强制消散（正常路径为高潮自灭）
                        this.active404 = null;
                    }
                    return;
                }
            }

            // 白眼传说联动：正在播放 13 号唱片 → 白眼目击率大幅提升（30 秒）
            if (cfg.enableWhiteEyes && now % 40L == 0L) {
                for (JukeboxBlockEntity jukebox : level.getBlockEntitiesOfClass(JukeboxBlockEntity.class,
                        player.getBoundingBox().inflate(24.0D))) {
                    if (jukebox.getRecord().is(Items.MUSIC_DISC_13)) {
                        this.disc13BoostUntil = now + 600L;
                        break;
                    }
                }
            }

            // Null：挂机 80 秒，他就会出现（AFK 凝视）
            if (cfg.enableNull && this.active == null) {
                boolean moved = !Double.isNaN(this.lastX)
                        && ((player.getX() - this.lastX) * (player.getX() - this.lastX)
                        + (player.getZ() - this.lastZ) * (player.getZ() - this.lastZ)) > 0.0009D;
                this.lastX = player.getX();
                this.lastZ = player.getZ();
                if (moved) {
                    this.idleTicks = 0;
                } else if (++this.idleTicks == 1600) {
                    this.spawnNull(player, level);
                }
            }

            if (this.stage == 0 && !cfg.allowNaturalSightings) {
                return;
            }
            if (this.nextEventAt < 0L || now >= this.nextEventAt) {
                if (this.fireEvent(player, level, cfg)) {
                    this.scheduleNext(player, level, cfg, now);
                } else {
                    // 本次尝试没有合适的落点/目标：稍后重试，保持节奏
                    this.nextEventAt = now + 400L + player.getRandom().nextInt(400);
                }
            }
        }

        private void scheduleNext(ServerPlayer player, ServerLevel level, HimConfig cfg, long now) {
            RandomSource random = player.getRandom();
            long baseMinutes;
            if (this.stage == 0) {
                baseMinutes = Math.max(1, cfg.naturalSightingIntervalMinutes);
            } else {
                baseMinutes = Math.max(cfg.hauntMinIntervalMinutes,
                        cfg.hauntEventIntervalMinutes - this.sightings / 3);
                baseMinutes = Math.max(1, baseMinutes);
            }
            long ticks = baseMinutes * 1200L;
            this.nextEventAt = now + ticks * (60L + random.nextInt(80)) / 100L;
        }

        /** @return 是否实际产生了事件（false = 落点/条件不满足） */
        private boolean fireEvent(ServerPlayer player, ServerLevel level, HimConfig cfg) {
            RandomSource random = player.getRandom();
            boolean night = level.isNight();
            int r = random.nextInt(100);

            if (this.stage == 0) {
                // 潜伏期：几乎只有"雾中目击"与诡异音效（303 极罕出现）
                if (cfg.enableEntity303 && r < 2) {
                    return this.stalk303(player, level, random);
                } else if (r < 60) {
                    return this.trySighting(player, level, 28, 48, HerobrineEntity.MODE_STARE);
                } else if (r < 85) {
                    ambientDread(player, level, random);
                    return true;
                }
                return false;
            }

            // 双雄之战：目击次数足够后，两位宿敌（Him/303/Null 任意配对）在你面前开战
            if (cfg.enableBattles && this.sightings >= cfg.battleMinSightings && r < 8) {
                return this.startBattle(player, level, random);
            } else if (cfg.enableEntity303 && r >= 8 && r < 14) {
                return this.stalk303(player, level, random);
            } else if (cfg.enableNull && cfg.enableWorldCorruption && r >= 14 && r < 16) {
                return this.corruptWorld(player, level, random);
            } else if (cfg.enableEntity404 && r >= 16 && r < 18) {
                return this.hunt404(player, level, random);
            } else if (cfg.enableWhiteEyes && r >= 18 && r < 21) {
                return this.whiteEyesSighting(player, level, random);
            } else if (night && r >= 21 && r < 34) {
                return this.trySighting(player, level, 26, 40, HerobrineEntity.MODE_APPROACH);
            } else if (r >= 34 && r < 52) {
                return this.trySighting(player, level, 22, 45, HerobrineEntity.MODE_STARE);
            } else if (r >= 52 && r < 62 && cfg.enableTunnels) {
                return this.carveTunnel(player, level, random);
            } else if (r >= 62 && r < 70 && cfg.enableLeaflessTrees) {
                return this.stripTrees(player, level, random);
            } else if (r >= 70 && r < 77 && cfg.enablePyramids) {
                return this.buildPyramid(player, level, random);
            } else if (r >= 77 && r < 84 && cfg.enableFires) {
                return this.lightFire(player, level, random);
            } else if (r >= 84 && r < 88 && cfg.enableSigns) {
                return this.placeSign(player, level, random);
            } else if (r >= 88 && r < 90 && cfg.enableTorchSwap) {
                return this.swapTorch(player, level, random);
            } else {
                ambientDread(player, level, random);
                return true;
            }
        }

        // ------------------------------------------------------------------
        //  目击 / 逼近
        // ------------------------------------------------------------------

        private boolean trySighting(ServerPlayer player, ServerLevel level, int minDist, int maxDist, int mode) {
            RandomSource random = player.getRandom();
            for (int attempt = 0; attempt < 10; attempt++) {
                Vec3 look = player.getLookAngle();
                if (mode == HerobrineEntity.MODE_APPROACH) {
                    look = look.scale(-1.0D); // 身后
                }
                double rotate = (random.nextDouble() - 0.5D) * 1.2D; // ±约34°
                double cos = Math.cos(rotate), sin = Math.sin(rotate);
                double dx = look.x * cos - look.z * sin;
                double dz = look.x * sin + look.z * cos;
                double dist = minDist + random.nextDouble() * (maxDist - minDist);

                int x = Mth.floor(player.getX() + dx * dist);
                int z = Mth.floor(player.getZ() + dz * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - player.getBlockY()) > 16) {
                    continue;
                }
                BlockPos ground = new BlockPos(x, y - 1, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(ground).isSolidRender(level, ground)) {
                    continue;
                }
                if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) {
                    continue;
                }

                float yaw = HerobrineEntity.yawToward(x + 0.5D, z + 0.5D, player.getX(), player.getZ());
                // 黑暗处小概率是"白眼"（Herobrine 传说的前身）；13 号唱片播放中概率大增
                HimConfig cfg = HimConfig.get();
                boolean dark = level.getBrightness(LightLayer.BLOCK, feet) <= 3;
                boolean discBoost = level.getGameTime() < this.disc13BoostUntil;
                boolean whiteVariant = cfg.enableWhiteEyes && dark
                        && (discBoost ? random.nextInt(5) < 3 : random.nextInt(10) == 0);
                HerobrineEntity hb = whiteVariant
                        ? new WhiteEyesEntity(HimMod.WHITE_EYES, level)
                        : new HerobrineEntity(HimMod.HEROBRINE, level);
                hb.moveTo(x + 0.5D, y, z + 0.5D, yaw, 0.0F);
                hb.setYHeadRot(yaw);
                hb.setSilent(true);
                hb.setInvulnerable(true);
                hb.setMode(mode);
                if (level.addFreshEntity(hb)) {
                    this.active = hb;
                    this.lifeTicks = 0;
                    this.stareTicks = 0;
                    this.awayTicks = 0;
                    this.sightings++;
                    if (mode == HerobrineEntity.MODE_APPROACH) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.7F, 0.6F);
                    } else if (random.nextInt(3) == 0) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENTITY_ENDERMAN_STARE, SoundSource.HOSTILE, 0.35F, 0.65F);
                    }
                    return true;
                }
            }
            return false;
        }

        /** 实体 303 单独追猎：从玩家背后现身，火焰之爪 + 短暂黑暗 */
        private boolean stalk303(ServerPlayer player, ServerLevel level, RandomSource random) {
            Vec3 look = player.getLookAngle().scale(-1.0D);
            for (int attempt = 0; attempt < 8; attempt++) {
                double rotate = (random.nextDouble() - 0.5D) * 0.8D;
                double cos = Math.cos(rotate), sin = Math.sin(rotate);
                double dx = look.x * cos - look.z * sin;
                double dz = look.x * sin + look.z * cos;
                double dist = 24.0D + random.nextDouble() * 12.0D;
                int x = Mth.floor(player.getX() + dx * dist);
                int z = Mth.floor(player.getZ() + dz * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - player.getBlockY()) > 16) {
                    continue;
                }
                BlockPos ground = new BlockPos(x, y - 1, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(ground).isSolidRender(level, ground)) {
                    continue;
                }
                if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) {
                    continue;
                }
                Entity303Entity e303 = new Entity303Entity(HimMod.ENTITY_303, level);
                float yaw = HerobrineEntity.yawToward(x + 0.5D, z + 0.5D, player.getX(), player.getZ());
                e303.moveTo(x + 0.5D, y, z + 0.5D, yaw, 0.0F);
                e303.setYHeadRot(yaw);
                e303.setSilent(true);
                e303.setInvulnerable(true);
                e303.setMode(Entity303Entity.MODE_STALK);
                if (level.addFreshEntity(e303)) {
                    this.active303 = e303;
                    this.lifeTicks303 = 0;
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.6F, 0.7F);
                    return true;
                }
            }
            return false;
        }

        /** 双雄之战：在玩家视野内平整处摆开战场，两位宿敌捉对厮杀 */
        private boolean startBattle(ServerPlayer player, ServerLevel level, RandomSource random) {
            HimConfig cfg = HimConfig.get();
            for (int attempt = 0; attempt < 10; attempt++) {
                Vec3 look = player.getLookAngle();
                double rotate = (random.nextDouble() - 0.5D) * 0.9D;
                double cos = Math.cos(rotate), sin = Math.sin(rotate);
                double dx = look.x * cos - look.z * sin;
                double dz = look.x * sin + look.z * cos;
                double dist = 24.0D + random.nextDouble() * 12.0D;
                int cx = Mth.floor(player.getX() + dx * dist);
                int cz = Mth.floor(player.getZ() + dz * dist);
                double plen = Math.sqrt(dx * dx + dz * dz);
                if (plen < 0.01D) {
                    continue;
                }
                double px = -dz / plen, pz = dx / plen;   // 垂直方向：两位主角分立两侧
                int x1 = Mth.floor(cx + px * 5.0D), z1 = Mth.floor(cz + pz * 5.0D);
                int x2 = Mth.floor(cx - px * 5.0D), z2 = Mth.floor(cz - pz * 5.0D);
                int y1 = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x1, z1);
                int y2 = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x2, z2);
                if (Math.abs(y1 - player.getBlockY()) > 12 || Math.abs(y2 - player.getBlockY()) > 12
                        || Math.abs(y1 - y2) > 3) {
                    continue;
                }
                BlockPos g1 = new BlockPos(x1, y1 - 1, z1), g2 = new BlockPos(x2, y2 - 1, z2);
                if (!level.getBlockState(g1).isSolidRender(level, g1)
                        || !level.getBlockState(g2).isSolidRender(level, g2)) {
                    continue;
                }
                BlockPos f1 = new BlockPos(x1, y1, z1), f2 = new BlockPos(x2, y2, z2);
                if (!level.getBlockState(f1).isAir() || !level.getBlockState(f1.above()).isAir()
                        || !level.getBlockState(f2).isAir() || !level.getBlockState(f2.above()).isAir()) {
                    continue;
                }

                float yaw1 = HerobrineEntity.yawToward(x1 + 0.5D, z1 + 0.5D, x2 + 0.5D, z2 + 0.5D);
                float yaw2 = HerobrineEntity.yawToward(x2 + 0.5D, z2 + 0.5D, x1 + 0.5D, z1 + 0.5D);
                // 战士级宿敌任意配对：Him vs 303（默认）/ Null vs 303 / Him vs Null
                int roll = cfg.enableNull ? random.nextInt(4) : 3;
                LivingEntity sideA;
                LivingEntity sideB;
                switch (roll) {
                    case 0 -> {   // Null vs Entity 303
                        sideA = new NullEntity(HimMod.NULL, level);
                        sideB = new Entity303Entity(HimMod.ENTITY_303, level);
                    }
                    case 1 -> {   // Herobrine vs Null
                        sideA = new HerobrineEntity(HimMod.HEROBRINE, level);
                        sideB = new NullEntity(HimMod.NULL, level);
                    }
                    default -> {
                        sideA = new HerobrineEntity(HimMod.HEROBRINE, level);
                        sideB = new Entity303Entity(HimMod.ENTITY_303, level);
                    }
                }
                pose(sideA, x1 + 0.5D, y1, z1 + 0.5D, yaw1);
                pose(sideB, x2 + 0.5D, y2, z2 + 0.5D, yaw2);
                if (level.addFreshEntity(sideA) && level.addFreshEntity(sideB)) {
                    this.battle = BattleManager.start(player, level, sideA, sideB);
                    this.sightings++;
                    return true;
                }
                sideA.discard();
                sideB.discard();
            }
            return false;
        }

        private static void pose(LivingEntity entity, double x, int y, double z, float yaw) {
            entity.moveTo(x, y, z, yaw, 0.0F);
            entity.setYHeadRot(yaw);
            entity.setSilent(true);
            entity.setInvulnerable(true);
        }

        /** Null 的 AFK 凝视：挂机时他从雾里走出来，就站在那看着你 */
        private void spawnNull(ServerPlayer player, ServerLevel level) {
            RandomSource random = player.getRandom();
            for (int attempt = 0; attempt < 8; attempt++) {
                double ang = random.nextDouble() * Math.PI * 2.0D;
                double dist = 28.0D + random.nextDouble() * 14.0D;
                int x = Mth.floor(player.getX() + Math.sin(ang) * dist);
                int z = Mth.floor(player.getZ() + Math.cos(ang) * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - player.getBlockY()) > 14) {
                    continue;
                }
                BlockPos ground = new BlockPos(x, y - 1, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(ground).isSolidRender(level, ground)) {
                    continue;
                }
                if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) {
                    continue;
                }
                float yaw = HerobrineEntity.yawToward(x + 0.5D, z + 0.5D, player.getX(), player.getZ());
                NullEntity nul = new NullEntity(HimMod.NULL, level);
                pose(nul, x + 0.5D, y, z + 0.5D, yaw);
                nul.setMode(HerobrineEntity.MODE_STARE);
                if (level.addFreshEntity(nul)) {
                    this.active = nul;
                    this.lifeTicks = 0;
                    this.stareTicks = 0;
                    this.awayTicks = 0;
                    this.sightings++;
                    // 静电般的"滋"声 —— Null 出现的信号
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.25F, 2.0F);
                }
                return;
            }
        }

        /** Null 的世界篡改：少量自然方块被替换成圆石，伴随灰烬飘落 */
        private boolean corruptWorld(ServerPlayer player, ServerLevel level, RandomSource random) {
            double ang = random.nextDouble() * Math.PI * 2.0D;
            double dist = 10.0D + random.nextDouble() * 18.0D;
            int cx = Mth.floor(player.getX() + Math.sin(ang) * dist);
            int cz = Mth.floor(player.getZ() + Math.cos(ang) * dist);
            int changed = 0;
            for (int i = 0; i < 6; i++) {
                int bx = cx + random.nextInt(7) - 3;
                int bz = cz + random.nextInt(7) - 3;
                int by = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1;
                BlockPos pos = new BlockPos(bx, by, bz);
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.STONE) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                        || state.is(Blocks.DEEPSLATE)) {
                    level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.ASH,
                            bx + 0.5D, by + 1.1D, bz + 0.5D, 8, 0.3D, 0.2D, 0.3D, 0.02D);
                    changed++;
                }
            }
            if (changed > 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.25F, 2.0F);
            }
            return changed > 0;
        }

        /** 实体 404 故障猎杀：从远处开始闪烁逼近，错误文本刷屏，贴身触发假断线 */
        private boolean hunt404(ServerPlayer player, ServerLevel level, RandomSource random) {
            Vec3 look = player.getLookAngle().scale(-1.0D);
            for (int attempt = 0; attempt < 8; attempt++) {
                double rotate = (random.nextDouble() - 0.5D) * 1.6D;
                double cos = Math.cos(rotate), sin = Math.sin(rotate);
                double dx = look.x * cos - look.z * sin;
                double dz = look.x * sin + look.z * cos;
                double dist = 28.0D + random.nextDouble() * 14.0D;
                int x = Mth.floor(player.getX() + dx * dist);
                int z = Mth.floor(player.getZ() + dz * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - player.getBlockY()) > 14) {
                    continue;
                }
                BlockPos ground = new BlockPos(x, y - 1, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(ground).isSolidRender(level, ground)) {
                    continue;
                }
                if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) {
                    continue;
                }
                float yaw = HerobrineEntity.yawToward(x + 0.5D, z + 0.5D, player.getX(), player.getZ());
                Entity404Entity e404 = new Entity404Entity(HimMod.ENTITY_404, level);
                pose(e404, x + 0.5D, y, z + 0.5D, yaw);
                if (level.addFreshEntity(e404)) {
                    this.active404 = e404;
                    this.lifeTicks404 = 0;
                    player.displayClientMessage(Component.translatable(
                                    "error.him.404." + (1 + random.nextInt(6)))
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
                    return true;
                }
                return false;
            }
            return false;
        }

        /** 白眼专场：只找黑暗中的落点（光级 ≤ 3） */
        private boolean whiteEyesSighting(ServerPlayer player, ServerLevel level, RandomSource random) {
            for (int attempt = 0; attempt < 12; attempt++) {
                double ang = random.nextDouble() * Math.PI * 2.0D;
                double dist = 16.0D + random.nextDouble() * 22.0D;
                int x = Mth.floor(player.getX() + Math.sin(ang) * dist);
                int z = Mth.floor(player.getZ() + Math.cos(ang) * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - player.getBlockY()) > 14) {
                    continue;
                }
                BlockPos ground = new BlockPos(x, y - 1, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(ground).isSolidRender(level, ground)) {
                    continue;
                }
                if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) {
                    continue;
                }
                if (level.getBrightness(LightLayer.BLOCK, feet) > 3) {
                    continue;   // 白眼只出现在黑暗里
                }
                float yaw = HerobrineEntity.yawToward(x + 0.5D, z + 0.5D, player.getX(), player.getZ());
                WhiteEyesEntity eyes = new WhiteEyesEntity(HimMod.WHITE_EYES, level);
                pose(eyes, x + 0.5D, y, z + 0.5D, yaw);
                eyes.setMode(HerobrineEntity.MODE_STARE);
                if (level.addFreshEntity(eyes)) {
                    this.active = eyes;
                    this.lifeTicks = 0;
                    this.stareTicks = 0;
                    this.awayTicks = 0;
                    this.sightings++;
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_ENDERMAN_STARE, SoundSource.HOSTILE, 0.2F, 1.2F);
                    return true;
                }
                return false;
            }
            return false;
        }

        private void trackActive(ServerPlayer player, ServerLevel level, HimConfig cfg) {
            this.lifeTicks++;
            HerobrineEntity hb = this.active;
            if (hb == null) {
                return;
            }
            double d = player.distanceTo(hb);
            boolean looking = this.isPlayerLookingAt(player, hb);
            int mode = hb.getMode();

            if (mode == HerobrineEntity.MODE_APPROACH) {
                // 你看着他，他就停在原地；移开视线，他才会继续逼近
                hb.setFrozen(this.isPlayerLookingAt(player, hb));
                if (d < 4.5D) {
                    this.punish(player, level, cfg);
                    hb.vanish(true);
                    this.active = null;
                    return;
                }
                if (this.lifeTicks > 2400) {
                    hb.vanish(false);
                    this.active = null;
                    return;
                }
                return;
            }

            // MODE_STARE：传说三定律 + 各角色特例
            if (hb instanceof WhiteEyesEntity) {
                // 白眼只留在黑暗中：被火把照亮、或靠得太近，就无声退回黑暗
                if (level.getBrightness(LightLayer.BLOCK, hb.blockPosition()) > 3 || d < 20.0D) {
                    hb.vanish(d < 20.0D);
                    this.active = null;
                }
                return;
            }
            if (hb instanceof NullEntity) {
                // Null 无视注视与移开视线：只有你靠近他、或时间耗尽，他才会走
                if (d < 14.0D) {
                    hb.vanish(true);
                    this.active = null;
                } else if (this.lifeTicks > 2400) {
                    hb.vanish(false);
                    this.active = null;
                }
                return;
            }
            if (d < 14.0D) {                       // 靠近即消失
                hb.vanish(true);
                this.active = null;
                return;
            }
            if (looking) {
                this.stareTicks++;
                this.awayTicks = 0;
            } else {
                this.awayTicks++;
                this.stareTicks = Math.max(0, this.stareTicks - 2);
            }
            if (this.stareTicks > 45) {            // 被注视太久
                hb.vanish(true);
                this.active = null;
                return;
            }
            if (this.awayTicks > 140 || this.lifeTicks > 1400) { // 你不看他，他悄悄离开
                hb.vanish(false);
                this.active = null;
            }
        }

        private boolean isPlayerLookingAt(ServerPlayer player, HerobrineEntity hb) {
            Vec3 eye = player.getEyePosition();
            Vec3 target = new Vec3(hb.getX(), hb.getEyeY(), hb.getZ());
            Vec3 dir = target.subtract(eye).normalize();
            return player.getLookAngle().dot(dir) > 0.982D;
        }

        private void punish(ServerPlayer player, ServerLevel level, HimConfig cfg) {
            // "直播被切断"：Copeland 看到 Herobrine 的那一刻，直播戛然而止
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, false, false));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.9F, 0.55F);
            if (!cfg.harmlessMode) {
                player.hurt(level.damageSources().magic(), 6.0F);
            }
        }

        // ------------------------------------------------------------------
        //  世界异象
        // ------------------------------------------------------------------

        /** 2x2 隧道：直穿山体，里面插着红石火把，有时他就站在尽头 */
        private boolean carveTunnel(ServerPlayer player, ServerLevel level, RandomSource random) {
            for (int attempt = 0; attempt < 14; attempt++) {
                double ang = random.nextDouble() * Math.PI * 2.0D;
                double dist = 34.0D + random.nextDouble() * 34.0D;
                int hx = Mth.floor(player.getX() + Math.sin(ang) * dist);
                int hz = Mth.floor(player.getZ() + Math.cos(ang) * dist);

                int[] dir = null;
                int[] perp = null;
                int h0 = 0;
                for (int[] d : DIRS) {
                    h0 = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hx, hz);
                    int hIn = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            hx + d[0] * 10, hz + d[1] * 10);
                    if (hIn - h0 >= 3 && h0 > level.getMinBuildHeight() + 2) {
                        dir = d;
                        perp = new int[]{-d[1], d[0]};
                        break;
                    }
                }
                if (dir == null) {
                    continue;
                }

                int length = 12 + random.nextInt(8);
                List<BlockPos> toCarve = new ArrayList<>();
                boolean ok = true;
                for (int i = 0; i < length && ok; i++) {
                    int bx = hx + dir[0] * i;
                    int bz = hz + dir[1] * i;
                    for (int w = 0; w < 2 && ok; w++) {
                        for (int dy = 0; dy < 2; dy++) {
                            BlockPos p = new BlockPos(bx + perp[0] * w, h0 + dy, bz + perp[1] * w);
                            BlockState s = level.getBlockState(p);
                            if (s.isAir()) {
                                continue;
                            }
                            if (!isNatural(s)) {
                                ok = false; // 碰到任何人工方块，立刻放弃（不破坏玩家建筑）
                                break;
                            }
                            toCarve.add(p);
                        }
                    }
                }
                if (!ok || toCarve.size() < 8) {
                    continue;
                }

                for (BlockPos p : toCarve) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
                int torches = 1 + random.nextInt(2);
                for (int t = 1; t <= torches; t++) {
                    int i = length * t / (torches + 1);
                    BlockPos torchPos = new BlockPos(hx + dir[0] * i, h0, hz + dir[1] * i);
                    BlockPos floorPos = torchPos.below();
                    if (level.getBlockState(floorPos).isSolidRender(level, floorPos)
                            && level.getBlockState(torchPos).isAir()) {
                        level.setBlock(torchPos, Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
                    }
                }
                // 35%：他就在隧道深处，面朝入口
                if (random.nextInt(100) < 35) {
                    int i = Math.max(2, length - 3);
                    double ex = hx + dir[0] * i + 0.5D + perp[0] * 0.5D;
                    double ez = hz + dir[1] * i + 0.5D + perp[1] * 0.5D;
                    float yaw = HerobrineEntity.yawToward(ex, ez, hx, hz);
                    HerobrineEntity hb = new HerobrineEntity(HimMod.HEROBRINE, level);
                    hb.moveTo(ex, h0, ez, yaw, 0.0F);
                    hb.setYHeadRot(yaw);
                    hb.setSilent(true);
                    hb.setInvulnerable(true);
                    hb.setMode(HerobrineEntity.MODE_STARE);
                    if (level.addFreshEntity(hb)) {
                        this.active = hb;
                        this.lifeTicks = 0;
                        this.stareTicks = 0;
                        this.awayTicks = 0;
                        this.sightings++;
                    }
                }
                return true;
            }
            return false;
        }

        /** 被扒光树叶的树 */
        private boolean stripTrees(ServerPlayer player, ServerLevel level, RandomSource random) {
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = player.getBlockX() + random.nextInt(57) - 28;
                int z = player.getBlockZ() + random.nextInt(57) - 28;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int dy = 0; dy < 12; dy++) {
                    BlockPos trunk = new BlockPos(x, y + dy, z);
                    if (!level.getBlockState(trunk).is(BlockTags.LOGS)) {
                        continue;
                    }
                    int removed = 0;
                    for (int lx = -2; lx <= 2; lx++) {
                        for (int ly = -1; ly <= 6; ly++) {
                            for (int lz = -2; lz <= 2; lz++) {
                                BlockPos lp = trunk.offset(lx, ly, lz);
                                if (level.getBlockState(lp).is(BlockTags.LEAVES)) {
                                    level.setBlock(lp, Blocks.AIR.defaultBlockState(), 3);
                                    removed++;
                                }
                            }
                        }
                    }
                    return removed > 0;
                }
            }
            return false;
        }

        /** 水中的小沙金字塔 */
        private boolean buildPyramid(ServerPlayer player, ServerLevel level, RandomSource random) {
            for (int attempt = 0; attempt < 12; attempt++) {
                double ang = random.nextDouble() * Math.PI * 2.0D;
                double dist = 16.0D + random.nextDouble() * 32.0D;
                int x = Mth.floor(player.getX() + Math.sin(ang) * dist);
                int z = Mth.floor(player.getZ() + Math.cos(ang) * dist);
                int surf = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (!level.getBlockState(new BlockPos(x, surf - 1, z)).is(Blocks.WATER)) {
                    continue;
                }
                int floorY = surf - 1;
                BlockPos below = new BlockPos(x, floorY - 1, z);
                while (floorY > level.getMinBuildHeight() + 1
                        && level.getBlockState(below).is(Blocks.WATER)) {
                    floorY--;
                    below = below.below();
                }
                if (!level.getBlockState(below).isSolidRender(level, below)) {
                    continue;
                }
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        setIfWater(level, new BlockPos(x + dx, floorY, z + dz), Blocks.SAND);
                    }
                }
                setIfWater(level, new BlockPos(x, floorY + 1, z), Blocks.SAND);
                setIfWater(level, new BlockPos(x + 1, floorY + 1, z), Blocks.SAND);
                setIfWater(level, new BlockPos(x - 1, floorY + 1, z), Blocks.SAND);
                setIfWater(level, new BlockPos(x, floorY + 1, z + 1), Blocks.SAND);
                setIfWater(level, new BlockPos(x, floorY + 1, z - 1), Blocks.SAND);
                setIfWater(level, new BlockPos(x, floorY + 2, z), Blocks.SAND);
                return true;
            }
            return false;
        }

        /** 燃烧的下界岩 */
        private boolean lightFire(ServerPlayer player, ServerLevel level, RandomSource random) {
            for (int attempt = 0; attempt < 10; attempt++) {
                double ang = random.nextDouble() * Math.PI * 2.0D;
                double dist = 18.0D + random.nextDouble() * 22.0D;
                int x = Mth.floor(player.getX() + Math.sin(ang) * dist);
                int z = Mth.floor(player.getZ() + Math.cos(ang) * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - player.getBlockY()) > 12) {
                    continue;
                }
                BlockPos ground = new BlockPos(x, y - 1, z);
                if (!level.getBlockState(ground).isSolidRender(level, ground)) {
                    continue;
                }
                BlockPos at = new BlockPos(x, y, z);
                if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) {
                    continue;
                }
                level.setBlock(at, Blocks.NETHERRACK.defaultBlockState(), 3);
                level.setBlock(at.above(), Blocks.FIRE.defaultBlockState(), 3);
                return true;
            }
            return false;
        }

        /** 诡异告示牌："WAKE UP" / "STOP" / "ONLY GOD CAN HELP YOU NOW" */
        private boolean placeSign(ServerPlayer player, ServerLevel level, RandomSource random) {
            for (int attempt = 0; attempt < 10; attempt++) {
                double ang = random.nextDouble() * Math.PI * 2.0D;
                double dist = 6.0D + random.nextDouble() * 10.0D;
                int x = Mth.floor(player.getX() + Math.sin(ang) * dist);
                int z = Mth.floor(player.getZ() + Math.cos(ang) * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos ground = new BlockPos(x, y - 1, z);
                BlockPos at = new BlockPos(x, y, z);
                if (!level.getBlockState(ground).isSolidRender(level, ground)) {
                    continue;
                }
                if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) {
                    continue;
                }
                level.setBlock(at, Blocks.OAK_SIGN.defaultBlockState(), 3);
                if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
                    String[] lines = SIGN_MESSAGES[random.nextInt(SIGN_MESSAGES.length)];
                    for (int i = 0; i < 4; i++) {
                        sign.setMessage(i, Component.literal(lines[i]));
                    }
                    sign.setWaxed(true);
                }
                return true;
            }
            return false;
        }

        /** 你家的火把，被换成了红石火把 */
        private boolean swapTorch(ServerPlayer player, ServerLevel level, RandomSource random) {
            for (int attempt = 0; attempt < 16; attempt++) {
                int x = player.getBlockX() + random.nextInt(33) - 16;
                int y = player.getBlockY() + random.nextInt(7) - 3;
                int z = player.getBlockZ() + random.nextInt(33) - 16;
                BlockPos p = new BlockPos(x, y, z);
                if (level.getBlockState(p).is(Blocks.TORCH)) {
                    level.setBlock(p, Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
                    return true;
                }
            }
            return false;
        }

        private static void setIfWater(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
            if (level.getBlockState(pos).is(Blocks.WATER)) {
                level.setBlock(pos, block.defaultBlockState(), 3);
            }
        }

        private static void ambientDread(ServerPlayer player, ServerLevel level, RandomSource random) {
            SoundEvent sound;
            switch (random.nextInt(3)) {
                case 0 -> sound = SoundEvents.ENTITY_ENDERMAN_STARE;
                case 1 -> sound = SoundEvents.BLOCK_PORTAL_AMBIENT;
                default -> sound = SoundEvents.ENTITY_WARDEN_HEARTBEAT;
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound,
                    SoundSource.HOSTILE, 0.35F, 0.6F);
        }
    }
}
