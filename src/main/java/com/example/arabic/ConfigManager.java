/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 * 
 * Licensed under the MIT License.
 */

package com.example.arabic;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public enum TranslationAPI {
        DEEPL("deepl"),
        GOOGLE("google");

        private final String id;

        TranslationAPI(String id) {
            this.id = id;
        }

        public static TranslationAPI fromString(String value) {
            try {
                return TranslationAPI.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return DEEPL;
            }
        }
    }

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createDefaultConfig() {
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
                
                String defaultConfig = "# ArabicTranslator Configuration\n" +
                        "# DeepL API Version: 'free' or 'pro'\n" +
                        "deepl-api-version: 'pro'\n" +
                        "\n" +
                        "# Get your API Key from: https://www.deepl.com/ja/your-account/keys\n" +
                        "# IMPORTANT: Always wrap the API Key with single quotes to avoid YAML parsing errors\n" +
                        "deepl-api-key: 'your-deepl-api-key-here'\n" +
                        "\n" +
                        "# Enable/disable translation on startup\n" +
                        "translation-enabled: false\n";
                
                java.nio.file.Files.write(configFile.toPath(), defaultConfig.getBytes());
                plugin.getLogger().info("Created default config.yml");
            }
            config = YamlConfiguration.loadConfiguration(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create config file: " + e.getMessage());
        }
    }

    public String getDeepLApiVersion() {
        return config.getString("deepl-api-version", "pro").toLowerCase();
    }

    public String getDeepLApiKey() {
        String key = config.getString("deepl-api-key", "");
        // Remove quotes if present
        if (key.startsWith("'") && key.endsWith("'")) {
            key = key.substring(1, key.length() - 1);
        }
        if (key.startsWith("\"") && key.endsWith("\"")) {
            key = key.substring(1, key.length() - 1);
        }
        return key;
    }

    public boolean isTranslationEnabled() {
        return config.getBoolean("translation-enabled", false);
    }

    public void setTranslationEnabled(boolean enabled) {
        config.set("translation-enabled", enabled);
    }

    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info("Configuration reloaded successfully");
    }
}
