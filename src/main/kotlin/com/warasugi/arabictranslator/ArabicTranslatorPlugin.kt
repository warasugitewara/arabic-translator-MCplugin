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
import com.warasugi.arabictranslator.language.LanguageState
import com.warasugi.arabictranslator.listener.ChatTranslationListener
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.plugin.java.JavaPlugin

/**
 * Entry point.
 *
 * One plugin translates chat into every language listed in `config.yml`; Arabic
 * and Chinese are two entries there rather than two plugins with the same code and
 * a different language code compiled in.
 *
 * The plugin keeps two pieces of mutable state: [runtime], the object graph built
 * from `config.yml`, and [languageState]. Everything else is immutable, which is
 * what makes it safe for chat events on Paper's async threads and command handlers
 * on the main thread to read the same objects.
 */
class ArabicTranslatorPlugin : JavaPlugin() {

    /** Scope for every outbound translation; cancelled wholesale on disable. */
    lateinit var scope: CoroutineScope
        private set

    /** Rebuilt by [reloadRuntime]; `null` only if the configuration could not be read. */
    @Volatile
    var runtime: TranslatorRuntime? = null
        private set

    val languageState: LanguageState by lazy { LanguageState(this) }

    val preferences: PlayerPreferences by lazy { PlayerPreferences(this) }

    /**
     * `true` when a reload changed which languages exist.
     *
     * Brigadier commands are registered once during the server's command lifecycle
     * phase, so a language added or removed at runtime keeps its old command set
     * until a restart. Everything else a reload changes takes effect immediately.
     */
    @Volatile
    var commandsNeedRestart: Boolean = false
        private set

    private var registeredLanguageIds: List<String> = emptyList()

    override fun onEnable() {
        saveDefaultConfig()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("ArabicTranslator"))

        reloadRuntime()
        val languages = runtime?.settings?.languages.orEmpty()
        registeredLanguageIds = languages.map { it.id }

        server.pluginManager.registerEvents(ChatTranslationListener(this), this)
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val registrar = event.registrar()
            languages.forEach { language ->
                val command = TranslatorCommand(this, language)
                registrar.register(command.build(), command.description, command.aliases)
            }
        }

        if (languages.isEmpty()) {
            logger.warning("No language is enabled in config.yml; nothing will be translated.")
            return
        }

        val providers = runtime?.service?.providerIds.orEmpty()
        if (providers.isEmpty()) {
            logger.warning(
                "No translation provider is configured. Add your DeepL key to config.yml " +
                    "(or enable the keyless `mymemory` provider) and reload.",
            )
        } else {
            logger.info("Ready - providers: ${providers.joinToString(" > ")}")
        }
        languages.forEach {
            logger.info("Language ${it.id} (${it.code}): ${if (languageState.isEnabled(it.id)) "enabled" else "disabled"}")
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
        languageState.load(settings.languages)
        preferences.load(settings.languages)

        if (registeredLanguageIds.isNotEmpty() && registeredLanguageIds != settings.languages.map { it.id }) {
            commandsNeedRestart = true
        }

        if (settings.debug) {
            val redacted = settings.copy(deepLApiKey = REDACTED, libreApiKey = REDACTED, myMemoryEmail = REDACTED)
            logger.info("Configuration loaded: $redacted")
        }
    }

    private companion object {
        const val REDACTED = "***"
    }
}
