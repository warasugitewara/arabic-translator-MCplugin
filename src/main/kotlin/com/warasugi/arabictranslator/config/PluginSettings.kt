/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.config

import com.warasugi.arabictranslator.romanize.RomanizationStyle
import com.warasugi.arabictranslator.translate.provider.DeepLProvider
import org.bukkit.configuration.file.FileConfiguration
import java.time.Duration

/**
 * An immutable snapshot of `config.yml`.
 *
 * Reading the file once into a value object keeps the hot chat path free of YAML
 * lookups and means a `/arabic reload` swaps a whole consistent configuration in
 * one go instead of mutating live objects half way through a translation.
 *
 * The DeepL keys stay at the top level under their 1.x names, so configs written
 * by the previous release keep working untouched.
 */
data class PluginSettings(
    val deepLApiKey: String,
    val deepLTier: DeepLProvider.Tier,
    val deepLEndpoint: String?,
    val myMemoryEnabled: Boolean,
    val myMemoryEmail: String,
    val myMemoryEndpoint: String,
    val myMemorySourceLanguage: String,
    val libreEnabled: Boolean,
    val libreEndpoint: String,
    val libreApiKey: String,
    val providerOrder: List<String>,
    val enabledOnStart: Boolean,
    val targetLanguage: String,
    val sourceLanguage: String?,
    val maxMessageLength: Int,
    val requestTimeout: Duration,
    val cooldown: Duration,
    val skipAlreadyTranslated: Boolean,
    val cacheMaxEntries: Int,
    val cacheTtl: Duration,
    val romanizationEnabled: Boolean,
    val romanizationStyle: RomanizationStyle,
    val insertShortVowels: Boolean,
    val receiveByDefault: Boolean,
    val format: String,
    val formatWithoutRomanization: String,
    val debug: Boolean,
) {

    companion object {
        const val DEFAULT_FORMAT =
            "<white><player></white><gray>: </gray><light_purple><bold><translation></bold></light_purple>" +
                "<gray> | </gray><yellow><italic><romanization></italic></yellow>"

        const val DEFAULT_FORMAT_PLAIN =
            "<white><player></white><gray>: </gray><light_purple><bold><translation></bold></light_purple>"

        fun from(config: FileConfiguration): PluginSettings = PluginSettings(
            // Legacy top-level keys - do not move, 1.x configs rely on them.
            deepLApiKey = config.getString("deepl-api-key").orEmpty().unquote(),
            deepLTier = DeepLProvider.Tier.fromString(config.getString("deepl-api-version", "auto")),
            deepLEndpoint = config.getString("providers.deepl.endpoint")?.trim()?.ifBlank { null },
            enabledOnStart = config.getBoolean("translation-enabled", false),

            myMemoryEnabled = config.getBoolean("providers.mymemory.enabled", true),
            myMemoryEmail = config.getString("providers.mymemory.email").orEmpty().unquote(),
            myMemoryEndpoint = config.getString(
                "providers.mymemory.endpoint",
                "https://api.mymemory.translated.net/get",
            )!!,
            myMemorySourceLanguage = config.getString("providers.mymemory.assumed-source-language", "en")!!,

            libreEnabled = config.getBoolean("providers.libretranslate.enabled", false),
            libreEndpoint = config.getString(
                "providers.libretranslate.endpoint",
                "http://localhost:5000/translate",
            )!!,
            libreApiKey = config.getString("providers.libretranslate.api-key").orEmpty().unquote(),

            providerOrder = config.getStringList("providers.order")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf("deepl", "mymemory", "libretranslate") },

            targetLanguage = config.getString("translation.target-language", "AR")!!.trim(),
            sourceLanguage = config.getString("translation.source-language", "auto")
                ?.trim()
                ?.takeUnless { it.isEmpty() || it.equals("auto", ignoreCase = true) },
            maxMessageLength = config.getInt("translation.max-length", 256).coerceIn(1, 2_000),
            requestTimeout = Duration.ofSeconds(
                config.getLong("translation.timeout-seconds", 8L).coerceIn(1L, 60L),
            ),
            cooldown = Duration.ofMillis(config.getLong("translation.cooldown-millis", 750L).coerceAtLeast(0L)),
            skipAlreadyTranslated = config.getBoolean("translation.skip-translated-messages", true),

            cacheMaxEntries = config.getInt("cache.max-entries", 1_000).coerceIn(0, 100_000),
            cacheTtl = Duration.ofMinutes(config.getLong("cache.expire-minutes", 60L).coerceIn(1L, 10_080L)),

            romanizationEnabled = config.getBoolean("display.romanization", true),
            romanizationStyle = RomanizationStyle.fromString(config.getString("display.romanization-style", "simple")),
            insertShortVowels = config.getBoolean("display.insert-short-vowels", true),
            receiveByDefault = config.getBoolean("display.players-receive-by-default", true),
            format = config.getString("display.format", DEFAULT_FORMAT)!!,
            formatWithoutRomanization = config.getString(
                "display.format-without-romanization",
                DEFAULT_FORMAT_PLAIN,
            )!!,

            debug = config.getBoolean("debug", false),
        )

        /**
         * Strips quotes a user wrapped around a value.
         *
         * `config.yml` tells people to quote the DeepL key because it contains a
         * colon; SnakeYAML already removes those, but keys pasted with extra quotes
         * (`"'abc:fx'"`) used to silently produce 403s.
         */
        private fun String.unquote(): String {
            var value = trim()
            while (value.length >= 2 &&
                ((value.startsWith('\'') && value.endsWith('\'')) || (value.startsWith('"') && value.endsWith('"')))
            ) {
                value = value.substring(1, value.length - 1).trim()
            }
            return value
        }
    }
}
