/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.listener

import com.warasugi.arabictranslator.ArabicTranslatorPlugin
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
 * Translates public chat and delivers the result to the players who want it.
 *
 * Listening at [EventPriority.MONITOR] with `ignoreCancelled = true` means the
 * plugin observes the chat other plugins have already decided to allow, rather
 * than translating messages that are about to be cancelled. Nothing blocks here:
 * the network call runs in a coroutine and the translated line follows a moment
 * later, so a slow API never holds up chat.
 */
class ChatTranslationListener(private val plugin: ArabicTranslatorPlugin) : Listener {

    private val lastMessageAt = ConcurrentHashMap<UUID, Long>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val runtime = plugin.runtime ?: return
        if (!plugin.translationEnabled) return

        val settings = runtime.settings
        val message = PLAIN.serialize(event.message()).trim()
        if (message.isEmpty() || message.length > settings.maxMessageLength) return
        if (settings.skipAlreadyTranslated && Scripts.isMostlyArabic(message)) return

        val player = event.player
        if (!withinRateLimit(player.uniqueId, settings.cooldown.toMillis())) return

        // Snapshot the audience while the event is still alive; it is not safe to
        // touch the live view once the event has been handed back to the server.
        val viewers = event.viewers().toList()
        val name = player.name

        plugin.scope.launch {
            val result = runtime.service.translate(message) ?: return@launch
            deliver(viewers, render(name, message, result), settings.receiveByDefault)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        lastMessageAt.remove(event.player.uniqueId)
    }

    private fun deliver(viewers: List<Audience>, message: Component, receiveByDefault: Boolean) {
        for (viewer in viewers) {
            if (viewer is Player && !plugin.preferences.receives(viewer.uniqueId, receiveByDefault)) continue
            viewer.sendMessage(message)
        }
    }

    private fun render(playerName: String, original: String, result: TranslationResult): Component {
        val settings = requireNotNull(plugin.runtime).settings
        val template = if (result.romanization.isNullOrBlank()) {
            settings.formatWithoutRomanization
        } else {
            settings.format
        }

        // `unparsed` placeholders: chat content can never be read back as MiniMessage tags.
        val resolver = TagResolver.resolver(
            Placeholder.unparsed("player", playerName),
            Placeholder.unparsed("message", original),
            Placeholder.unparsed("translation", result.text),
            Placeholder.unparsed("romanization", result.romanization.orEmpty()),
            Placeholder.unparsed("provider", result.provider),
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
