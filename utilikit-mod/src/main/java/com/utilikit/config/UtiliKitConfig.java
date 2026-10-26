package com.utilikit.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.utilikit.UtiliKitMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/utilikit.json
 */
public class UtiliKitConfig {
    /** 探测仪扫描半径（格，立方体） */
    public int prospectorRadius = 12;
    /** 探测仪最多同时标记的矿脉数 */
    public int prospectorMaxVeins = 24;
    /** 传送仪冷却（tick，60 = 3 秒） */
    public int teleportCooldownTicks = 60;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static UtiliKitConfig instance;

    public static UtiliKitConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("utilikit.json");
        UtiliKitConfig cfg = new UtiliKitConfig();
        try {
            if (Files.exists(path)) {
                cfg = GSON.fromJson(Files.readString(path), UtiliKitConfig.class);
                if (cfg == null) {
                    cfg = new UtiliKitConfig();
                }
            }
        } catch (Exception e) {
            UtiliKitMod.LOGGER.warn("Failed to read config, using defaults", e);
            cfg = new UtiliKitConfig();
        }
        instance = cfg;
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance));
        } catch (IOException e) {
            UtiliKitMod.LOGGER.warn("Failed to save config", e);
        }
    }
}
