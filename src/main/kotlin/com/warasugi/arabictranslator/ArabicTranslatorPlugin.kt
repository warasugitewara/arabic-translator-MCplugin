/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator

import com.warasugi.arabictranslator.command.TranslatorCommand
import com.warasugi.arabictranslator.config.PlayerPreferences
import com.warasugi.arabictranslator.config.PluginSettings
import com.warasugi.arabictranslator.listener.ChatTranslationListener
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Level

/**
 * Entry point for ArabicTranslator.
 *
 * The plugin keeps two pieces of mutable state: [runtime], the object graph built
 * from `config.yml`, and [translationEnabled]. Everything else is immutable, which
 * is what makes it safe for chat events on Paper's async threads and command
 * handlers on the main thread to read the same objects.
 */
class ArabicTranslatorPlugin : JavaPlugin() {

    /** Scope for every outbound translation; cancelled wholesale on disable. */
    lateinit var scope: CoroutineScope
        private set

    /** Rebuilt by [reloadRuntime]; `null` only if the configuration could not be read. */
    @Volatile
    var runtime: TranslatorRuntime? = null
        private set

    @Volatile
    var translationEnabled: Boolean = false
        private set

    val preferences: PlayerPreferences by lazy { PlayerPreferences(this) }

    override fun onEnable() {
        saveDefaultConfig()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("ArabicTranslator"))

        reloadRuntime()
        translationEnabled = readStoredState(runtime?.settings?.enabledOnStart ?: false)

        server.pluginManager.registerEvents(ChatTranslationListener(this), this)
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                TranslatorCommand(this).build(),
                "Control real-time Arabic chat translation",
                listOf("ar"),
            )
        }

        val providers = runtime?.service?.providerIds.orEmpty()
        if (providers.isEmpty()) {
            logger.warning(
                "No translation provider is configured. Add your DeepL key to config.yml " +
                    "(or enable the keyless `mymemory` provider) and run /arabic reload.",
            )
        } else {
            logger.info("Ready - providers: ${providers.joinToString(" > ")}, translation ${if (translationEnabled) "enabled" else "disabled"}")
        }
    }

    override fun onDisable() {
        if (::scope.isInitialized) scope.cancel("Plugin disabled")
        runtime?.close()
        runtime = null
        preferences.save()
    }

    /** Re-reads `config.yml` and swaps in a fresh [TranslatorRuntime]. */
    fun reloadRuntime() {
        reloadConfig()
        val settings = PluginSettings.from(config)
        val replacement = TranslatorRuntime.create(settings, scope, logger)

        runtime?.close()
        runtime = replacement
        preferences.load()

        if (settings.debug) {
            val redacted = settings.copy(deepLApiKey = REDACTED, libreApiKey = REDACTED, myMemoryEmail = REDACTED)
            logger.info("Configuration loaded: $redacted")
        }
    }

    /**
     * Flips translation on or off and remembers the choice across restarts.
     *
     * The flag lives in `state.yml` rather than in `config.yml`: Bukkit's
     * `saveConfig()` rewrites the file from memory and would strip every comment
     * the user reads to configure the plugin. `translation-enabled` in config.yml
     * stays the first-run default.
     */
    fun setTranslationEnabled(enabled: Boolean) {
        translationEnabled = enabled

        val yaml = YamlConfiguration()
        yaml.options().setHeader(listOf("Written by /arabic enable and /arabic disable. Safe to delete."))
        yaml.set(STATE_KEY, enabled)
        try {
            dataFolder.mkdirs()
            yaml.save(stateFile)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Could not remember the translation state", e)
        }
    }

    private fun readStoredState(default: Boolean): Boolean = when {
        !stateFile.exists() -> default
        else -> YamlConfiguration.loadConfiguration(stateFile).getBoolean(STATE_KEY, default)
    }

    private val stateFile: File get() = File(dataFolder, "state.yml")

    private companion object {
        const val STATE_KEY = "translation-enabled"
        const val REDACTED = "***"
    }
}
