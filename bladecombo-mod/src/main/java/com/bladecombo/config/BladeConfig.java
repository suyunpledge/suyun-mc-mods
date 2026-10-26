package com.bladecombo.config;

import com.bladecombo.BladeComboMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/bladecombo.json
 */
public class BladeConfig {
    /** 连招窗口：命中后多少 tick 内继续攻击算衔接（15 = 0.75 秒，手残党友好） */
    public int comboWindowTicks = 15;
    /** 两次斩击最小间隔 */
    public int swingIntervalTicks = 6;
    /** 格挡架势持续 */
    public int parryDurationTicks = 30;
    /** 格挡冷却 */
    public int parryCooldownTicks = 60;
    /** 格挡时受到的伤害比例 */
    public double parryDamageFactor = 0.10D;
    /** 格挡弹刀反伤比例 */
    public double parryReflectFactor = 0.30D;
    /** 空中连段状态持续 */
    public int airComboTicks = 80;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BladeConfig instance;

    public static BladeConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("bladecombo.json");
        BladeConfig cfg = new BladeConfig();
        try {
            if (Files.exists(path)) {
                cfg = GSON.fromJson(Files.readString(path), BladeConfig.class);
                if (cfg == null) {
                    cfg = new BladeConfig();
                }
            }
        } catch (Exception e) {
            BladeComboMod.LOGGER.warn("Failed to read config, using defaults", e);
            cfg = new BladeConfig();
        }
        instance = cfg;
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance));
        } catch (IOException e) {
            BladeComboMod.LOGGER.warn("Failed to save config", e);
        }
    }
}
