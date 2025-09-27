package com.zetaplugins.netwatchz.spigot.util;

import com.zetaplugins.netwatchz.spigot.NetwatchZSpigot;
import org.bukkit.event.Listener;

public final class EventManager {
    private final NetwatchZSpigot plugin;

    public EventManager(NetwatchZSpigot plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all listeners
     */
    public void registerListeners() {
        //registerListener(new AsyncPlayerPreLoginListener(plugin));
    }

    /**
     * Registers a listener
     *
     * @param listener The listener to register
     */
    private void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}
