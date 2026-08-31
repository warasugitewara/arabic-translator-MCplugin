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
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

/**
 * The `/arabic` command tree, built on Paper's Brigadier API.
 *
 * Brigadier replaces the hand-rolled `TabCompleter` and the manual permission
 * checks of the 1.x command: every node declares its own permission, so players
 * are never offered a subcommand they cannot run, and unknown input is rejected by
 * the parser with the error underlined in place.
 */
class TranslatorCommand(private val plugin: ArabicTranslatorPlugin) {

    fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal(NAME)
            .requires { it.sender.hasPermission("arabic.help") }
            .executes(::showHelp)
            .then(
                literal("enable", "arabic.enable") { context ->
                    val runtime = plugin.runtime
                    if (runtime == null || !runtime.service.hasUsableProvider) {
                        context.source.sender.sendMessage(
                            error(
                                "No translation backend is usable. Set `deepl-api-key` in config.yml, " +
                                    "or enable a keyless provider under `providers`, then run /arabic reload.",
                            ),
                        )
                        return@literal
                    }
                    plugin.setTranslationEnabled(true)
                    plugin.server.broadcast(
                        success("Arabic translation enabled (${runtime.service.providerIds.joinToString(" > ")})"),
                    )
                },
            )
            .then(
                literal("disable", "arabic.disable") {
                    plugin.setTranslationEnabled(false)
                    plugin.server.broadcast(warning("Arabic translation disabled"))
                },
            )
            .then(literal("status", "arabic.status", ::showStatus))
            .then(literal("toggle", "arabic.toggle", ::toggleForPlayer))
            .then(
                literal("reload", "arabic.reload") { context ->
                    val sender = context.source.sender
                    runCatching { plugin.reloadRuntime() }
                        .onSuccess {
                            sender.sendMessage(success("Configuration reloaded, caches cleared"))
                        }
                        .onFailure { failure ->
                            sender.sendMessage(error("Reload failed: ${failure.message}"))
                            plugin.logger.warning("Reload failed: ${failure.message}")
                        }
                },
            )
            .then(literal("help", "arabic.help") { showHelp(it) })
            .build()

    // -- subcommands --------------------------------------------------------

    private fun showStatus(context: CommandContext<CommandSourceStack>) {
        val sender = context.source.sender
        val runtime = plugin.runtime
        if (runtime == null) {
            sender.sendMessage(error("The plugin failed to configure itself; see the server log"))
            return
        }

        val stats = runtime.service.cacheStats()
        sender.sendMessage(header("ArabicTranslator"))
        sender.sendMessage(field("Translation", if (plugin.translationEnabled) "enabled" else "disabled"))
        sender.sendMessage(field("Target language", runtime.settings.targetLanguage))
        sender.sendMessage(
            field("Providers", runtime.service.providerIds.joinToString(" > ").ifEmpty { "none configured" }),
        )
        sender.sendMessage(
            field("Cache", "${stats.size}/${stats.capacity} entries, ${"%.0f".format(stats.hitRate * 100)}% hit rate"),
        )
        if (sender is Player) {
            val receiving = plugin.preferences.receives(sender.uniqueId, runtime.settings.receiveByDefault)
            sender.sendMessage(field("You receive translations", if (receiving) "yes" else "no"))
        }
    }

    private fun toggleForPlayer(context: CommandContext<CommandSourceStack>) {
        val sender = context.source.sender
        if (sender !is Player) {
            sender.sendMessage(error("Only a player can toggle their own translation display"))
            return
        }
        val default = plugin.runtime?.settings?.receiveByDefault ?: true
        val receiving = plugin.preferences.toggle(sender.uniqueId, default)
        sender.sendMessage(
            if (receiving) success("You will now see translated chat") else warning("Translated chat hidden for you"),
        )
    }

    private fun showHelp(context: CommandContext<CommandSourceStack>): Int {
        val sender = context.source.sender
        sender.sendMessage(header("ArabicTranslator commands"))
        HELP.forEach { (usage, description) ->
            sender.sendMessage(
                Component.text("  /$NAME ", NamedTextColor.GRAY)
                    .append(Component.text(usage, NamedTextColor.YELLOW))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(description, NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.suggestCommand("/$NAME $usage")),
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
        const val NAME = "arabic"

        val HELP = listOf(
            "enable" to "Turn translation on for the whole server",
            "disable" to "Turn translation off",
            "status" to "Show state, providers and cache statistics",
            "toggle" to "Show or hide translated chat for yourself",
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
    }
}
