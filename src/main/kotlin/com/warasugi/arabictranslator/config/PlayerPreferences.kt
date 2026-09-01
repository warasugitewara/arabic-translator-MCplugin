/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.config

import com.warasugi.arabictranslator.language.LanguageProfile
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Remembers who wants to see which language, persisted in `players.yml`.
 *
 * Server-wide translation used to be all-or-nothing; a player who reads the source
 * language got every line twice with no way out, and with two languages enabled,
 * three times. Only the players who differ from a language's configured default
 * are stored, so the file stays tiny.
 */
class PlayerPreferences(private val plugin: Plugin) {

    private data class Key(val uuid: UUID, val languageId: String)

    private val overrides = ConcurrentHashMap<Key, Boolean>()

    fun load(languages: Collection<LanguageProfile>) {
        overrides.clear()
        if (!file.exists()) return

        val yaml = YamlConfiguration.loadConfiguration(file)
        languages.forEach { language ->
            yaml.getStringList("${language.id}.$RECEIVING")
                .mapNotNull(::parseUuid)
                .forEach { overrides[Key(it, language.id)] = true }
            yaml.getStringList("${language.id}.$NOT_RECEIVING")
                .mapNotNull(::parseUuid)
                .forEach { overrides[Key(it, language.id)] = false }
        }
    }

    fun receives(uuid: UUID, language: LanguageProfile): Boolean =
        overrides[Key(uuid, language.id)] ?: language.receiveByDefault

    /** Flips [uuid]'s choice for one language and returns the new state. */
    fun toggle(uuid: UUID, language: LanguageProfile): Boolean {
        val key = Key(uuid, language.id)
        val next = !receives(uuid, language)
        if (next == language.receiveByDefault) overrides.remove(key) else overrides[key] = next
        save()
        return next
    }

    fun save() {
        val yaml = YamlConfiguration()
        yaml.options().setHeader(
            listOf(
                "Per-player translation display choices, written by the toggle command.",
                "Players missing from both lists follow players-receive-by-default.",
            ),
        )
        overrides.entries.groupBy { it.key.languageId }.forEach { (languageId, entries) ->
            yaml.set(
                "$languageId.$RECEIVING",
                entries.filter { it.value }.map { it.key.uuid.toString() },
            )
            yaml.set(
                "$languageId.$NOT_RECEIVING",
                entries.filterNot { it.value }.map { it.key.uuid.toString() },
            )
        }

        try {
            plugin.dataFolder.mkdirs()
            yaml.save(file)
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Could not save $FILE_NAME", e)
        }
    }

    private val file: File get() = File(plugin.dataFolder, FILE_NAME)

    private fun parseUuid(raw: String): UUID? = runCatching { UUID.fromString(raw) }.getOrNull()

    private companion object {
        const val FILE_NAME = "players.yml"
        const val RECEIVING = "receiving"
        const val NOT_RECEIVING = "not-receiving"
    }
}
