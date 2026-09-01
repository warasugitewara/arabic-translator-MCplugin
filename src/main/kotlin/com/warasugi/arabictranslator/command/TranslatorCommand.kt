/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import com.warasugi.arabictranslator.ArabicTranslatorPlugin
import com.warasugi.arabictranslator.language.LanguageProfile
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

/**
 * One command per configured language, built on Paper's Brigadier API.
 *
 * `/arabic` and `/chinese` behave exactly as the two separate plugins did, but
 * they are now the same code driven by a [LanguageProfile], so a language added to
 * `config.yml` gets its command for free. Brigadier replaces the hand-rolled
 * `TabCompleter` and the manual permission checks: every node declares its own
 * permission, so players are never offered a subcommand they cannot run, and
 * unknown input is rejected by the parser with the error underlined in place.
 */
class TranslatorCommand(
    private val plugin: ArabicTranslatorPlugin,
    private val language: LanguageProfile,
) {

    val aliases: List<String> get() = language.aliases

    val description: String get() = "Control real-time ${language.label} chat translation"

    fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal(language.id)
            .requires { it.sender.hasPermission("translator.help") }
            .executes(::showHelp)
            .then(literal("enable", "translator.enable", ::enable))
            .then(literal("disable", "translator.disable", ::disable))
            .then(literal("status", "translator.status", ::showStatus))
            .then(literal("toggle", "translator.toggle", ::toggleForPlayer))
            .then(literal("reload", "translator.reload", ::reload))
            .then(literal("help", "translator.help") { showHelp(it) })
            .build()

    // -- subcommands --------------------------------------------------------

    private fun enable(context: CommandContext<CommandSourceStack>) {
        val runtime = plugin.runtime
        if (runtime == null || !runtime.service.hasUsableProvider) {
            context.source.sender.sendMessage(
                error(
                    "No translation backend is usable. Set `deepl-api-key` in config.yml, " +
                        "or enable a keyless provider under `providers`, then reload.",
                ),
            )
            return
        }
        plugin.languageState.set(language.id, true)
        plugin.server.broadcast(
            success("${language.label} translation enabled (${runtime.service.providerIds.joinToString(" > ")})"),
        )
    }

    private fun disable(context: CommandContext<CommandSourceStack>) {
        plugin.languageState.set(language.id, false)
        plugin.server.broadcast(warning("${language.label} translation disabled"))
    }

    private fun showStatus(context: CommandContext<CommandSourceStack>) {
        val sender = context.source.sender
        val runtime = plugin.runtime
        if (runtime == null) {
            sender.sendMessage(error("The plugin failed to configure itself; see the server log"))
            return
        }

        val stats = runtime.service.cacheStats()
        sender.sendMessage(header("ArabicTranslator"))
        sender.sendMessage(
            field("Providers", runtime.service.providerIds.joinToString(" > ").ifEmpty { "none configured" }),
        )
        sender.sendMessage(
            field("Cache", "${stats.size}/${stats.capacity} entries, ${"%.0f".format(stats.hitRate * 100)}% hit rate"),
        )
        for (profile in runtime.settings.languages) {
            val state = if (plugin.languageState.isEnabled(profile.id)) "enabled" else "disabled"
            val mine = when {
                sender !is Player -> ""
                plugin.preferences.receives(sender.uniqueId, profile) -> ", shown to you"
                else -> ", hidden from you"
            }
            sender.sendMessage(field("${profile.label} (${profile.code})", "$state$mine"))
        }
    }

    private fun toggleForPlayer(context: CommandContext<CommandSourceStack>) {
        val sender = context.source.sender
        if (sender !is Player) {
            sender.sendMessage(error("Only a player can toggle their own translation display"))
            return
        }
        val receiving = plugin.preferences.toggle(sender.uniqueId, language)
        sender.sendMessage(
            if (receiving) {
                success("You will now see ${language.label} translations")
            } else {
                warning("${language.label} translations hidden for you")
            },
        )
    }

    private fun reload(context: CommandContext<CommandSourceStack>) {
        val sender = context.source.sender
        runCatching { plugin.reloadRuntime() }
            .onSuccess {
                sender.sendMessage(success("Configuration reloaded, caches cleared"))
                sender.sendMessage(
                    field("Languages", plugin.runtime?.settings?.languages.orEmpty().joinToString { it.label }),
                )
                if (plugin.commandsNeedRestart) {
                    sender.sendMessage(
                        note("Language commands change only on a server restart; the rest is live"),
                    )
                }
            }
            .onFailure { failure ->
                sender.sendMessage(error("Reload failed: ${failure.message}"))
                plugin.logger.warning("Reload failed: ${failure.message}")
            }
    }

    private fun showHelp(context: CommandContext<CommandSourceStack>): Int {
        val sender = context.source.sender
        sender.sendMessage(header("/${language.id} - ${language.label} translation"))
        HELP.forEach { (usage, description) ->
            sender.sendMessage(
                Component.text("  /${language.id} ", NamedTextColor.GRAY)
                    .append(Component.text(usage, NamedTextColor.YELLOW))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(description, NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.suggestCommand("/${language.id} $usage")),
            )
        }
        return Command.SINGLE_SUCCESS
    }

    // -- helpers ------------------------------------------------------------

    private fun literal(
        name: String,
        permission: String,
        action: (CommandContext<CommandSourceStack>) -> Unit,
    ): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(name)
            .requires { it.sender.hasPermission(permission) }
            .executes { context ->
                action(context)
                Command.SINGLE_SUCCESS
            }

    private companion object {

        val HELP = listOf(
            "enable" to "Turn this language on for the whole server",
            "disable" to "Turn this language off",
            "status" to "Show every language, providers and cache statistics",
            "toggle" to "Show or hide these translations for yourself",
            "reload" to "Re-read config.yml",
            "help" to "Show this help",
        )

        fun header(text: String): Component = Component.text(text, NamedTextColor.GOLD)
            .decoration(TextDecoration.BOLD, true)

        fun field(label: String, value: String): Component =
            Component.text("  $label: ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE))

        fun success(text: String): Component = Component.text("✓ $text", NamedTextColor.GREEN)

        fun warning(text: String): Component = Component.text("✗ $text", NamedTextColor.RED)

        fun error(text: String): Component = Component.text(text, NamedTextColor.RED)

        fun note(text: String): Component = Component.text("· $text", NamedTextColor.GRAY)
    }
}
