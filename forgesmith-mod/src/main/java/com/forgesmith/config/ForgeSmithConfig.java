package com.forgesmith.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.forgesmith.ForgeSmithMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * config/forgesmith.json
 */
public class ForgeSmithConfig {
    public boolean enabled = true;
    /** 每层绿宝石的吸血比例（按攻击伤害） */
    public double lifestealPerLayer = 0.10D;
    /** 青金石 3 层时水中受击的回补比例 */
    public double waterHealRatio = 0.20D;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ForgeSmithConfig instance;

    public static ForgeSmithConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("forgesmith.json");
        ForgeSmithConfig cfg = new ForgeSmithConfig();
        try {
            if (Files.exists(path)) {
                cfg = GSON.fromJson(Files.readString(path), ForgeSmithConfig.class);
                if (cfg == null) {
                    cfg = new ForgeSmithConfig();
                }
            }
        } catch (Exception e) {
            ForgeSmithMod.LOGGER.warn("Failed to read config, using defaults", e);
            cfg = new ForgeSmithConfig();
        }
        instance = cfg;
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance));
        } catch (IOException e) {
            ForgeSmithMod.LOGGER.warn("Failed to save config", e);
        }
    }
}
