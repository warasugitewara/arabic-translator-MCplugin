/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator

import com.warasugi.arabictranslator.config.PluginSettings
import com.warasugi.arabictranslator.romanize.ArabicRomanizer
import com.warasugi.arabictranslator.translate.TranslationCache
import com.warasugi.arabictranslator.translate.TranslationService
import com.warasugi.arabictranslator.translate.provider.DeepLProvider
import com.warasugi.arabictranslator.translate.provider.HttpSupport
import com.warasugi.arabictranslator.translate.provider.LibreTranslateProvider
import com.warasugi.arabictranslator.translate.provider.MyMemoryProvider
import com.warasugi.arabictranslator.translate.provider.TranslationProvider
import kotlinx.coroutines.CoroutineScope
import java.time.Duration
import java.util.logging.Logger

/**
 * Everything derived from one reading of `config.yml`.
 *
 * `/arabic reload` builds a brand new instance and closes the old one, so a reload
 * can never leave the plugin half-configured and cannot leak the HTTP client.
 */
class TranslatorRuntime private constructor(
    val settings: PluginSettings,
    val service: TranslationService,
    private val httpClient: java.net.http.HttpClient,
) : AutoCloseable {

    override fun close() {
        httpClient.close()
    }

    companion object {

        fun create(settings: PluginSettings, scope: CoroutineScope, logger: Logger): TranslatorRuntime {
            val client = HttpSupport.newClient(CONNECT_TIMEOUT)

            val available = buildMap<String, TranslationProvider> {
                put(
                    "deepl",
                    DeepLProvider(
                        apiKey = settings.deepLApiKey,
                        tier = settings.deepLTier,
                        endpointOverride = settings.deepLEndpoint,
                        client = client,
                        timeout = settings.requestTimeout,
                    ),
                )
                put(
                    "mymemory",
                    MyMemoryProvider(
                        enabled = settings.myMemoryEnabled,
                        email = settings.myMemoryEmail,
                        endpoint = settings.myMemoryEndpoint,
                        defaultSourceLanguage = settings.myMemorySourceLanguage,
                        client = client,
                        timeout = settings.requestTimeout,
                    ),
                )
                put(
                    "libretranslate",
                    LibreTranslateProvider(
                        enabled = settings.libreEnabled,
                        endpoint = settings.libreEndpoint,
                        apiKey = settings.libreApiKey,
                        client = client,
                        timeout = settings.requestTimeout,
                    ),
                )
            }

            val unknown = settings.providerOrder.filterNot(available::containsKey)
            if (unknown.isNotEmpty()) {
                logger.warning("Ignoring unknown provider(s) in providers.order: ${unknown.joinToString()}")
            }

            val service = TranslationService(
                providers = settings.providerOrder.mapNotNull(available::get),
                cache = TranslationCache(settings.cacheMaxEntries, settings.cacheTtl.toMillis()),
                romanizer = ArabicRomanizer(settings.romanizationStyle, settings.insertShortVowels)
                    .takeIf { settings.romanizationEnabled },
                targetLanguage = settings.targetLanguage,
                sourceLanguage = settings.sourceLanguage,
                scope = scope,
                logger = logger,
                debug = settings.debug,
            )

            return TranslatorRuntime(settings, service, client)
        }

        private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
