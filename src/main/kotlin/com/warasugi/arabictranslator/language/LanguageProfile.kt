/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 *
 * Licensed under the MIT License.
 */

package com.warasugi.arabictranslator.language

import com.warasugi.arabictranslator.romanize.ArabicRomanizer
import com.warasugi.arabictranslator.romanize.PinyinRomanizer
import com.warasugi.arabictranslator.romanize.RomanizationStyle
import com.warasugi.arabictranslator.romanize.Romanizer
import com.warasugi.arabictranslator.romanize.ToneStyle
import org.bukkit.configuration.ConfigurationSection

/**
 * One target language the plugin can translate chat into.
 *
 * Arabic and Chinese used to be two plugins repeating the same provider, cache and
 * chat-listener code with a different language code hard-wired into each. They are
 * now rows in `languages:` - which is also what makes a third language a config
 * edit rather than a fork.
 *
 * @param id           config key and command name, e.g. `arabic`
 * @param label        human-readable name used in command output
 * @param code         language code handed to the translation backend (`AR`, `ZH`)
 * @param aliases      extra command aliases, e.g. `ar`
 * @param romanizer    pronunciation aid, or `null` to show the translation alone
 * @param skipScript   Unicode script that means "already translated", or `null`
 * @param enabledByDefault  state before anyone runs enable/disable
 */
data class LanguageProfile(
    val id: String,
    val label: String,
    val code: String,
    val aliases: List<String>,
    val romanizer: Romanizer?,
    val skipScript: Character.UnicodeScript?,
    val format: String,
    val formatWithoutRomanization: String,
    val enabledByDefault: Boolean,
    val receiveByDefault: Boolean,
) {

    companion object {

        const val DEFAULT_FORMAT =
            "<white><player></white><gray>: </gray><light_purple><bold><translation></bold></light_purple>" +
                "<gray> | </gray><yellow><italic><romanization></italic></yellow>"

        const val DEFAULT_FORMAT_PLAIN =
            "<white><player></white><gray>: </gray><light_purple><bold><translation></bold></light_purple>"

        /**
         * Reads one entry of the `languages:` section.
         *
         * Returns `null` for a language switched off in the config, so disabled
         * languages cost nothing at runtime - not even a registered command.
         */
        fun from(id: String, section: ConfigurationSection, defaultEnabled: Boolean = false): LanguageProfile? {
            if (!section.getBoolean("enabled", false)) return null

            return LanguageProfile(
                id = id,
                label = section.getString("label", id.replaceFirstChar(Char::uppercase))!!,
                code = section.getString("code", "EN")!!.trim(),
                aliases = section.getStringList("aliases").map { it.trim() }.filter { it.isNotEmpty() },
                romanizer = romanizerFor(section),
                skipScript = scriptFor(section.getString("skip-script")),
                format = section.getString("format", DEFAULT_FORMAT)!!,
                formatWithoutRomanization = section.getString(
                    "format-without-romanization",
                    DEFAULT_FORMAT_PLAIN,
                )!!,
                enabledByDefault = section.getBoolean("translation-enabled", defaultEnabled),
                receiveByDefault = section.getBoolean("players-receive-by-default", true),
            )
        }

        /**
         * The Arabic profile a 1.x `config.yml` implies.
         *
         * Configs written before languages existed carry only the DeepL keys and
         * `translation-enabled`; this keeps those servers translating after the
         * upgrade until they copy the new `languages:` block in.
         */
        fun legacyArabic(enabled: Boolean): LanguageProfile = LanguageProfile(
            id = "arabic",
            label = "Arabic",
            code = "AR",
            aliases = listOf("ar"),
            romanizer = ArabicRomanizer(RomanizationStyle.SIMPLE, insertShortVowels = true),
            skipScript = Character.UnicodeScript.ARABIC,
            format = DEFAULT_FORMAT,
            formatWithoutRomanization = DEFAULT_FORMAT_PLAIN,
            enabledByDefault = enabled,
            receiveByDefault = true,
        )

        private fun romanizerFor(section: ConfigurationSection): Romanizer? =
            when (section.getString("romanization", "none")!!.trim().lowercase()) {
                "arabic" -> ArabicRomanizer(
                    style = RomanizationStyle.fromString(section.getString("romanization-style")),
                    insertShortVowels = section.getBoolean("insert-short-vowels", true),
                )

                "pinyin" -> PinyinRomanizer(ToneStyle.fromString(section.getString("tone-style")))
                else -> null
            }

        /**
         * `Character.UnicodeScript` name, e.g. `ARABIC` or `HAN`.
         *
         * An unknown or empty value simply disables the check rather than failing
         * the whole config load.
         */
        private fun scriptFor(name: String?): Character.UnicodeScript? {
            val trimmed = name?.trim().orEmpty()
            if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null
            return runCatching { Character.UnicodeScript.forName(trimmed) }.getOrNull()
        }
    }
}
