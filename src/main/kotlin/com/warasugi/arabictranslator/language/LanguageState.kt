/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.language

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Whether each language is currently switched on, remembered across restarts.
 *
 * The flags live in `state.yml` rather than in `config.yml`, because Bukkit's
 * `saveConfig()` rewrites the file from memory and would strip every comment the
 * user reads to configure the plugin. `translation-enabled` under each language in
 * config.yml stays the first-run default.
 */
class LanguageState(private val plugin: Plugin) {

    private val file = File(plugin.dataFolder, FILE_NAME)
    private val enabled = ConcurrentHashMap<String, Boolean>()

    fun load(profiles: Collection<LanguageProfile>) {
        val stored = if (file.exists()) YamlConfiguration.loadConfiguration(file) else null
        enabled.clear()
        profiles.forEach { profile ->
            enabled[profile.id] = stored?.get("$SECTION.${profile.id}") as? Boolean
                ?: profile.enabledByDefault
        }
    }

    fun isEnabled(languageId: String): Boolean = enabled[languageId] ?: false

    fun set(languageId: String, value: Boolean) {
        enabled[languageId] = value
        save()
    }

    private fun save() {
        val yaml = YamlConfiguration()
        yaml.options().setHeader(
            listOf("Written by the enable/disable commands. Safe to delete."),
        )
        enabled.forEach { (id, value) -> yaml.set("$SECTION.$id", value) }

        try {
            plugin.dataFolder.mkdirs()
            yaml.save(file)
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Could not remember which languages are enabled", e)
        }
    }

    private companion object {
        const val FILE_NAME = "state.yml"
        const val SECTION = "enabled"
    }
}
