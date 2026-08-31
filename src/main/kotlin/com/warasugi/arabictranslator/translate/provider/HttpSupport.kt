/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.translate.provider

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Shared HTTP/JSON plumbing for the providers.
 *
 * Requests go out through [HttpClient.sendAsync] and are awaited as coroutines, so
 * a slow API parks a suspended coroutine instead of blocking a whole thread - the
 * previous implementation burned a pooled thread per chat message.
 */
internal object HttpSupport {

    const val USER_AGENT = "ArabicTranslator/2.0 (+https://github.com/warasugitewara/arabic-translator-MCplugin)"

    fun newClient(connectTimeout: Duration): HttpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    suspend fun HttpClient.fetch(request: HttpRequest): HttpResponse<String> =
        sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).await()

    fun jsonRequest(uri: String, body: String, timeout: Duration): HttpRequest.Builder =
        HttpRequest.newBuilder(URI.create(uri))
            .timeout(timeout)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))

    fun getRequest(uri: String, timeout: Duration): HttpRequest.Builder =
        HttpRequest.newBuilder(URI.create(uri))
            .timeout(timeout)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .GET()

    fun parseObject(body: String, provider: String): JsonObject = try {
        JsonParser.parseString(body).takeIf(JsonElement::isJsonObject)?.asJsonObject
            ?: throw TranslationException("$provider returned a non-object JSON response")
    } catch (e: JsonSyntaxException) {
        throw TranslationException("$provider returned malformed JSON", cause = e)
    }

    /** Truncates an API error body so a failing backend cannot flood the console. */
    fun summarise(body: String): String =
        body.replace(Regex("\\s+"), " ").trim().take(300)
}
