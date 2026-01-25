/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 * 
 * Licensed under the MIT License.
 */

package com.example.arabic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TranslationManager {
    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private final Map<String, TranslationResult> cache;
    private String deepLApiKey;
    private String deepLApiUrl;
    private static final int CACHE_SIZE = 500;

    public TranslationManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.deepLApiKey = configManager.getDeepLApiKey();
        this.httpClient = HttpClient.newHttpClient();
        this.cache = new HashMap<>();
        
        setDeepLApiUrlInternal(configManager.getDeepLApiVersion());
        
        plugin.getLogger().info("[ArabicTranslator] TranslationManager initialized");
        plugin.getLogger().info("[ArabicTranslator] DeepL API URL: " + deepLApiUrl);
        plugin.getLogger().info("[ArabicTranslator] DeepL Key set: " + (deepLApiKey != null && !deepLApiKey.isEmpty() && !deepLApiKey.equals("your-deepl-api-key-here")));
    }

    public void setDeepLApiKey(String apiKey) {
        this.deepLApiKey = apiKey;
        cache.clear();
    }

    public void setDeepLApiUrl(String version) {
        setDeepLApiUrlInternal(version);
        cache.clear();
    }

    private void setDeepLApiUrlInternal(String version) {
        if ("pro".equalsIgnoreCase(version)) {
            this.deepLApiUrl = "https://api.deepl.com/v1/translate";
        } else {
            this.deepLApiUrl = "https://api-free.deepl.com/v1/translate";
        }
    }

    public void translateAsync(String text, Consumer<TranslationResult> callback) {
        if (cache.containsKey(text)) {
            callback.accept(cache.get(text));
            return;
        }

        CompletableFuture.supplyAsync(() -> translate(text))
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    plugin.getLogger().warning("Translation error: " + exception.getMessage());
                    callback.accept(null);
                } else {
                    if (result != null) {
                        cacheTranslation(text, result);
                    }
                    callback.accept(result);
                }
            });
    }

    private TranslationResult translate(String text) {
        plugin.getLogger().info("[ArabicTranslator] Starting translation for: " + text);
        
        TranslationResult result = translateWithDeepL(text);
        
        if (result == null) {
            plugin.getLogger().warning("[ArabicTranslator] DeepL translation failed for: " + text);
        } else {
            plugin.getLogger().info("[ArabicTranslator] Translation successful: " + result.getArabic());
        }
        
        return result;
    }

    private TranslationResult translateWithDeepL(String text) {
        if (deepLApiKey == null || deepLApiKey.isEmpty() || deepLApiKey.equals("your-deepl-api-key-here")) {
            plugin.getLogger().warning("[ArabicTranslator] DeepL API Key is not configured");
            return null;
        }

        try {
            String jsonBody = "{\"text\":[\"" + escapeJson(text) + "\"],\"target_lang\":\"AR\"}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deepLApiUrl.replace("/v1/", "/v2/")))
                .timeout(java.time.Duration.ofSeconds(10))
                .header("Authorization", "DeepL-Auth-Key " + deepLApiKey)
                .header("Content-Type", "application/json")
                .header("User-Agent", "ArabicTranslator/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            plugin.getLogger().info("[ArabicTranslator] Sending request to DeepL API: " + request.uri());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            plugin.getLogger().info("[ArabicTranslator] DeepL API response code: " + response.statusCode());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("translations")) {
                    String arabicText = json.getAsJsonArray("translations")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
                    
                    plugin.getLogger().info("[ArabicTranslator] DeepL translation result: " + arabicText);
                    String romanization = arabicToRomanization(arabicText);
                    return new TranslationResult(arabicText, romanization);
                }
            } else {
                plugin.getLogger().warning("[ArabicTranslator] DeepL API error: " + response.statusCode() + " - " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            plugin.getLogger().warning("[ArabicTranslator] DeepL API exception: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String arabicToRomanization(String arabic) {
        StringBuilder romanized = new StringBuilder();
        
        for (char c : arabic.toCharArray()) {
            String romanChar = getArabicRomanization(c);
            romanized.append(romanChar);
        }
        
        return romanized.toString();
    }

    private String getArabicRomanization(char c) {
        return switch (c) {
            case 'ا' -> "a";
            case 'ب' -> "b";
            case 'ت' -> "t";
            case 'ث' -> "th";
            case 'ج' -> "j";
            case 'ح' -> "h";
            case 'خ' -> "kh";
            case 'د' -> "d";
            case 'ذ' -> "dh";
            case 'ر' -> "r";
            case 'ز' -> "z";
            case 'س' -> "s";
            case 'ش' -> "sh";
            case 'ص' -> "s";
            case 'ض' -> "d";
            case 'ط' -> "t";
            case 'ظ' -> "z";
            case 'ع' -> "'";
            case 'غ' -> "gh";
            case 'ف' -> "f";
            case 'ق' -> "q";
            case 'ك' -> "k";
            case 'ل' -> "l";
            case 'م' -> "m";
            case 'ن' -> "n";
            case 'ه' -> "h";
            case 'و' -> "w";
            case 'ي' -> "y";
            case 'ة' -> "a";
            case 'آ' -> "aa";
            case 'أ' -> "a";
            case 'إ' -> "i";
            case 'ى' -> "a";
            case 'ئ' -> "y";
            case 'ؤ' -> "w";
            case 'ً' -> "an";
            case 'ٌ' -> "un";
            case 'ٍ' -> "in";
            case 'َ' -> "a";
            case 'ُ' -> "u";
            case 'ِ' -> "i";
            case 'ّ' -> "";
            case ' ' -> " ";
            case '!' -> "!";
            case '?' -> "?";
            case '.' -> ".";
            case ',' -> ",";
            default -> "";
        };
    }

    private void cacheTranslation(String original, TranslationResult result) {
        if (cache.size() >= CACHE_SIZE) {
            cache.clear();
        }
        cache.put(original, result);
    }
}
