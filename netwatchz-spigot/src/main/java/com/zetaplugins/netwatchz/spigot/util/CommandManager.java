package com.zetaplugins.netwatchz.spigot.util;

import com.zetaplugins.netwatchz.spigot.commands.IpInfoCommand;
import com.zetaplugins.netwatchz.spigot.NetwatchZSpigot;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

public final class CommandManager {
    private final NetwatchZSpigot plugin;

    public CommandManager(NetwatchZSpigot plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all commands
     */
    public void registerCommands() {
        var ipInfoCommand = new IpInfoCommand(plugin);
        registerCommand("ipinfo", ipInfoCommand, ipInfoCommand);
    }

    /**
     * Registers a command
     *
     * @param name The name of the command
     * @param executor The executor of the command
     * @param tabCompleter The tab completer of the command
     */
    private void registerCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = plugin.getCommand(name);

        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(tabCompleter);
        }
    }
}
