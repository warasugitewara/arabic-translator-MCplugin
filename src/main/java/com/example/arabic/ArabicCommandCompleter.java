/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 * 
 * Licensed under the MIT License.
 */

package com.example.arabic;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ArabicCommandCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            
            List<String> commands = List.of("enable", "disable", "status", "reload", "help");
            for (String cmd : commands) {
                if (cmd.startsWith(prefix)) {
                    completions.add(cmd);
                }
            }
        }

        return completions;
    }
}
