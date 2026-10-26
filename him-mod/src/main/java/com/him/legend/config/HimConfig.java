package com.him.legend.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.him.legend.HimMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/him.json — 所有"闹鬼"行为的开关与节奏。
 */
public class HimConfig {
    public boolean allowNaturalSightings = true;
    /** 未点燃图腾时，自然目击的平均间隔（分钟，实际在其 60%~140% 间随机） */
    public int naturalSightingIntervalMinutes = 25;
    /** 点燃图腾后，异象事件的平均间隔（分钟） */
    public int hauntEventIntervalMinutes = 7;
    /** 事件间隔会随"目击次数"逐渐变短，这是下限（分钟） */
    public int hauntMinIntervalMinutes = 2;

    public boolean enableTunnels = true;
    public boolean enablePyramids = true;
    public boolean enableLeaflessTrees = true;
    public boolean enableFires = true;
    public boolean enableSigns = true;
    public boolean enableTorchSwap = true;

    /** 实体 303：允许其出现（单独追猎 + 双雄之战） */
    public boolean enableEntity303 = true;
    /** Herobrine vs Entity 303 的双雄之战事件 */
    public boolean enableBattles = true;
    /** 触发双雄之战所需的最少 Herobrine 目击次数 */
    public int battleMinSightings = 3;
    /** 实体 Null：AFK 凝视 / 世界篡改 / 参战 */
    public boolean enableNull = true;
    /** Null 的世界篡改事件（少量自然方块变成圆石） */
    public boolean enableWorldCorruption = true;
    /** 实体 404：故障猎杀事件 */
    public boolean enableEntity404 = true;
    /** 白眼（White Eyes）：黑暗目击 + 13 号唱片联动 */
    public boolean enableWhiteEyes = true;

    /** 无害模式：Herobrine 逼近时不再造成伤害，只有黑暗与雷声 */
    public boolean harmlessMode = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HimConfig instance;

    public static HimConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("him.json");
        HimConfig cfg = new HimConfig();
        try {
            if (Files.exists(path)) {
                cfg = GSON.fromJson(Files.readString(path), HimConfig.class);
                if (cfg == null) {
                    cfg = new HimConfig();
                }
            }
        } catch (Exception e) {
            HimMod.LOGGER.warn("Failed to read config, using defaults", e);
            cfg = new HimConfig();
        }
        instance = cfg;
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance));
        } catch (IOException e) {
            HimMod.LOGGER.warn("Failed to save config", e);
        }
    }
}
