package com.realcraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.realcraft.RealCraftMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/realcraft.json — 各现实化机制的开关与参数。
 */
public class RealCraftConfig {
    // ---- 火把 ----
    public boolean enableTorchBurnout = true;
    /** 火把燃料时长（tick）：9600 = 8 分钟（实际 ±1 分钟随机） */
    public int torchFuelTicks = 9600;
    /** 雨水浇熄火把 */
    public boolean enableRainExtinguish = true;

    // ---- 现实伐木 ----
    public boolean enableTreeFelling = true;
    /** 一棵树最多连锁倒下的原木数 */
    public int treeFellMaxLogs = 32;

    // ---- 真实摔落 ----
    public boolean enableFractures = true;
    /** 跛行阈值（格） */
    public float limpFallBlocks = 5.0F;
    /** 骨折阈值（格） */
    public float fractureFallBlocks = 10.0F;

    // ---- 负重 ----
    public boolean enableWeight = true;
    /** 软上限（物品件数）：超过 → 缓慢 I */
    public int weightSoft = 1296;
    /** 硬上限：超过 → 缓慢 II */
    public int weightHard = 1984;

    // ---- 深井高温 ----
    public boolean enableDeepHeat = true;
    /** 开始出现高温效应的 y 值（主世界） */
    public int deepHeatY = -24;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static RealCraftConfig instance;

    public static RealCraftConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("realcraft.json");
        RealCraftConfig cfg = new RealCraftConfig();
        try {
            if (Files.exists(path)) {
                cfg = GSON.fromJson(Files.readString(path), RealCraftConfig.class);
                if (cfg == null) {
                    cfg = new RealCraftConfig();
                }
            }
        } catch (Exception e) {
            RealCraftMod.LOGGER.warn("Failed to read config, using defaults", e);
            cfg = new RealCraftConfig();
        }
        instance = cfg;
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance));
        } catch (IOException e) {
            RealCraftMod.LOGGER.warn("Failed to save config", e);
        }
    }
}
