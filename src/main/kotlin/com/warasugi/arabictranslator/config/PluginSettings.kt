/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.config

import com.warasugi.arabictranslator.language.LanguageProfile
import com.warasugi.arabictranslator.translate.provider.DeepLProvider
import org.bukkit.configuration.file.FileConfiguration
import java.time.Duration

/**
 * An immutable snapshot of `config.yml`.
 *
 * Reading the file once into a value object keeps the hot chat path free of YAML
 * lookups and means a reload swaps a whole consistent configuration in one go
 * instead of mutating live objects half way through a translation.
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
    val sourceLanguage: String?,
    val maxMessageLength: Int,
    val requestTimeout: Duration,
    val cooldown: Duration,
    val skipAlreadyTranslated: Boolean,
    val cacheMaxEntries: Int,
    val cacheTtl: Duration,
    val languages: List<LanguageProfile>,
    val debug: Boolean,
) {

    companion object {

        fun from(config: FileConfiguration): PluginSettings = PluginSettings(
            // Legacy top-level keys - do not move, 1.x configs rely on them.
            deepLApiKey = config.getString("deepl-api-key").orEmpty().unquote(),
            deepLTier = DeepLProvider.Tier.fromString(config.getString("deepl-api-version", "auto")),
            deepLEndpoint = config.getString("providers.deepl.endpoint")?.trim()?.ifBlank { null },

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

            languages = readLanguages(config),

            debug = config.getBoolean("debug", false),
        )

        /**
         * Reads every entry under `languages:`, skipping the ones switched off.
         *
         * A 1.x config has no `languages:` section at all, so Arabic is synthesised
         * from the old top-level keys and such a server keeps translating after the
         * upgrade without anyone editing anything.
         */
        private fun readLanguages(config: FileConfiguration): List<LanguageProfile> {
            val legacyEnabled = config.getBoolean("translation-enabled", false)
            val section = config.getConfigurationSection("languages")
                ?: return listOf(LanguageProfile.legacyArabic(legacyEnabled))

            return section.getKeys(false).mapNotNull { id ->
                section.getConfigurationSection(id)?.let { entry ->
                    // The 1.x top-level flag is the startup default for Arabic.
                    LanguageProfile.from(id, entry, defaultEnabled = legacyEnabled && id == ARABIC_ID)
                }
            }
        }

        private const val ARABIC_ID = "arabic"

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
