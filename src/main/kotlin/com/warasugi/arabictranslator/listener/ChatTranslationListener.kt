/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.listener

import com.warasugi.arabictranslator.ArabicTranslatorPlugin
import com.warasugi.arabictranslator.language.LanguageProfile
import com.warasugi.arabictranslator.translate.TranslationResult
import com.warasugi.arabictranslator.util.Scripts
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Translates public chat into every enabled language and delivers each result to
 * the players who asked for it.
 *
 * Listening at [EventPriority.MONITOR] with `ignoreCancelled = true` means the
 * plugin observes the chat other plugins have already decided to allow, rather
 * than translating messages that are about to be cancelled. Nothing blocks here:
 * each language's network call runs in its own coroutine and the translated lines
 * follow a moment later, so a slow API never holds up chat and two languages are
 * translated side by side rather than one after the other.
 */
class ChatTranslationListener(private val plugin: ArabicTranslatorPlugin) : Listener {

    private val lastMessageAt = ConcurrentHashMap<UUID, Long>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val runtime = plugin.runtime ?: return
        val active = runtime.settings.languages.filter { plugin.languageState.isEnabled(it.id) }
        if (active.isEmpty()) return

        val settings = runtime.settings
        val message = PLAIN.serialize(event.message()).trim()
        if (message.isEmpty() || message.length > settings.maxMessageLength) return

        val player = event.player
        if (!withinRateLimit(player.uniqueId, settings.cooldown.toMillis())) return

        // Snapshot the audience while the event is still alive; it is not safe to
        // touch the live view once the event has been handed back to the server.
        val viewers = event.viewers().toList()
        val name = player.name

        for (language in active) {
            if (settings.skipAlreadyTranslated && isAlreadyTranslated(message, language)) continue
            plugin.scope.launch {
                val result = runtime.service.translate(message, language) ?: return@launch
                deliver(viewers, language, render(name, message, language, result))
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        lastMessageAt.remove(event.player.uniqueId)
    }

    /** Skips a message that is already written in the language we would translate to. */
    private fun isAlreadyTranslated(message: String, language: LanguageProfile): Boolean {
        val script = language.skipScript ?: return false
        return Scripts.isMostly(message, script)
    }

    private fun deliver(viewers: List<Audience>, language: LanguageProfile, message: Component) {
        for (viewer in viewers) {
            if (viewer is Player && !plugin.preferences.receives(viewer.uniqueId, language)) continue
            viewer.sendMessage(message)
        }
    }

    private fun render(
        playerName: String,
        original: String,
        language: LanguageProfile,
        result: TranslationResult,
    ): Component {
        val template = if (result.romanization.isNullOrBlank()) {
            language.formatWithoutRomanization
        } else {
            language.format
        }

        // `unparsed` placeholders: chat content can never be read back as MiniMessage tags.
        val resolver = TagResolver.resolver(
            Placeholder.unparsed("player", playerName),
            Placeholder.unparsed("message", original),
            Placeholder.unparsed("translation", result.text),
            Placeholder.unparsed("romanization", result.romanization.orEmpty()),
            Placeholder.unparsed("provider", result.provider),
            Placeholder.unparsed("language", language.label),
        )
        return MiniMessage.miniMessage().deserialize(template, resolver)
    }

    /** Simple per-player throttle so one spammer cannot drain the API quota. */
    private fun withinRateLimit(uuid: UUID, cooldownMillis: Long): Boolean {
        if (cooldownMillis <= 0) return true
        val now = System.currentTimeMillis()
        val previous = lastMessageAt.put(uuid, now)
        if (previous != null && now - previous < cooldownMillis) {
            lastMessageAt[uuid] = previous
            return false
        }
        return true
    }

    private companion object {
        val PLAIN: PlainTextComponentSerializer = PlainTextComponentSerializer.plainText()
    }
}
