package com.zetaplugins.netwatchz.paper.util;

import com.zetaplugins.netwatchz.paper.listeners.*;
import com.zetaplugins.netwatchz.paper.NetwatchZPaper;
import org.bukkit.event.Listener;

public final class EventManager {
    private final NetwatchZPaper plugin;

    public EventManager(NetwatchZPaper plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all listeners
     */
    public void registerListeners() {
        registerListener(new AsyncPlayerPreLoginListener(plugin));
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
