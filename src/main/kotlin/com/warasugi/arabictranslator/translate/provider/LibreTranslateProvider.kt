/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.translate.provider

import com.google.gson.JsonObject
import com.warasugi.arabictranslator.translate.provider.HttpSupport.fetch
import com.warasugi.arabictranslator.translate.provider.HttpSupport.jsonRequest
import com.warasugi.arabictranslator.translate.provider.HttpSupport.parseObject
import com.warasugi.arabictranslator.translate.provider.HttpSupport.summarise
import java.net.http.HttpClient
import java.time.Duration

/**
 * LibreTranslate - open source and self-hostable.
 *
 * Disabled by default because the public instance now asks for a key, but pointing
 * `providers.libretranslate.endpoint` at your own container gives unlimited free
 * translation with no data leaving your network.
 */
class LibreTranslateProvider(
    private val enabled: Boolean,
    private val endpoint: String,
    private val apiKey: String,
    private val client: HttpClient,
    private val timeout: Duration,
) : TranslationProvider {

    override val id: String = ID

    override val isConfigured: Boolean get() = enabled && endpoint.isNotBlank()

    override suspend fun translate(request: TranslationRequest): String {
        val payload = JsonObject().apply {
            addProperty("q", request.text)
            addProperty("source", request.sourceLanguage?.lowercase() ?: "auto")
            addProperty("target", languageCode(request.targetLanguage))
            addProperty("format", "text")
            if (apiKey.isNotBlank()) addProperty("api_key", apiKey)
        }

        val response = client.fetch(jsonRequest(endpoint, payload.toString(), timeout).build())
        val json = runCatching { parseObject(response.body(), "LibreTranslate") }.getOrNull()

        if (response.statusCode() != 200) {
            val detail = json?.get("error")?.asString ?: summarise(response.body())
            throw TranslationException(
                "LibreTranslate returned HTTP ${response.statusCode()}: $detail",
                fatal = response.statusCode() == 403,
            )
        }

        return json?.get("translatedText")?.asString
            ?: throw TranslationException("LibreTranslate response contained no translation")
    }

    private fun languageCode(language: String): String =
        language.trim().lowercase().substringBefore('-')

    private companion object {
        const val ID = "libretranslate"
    }
}
