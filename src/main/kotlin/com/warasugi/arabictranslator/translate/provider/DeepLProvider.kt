/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.translate.provider

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.warasugi.arabictranslator.translate.provider.HttpSupport.fetch
import com.warasugi.arabictranslator.translate.provider.HttpSupport.jsonRequest
import com.warasugi.arabictranslator.translate.provider.HttpSupport.parseObject
import com.warasugi.arabictranslator.translate.provider.HttpSupport.summarise
import java.net.http.HttpClient
import java.time.Duration

/**
 * DeepL v2 - the highest quality backend and the plugin's default.
 *
 * Free keys end in `:fx` and must talk to `api-free.deepl.com`; [Tier.AUTO] picks
 * the right host from the key itself, which removes the single most common
 * misconfiguration (a free key pointed at the pro endpoint returning 403).
 */
class DeepLProvider(
    private val apiKey: String,
    tier: Tier,
    endpointOverride: String?,
    private val client: HttpClient,
    private val timeout: Duration,
) : TranslationProvider {

    enum class Tier {
        AUTO,
        FREE,
        PRO,
        ;

        companion object {
            fun fromString(value: String?): Tier =
                entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: AUTO
        }
    }

    val endpoint: String = when {
        !endpointOverride.isNullOrBlank() -> endpointOverride.trim()
        tier == Tier.PRO -> PRO_ENDPOINT
        tier == Tier.FREE -> FREE_ENDPOINT
        apiKey.endsWith(FREE_KEY_SUFFIX) -> FREE_ENDPOINT
        else -> PRO_ENDPOINT
    }

    override val id: String = ID

    override val isConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey !in PLACEHOLDER_KEYS

    override suspend fun translate(request: TranslationRequest): String {
        val payload = JsonObject().apply {
            add("text", JsonArray().apply { add(request.text) })
            addProperty("target_lang", languageCode(request.targetLanguage))
            request.sourceLanguage?.let { addProperty("source_lang", it.uppercase()) }
            // Chat is one short line; asking DeepL not to split keeps it one entry.
            addProperty("split_sentences", "0")
        }

        val response = client.fetch(
            jsonRequest(endpoint, payload.toString(), timeout)
                .header("Authorization", "DeepL-Auth-Key $apiKey")
                .build(),
        )

        when (val status = response.statusCode()) {
            200 -> Unit
            403 -> throw TranslationException(
                "DeepL rejected the API key (403). Check `deepl-api-key` and that the " +
                    "key matches the endpoint in use ($endpoint).",
                fatal = true,
            )

            429 -> throw TranslationException("DeepL rate limit hit (429), backing off")
            456 -> throw TranslationException("DeepL quota for this billing period is used up (456)", fatal = true)
            else -> throw TranslationException("DeepL returned HTTP $status: ${summarise(response.body())}")
        }

        val translations = parseObject(response.body(), "DeepL").getAsJsonArray("translations")
        if (translations == null || translations.isEmpty) {
            throw TranslationException("DeepL response contained no translations")
        }
        return translations[0].asJsonObject.get("text")?.asString
            ?: throw TranslationException("DeepL response contained no text")
    }

    /** DeepL wants uppercase codes and, for Chinese, the script-qualified variants. */
    private fun languageCode(language: String): String = when (val code = language.trim().uppercase()) {
        "ZH", "ZH-CN", "ZH_CN", "ZH-HANS" -> "ZH-HANS"
        "ZH-TW", "ZH_TW", "ZH-HANT" -> "ZH-HANT"
        else -> code
    }

    private companion object {
        const val ID = "deepl"
        const val FREE_ENDPOINT = "https://api-free.deepl.com/v2/translate"
        const val PRO_ENDPOINT = "https://api.deepl.com/v2/translate"
        const val FREE_KEY_SUFFIX = ":fx"

        val PLACEHOLDER_KEYS = setOf(
            "your-deepl-api-key-here",
            "your-api-key-here",
            "your-api-key-here:fx",
        )
    }
}
