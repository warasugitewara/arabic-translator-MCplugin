/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Remembers who wants to see translations, persisted in `players.yml`.
 *
 * Server-wide translation used to be all-or-nothing; players who read the source
 * language got every line twice with no way out. Only the players who differ from
 * the configured default are stored, so the file stays tiny.
 */
class PlayerPreferences(private val plugin: Plugin) {

    private val file = File(plugin.dataFolder, FILE_NAME)
    private val overrides = ConcurrentHashMap<UUID, Boolean>()

    fun load() {
        overrides.clear()
        if (!file.exists()) return

        val yaml = YamlConfiguration.loadConfiguration(file)
        yaml.getStringList(RECEIVING).mapNotNull(::parseUuid).forEach { overrides[it] = true }
        yaml.getStringList(NOT_RECEIVING).mapNotNull(::parseUuid).forEach { overrides[it] = false }
    }

    /** @param default the `display.players-receive-by-default` setting currently in force */
    fun receives(uuid: UUID, default: Boolean): Boolean = overrides[uuid] ?: default

    /** Flips [uuid]'s choice and returns the new state. */
    fun toggle(uuid: UUID, default: Boolean): Boolean {
        val next = !receives(uuid, default)
        if (next == default) overrides.remove(uuid) else overrides[uuid] = next
        save()
        return next
    }

    fun save() {
        val yaml = YamlConfiguration()
        yaml.options().setHeader(
            listOf(
                "Per-player translation display choices, written by /arabic toggle.",
                "Players missing from both lists follow display.players-receive-by-default.",
            ),
        )
        yaml.set(RECEIVING, overrides.filterValues { it }.keys.map(UUID::toString))
        yaml.set(NOT_RECEIVING, overrides.filterValues { !it }.keys.map(UUID::toString))

        try {
            plugin.dataFolder.mkdirs()
            yaml.save(file)
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Could not save $FILE_NAME", e)
        }
    }

    private fun parseUuid(raw: String): UUID? = runCatching { UUID.fromString(raw) }.getOrNull()

    private companion object {
        const val FILE_NAME = "players.yml"
        const val RECEIVING = "receiving"
        const val NOT_RECEIVING = "not-receiving"
    }
}
