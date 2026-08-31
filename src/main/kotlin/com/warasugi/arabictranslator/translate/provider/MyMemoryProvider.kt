/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.translate.provider

import com.warasugi.arabictranslator.translate.provider.HttpSupport.fetch
import com.warasugi.arabictranslator.translate.provider.HttpSupport.getRequest
import com.warasugi.arabictranslator.translate.provider.HttpSupport.parseObject
import com.warasugi.arabictranslator.translate.provider.HttpSupport.summarise
import java.net.URLEncoder
import java.net.http.HttpClient
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * MyMemory - a keyless free fallback, so the plugin still works before anyone has
 * signed up for DeepL and keeps working once a DeepL quota runs out.
 *
 * The anonymous tier allows roughly 5.000 characters a day; supplying [email]
 * raises that to about 50.000. It cannot auto-detect the source language, so
 * [defaultSourceLanguage] is used when the config asks for `auto`.
 */
class MyMemoryProvider(
    private val enabled: Boolean,
    private val email: String,
    private val endpoint: String,
    private val defaultSourceLanguage: String,
    private val client: HttpClient,
    private val timeout: Duration,
) : TranslationProvider {

    override val id: String = ID

    override val isConfigured: Boolean get() = enabled && endpoint.isNotBlank()

    override suspend fun translate(request: TranslationRequest): String {
        val source = (request.sourceLanguage ?: defaultSourceLanguage).lowercase()
        val target = languageCode(request.targetLanguage)
        val url = buildString {
            append(endpoint)
            append(if (endpoint.contains('?')) '&' else '?')
            append("q=").append(encode(request.text))
            append("&langpair=").append(encode("$source|$target"))
            if (email.isNotBlank()) append("&de=").append(encode(email))
        }

        val response = client.fetch(getRequest(url, timeout).build())
        if (response.statusCode() != 200) {
            throw TranslationException("MyMemory returned HTTP ${response.statusCode()}: ${summarise(response.body())}")
        }

        val json = parseObject(response.body(), "MyMemory")
        val status = json.get("responseStatus")?.takeIf { it.isJsonPrimitive }?.asString
        if (status != null && status != "200") {
            val detail = json.get("responseDetails")?.asString.orEmpty()
            throw TranslationException("MyMemory error $status: ${summarise(detail)}", fatal = status == "403")
        }

        val text = json.getAsJsonObject("responseData")?.get("translatedText")?.asString
            ?: throw TranslationException("MyMemory response contained no translation")

        // The daily-limit notice arrives with HTTP 200 in the translation field itself.
        if (QUOTA_WARNING in text.uppercase()) {
            throw TranslationException("MyMemory daily quota is used up", fatal = true)
        }
        return text
    }

    /** MyMemory expects lowercase ISO 639-1, with a region for Chinese. */
    private fun languageCode(language: String): String = when (val code = language.trim().lowercase()) {
        "zh", "zh-hans", "zh_cn" -> "zh-CN"
        "zh-hant", "zh_tw" -> "zh-TW"
        else -> code.substringBefore('-')
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private companion object {
        const val ID = "mymemory"
        const val QUOTA_WARNING = "MYMEMORY WARNING"
    }
}
