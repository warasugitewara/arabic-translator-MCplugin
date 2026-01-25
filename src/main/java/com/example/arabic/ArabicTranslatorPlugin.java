/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 * 
 * Licensed under the MIT License.
 */

package com.example.arabic;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import io.papermc.paper.event.player.ChatEvent;

public class ArabicTranslatorPlugin extends JavaPlugin implements Listener {
    
    private TranslationManager translationManager;
    private ConfigManager configManager;
    private boolean translationEnabled = false;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        String deepLKey = configManager.getDeepLApiKey();
        
        getLogger().info("[ArabicTranslator] Loaded DeepL Key: " + (deepLKey != null && !deepLKey.isEmpty() ? "YES (length: " + deepLKey.length() + ")" : "NO"));
        
        translationManager = new TranslationManager(this, configManager);
        getServer().getPluginManager().registerEvents(this, this);
        
        getCommand("arabic").setTabCompleter(new ArabicCommandCompleter());
        
        getCommand("arabic").setExecutor((sender, cmd, label, args) -> {
            if (args.length == 0) {
                showHelp(sender);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("enable")) {
                String apiKey = getValidAPIKey();
                if (apiKey == null) {
                    sender.sendMessage(Component.text("Error: No valid API Key configured!", NamedTextColor.RED));
                    return true;
                }
                translationEnabled = true;
                broadcastMessage("✓ Arabic translation enabled (DeepL API)", NamedTextColor.GREEN);
                return true;

            } else if (subCommand.equals("disable")) {
                translationEnabled = false;
                broadcastMessage("✗ Arabic translation disabled", NamedTextColor.RED);
                return true;

            } else if (subCommand.equals("status")) {
                String status = translationEnabled ? "ENABLED" : "DISABLED";
                NamedTextColor color = translationEnabled ? NamedTextColor.GREEN : NamedTextColor.RED;
                sender.sendMessage(Component.text("Arabic translation: " + status, color));
                return true;

            } else if (subCommand.equals("help")) {
                showHelp(sender);
                return true;

            } else if (subCommand.equals("reload")) {
                if (!sender.hasPermission("arabic.reload")) {
                    sender.sendMessage(Component.text("You don't have permission!", NamedTextColor.RED));
                    return true;
                }
                configManager.reloadConfig();
                String reloadDeepLKey = configManager.getDeepLApiKey();
                String reloadDeepLVersion = configManager.getDeepLApiVersion();
                translationManager.setDeepLApiKey(reloadDeepLKey);
                translationManager.setDeepLApiUrl(reloadDeepLVersion);
                broadcastMessage("✓ Configuration reloaded (Version: " + reloadDeepLVersion.toUpperCase() + ")", NamedTextColor.AQUA);
                return true;
            }

            showHelp(sender);
            return true;
        });

        getLogger().info("ArabicTranslator enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ArabicTranslator disabled!");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChatEvent(ChatEvent event) {
        if (!translationEnabled) {
            return;
        }

        Component messageComponent = event.message();
        String originalMessage = PlainTextComponentSerializer.plainText().serialize(messageComponent);
        
        if (originalMessage.isEmpty() || originalMessage.startsWith("/")) {
            return;
        }

        Player player = event.getPlayer();
        getLogger().info("[ArabicTranslator] ChatEvent detected from " + player.getName() + ": " + originalMessage);

        translationManager.translateAsync(originalMessage, translationResult -> {
            Bukkit.getScheduler().runTask(this, () -> {
                if (translationResult == null) {
                    getLogger().warning("[ArabicTranslator] Translation returned null");
                    return;
                }

                String playerName = player.getName();
                Component arabicTranslation = Component.text(
                    translationResult.getArabic(),
                    NamedTextColor.LIGHT_PURPLE
                ).decoration(TextDecoration.BOLD, true);

                Component romanization = Component.text(
                    translationResult.getRomanization(),
                    NamedTextColor.YELLOW
                ).decoration(TextDecoration.ITALIC, true);

                Component separator = Component.text(" | ", NamedTextColor.GRAY);

                Component fullComponent = Component.text(playerName + ": ", NamedTextColor.WHITE)
                    .append(arabicTranslation)
                    .append(separator)
                    .append(romanization);

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.sendMessage(fullComponent);
                }
                getLogger().info("[ArabicTranslator] Translation sent: " + translationResult.getArabic());
            });
        });
    }



    private String getValidAPIKey() {
        String key = configManager.getDeepLApiKey();
        if (key != null && !key.isEmpty() && !key.equals("your-deepl-api-key-here")) {
            return key;
        }
        return null;
    }

    private void broadcastMessage(String message, NamedTextColor color) {
        Bukkit.broadcast(Component.text(message, color));
    }

    private void showHelp(CommandSender sender) {
        Component help = Component.text("=== ArabicTranslator Commands ===\n", NamedTextColor.GOLD)
            .append(Component.text("/arabic enable", NamedTextColor.YELLOW))
            .append(Component.text(" - Enable translation\n", NamedTextColor.GRAY))
            .append(Component.text("/arabic disable", NamedTextColor.YELLOW))
            .append(Component.text(" - Disable translation\n", NamedTextColor.GRAY))
            .append(Component.text("/arabic status", NamedTextColor.YELLOW))
            .append(Component.text(" - Show current status\n", NamedTextColor.GRAY))
            .append(Component.text("/arabic reload", NamedTextColor.YELLOW))
            .append(Component.text(" - Reload configuration\n", NamedTextColor.GRAY))
            .append(Component.text("/arabic help", NamedTextColor.YELLOW))
            .append(Component.text(" - Show this help", NamedTextColor.GRAY));
        sender.sendMessage(help);
    }
}
