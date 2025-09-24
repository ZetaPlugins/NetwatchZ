package com.zetaplugins.netwatchz.paper.listeners;

import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.paper.NetwatchZPaper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.List;

public final class AsyncPlayerPreLoginListener implements Listener {
    private final NetwatchZPaper plugin;

    public AsyncPlayerPreLoginListener(NetwatchZPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        String playerIp = NetwatchZPaper.getIpFromInetAdress(event.getAddress());

        IpData ipData = plugin.getIpDataFetcher().fetchIpData(playerIp);

        if (ipData == null) {
            plugin.getLogger().warning("Failed to fetch IP data for player " + playerName + " with IP: " + playerIp);
            return;
        }

        //plugin.getLogger().info("Player " + playerName + " is attempting to log in from IP: " + playerIp);
        //plugin.getLogger().info("IP Data: " + ipData);

        boolean enableGeoBlocking = plugin.getConfig().getBoolean("geo_blocking.enabled", true);
        if (enableGeoBlocking && handleGeoBlocking(ipData, playerName, playerIp, event)) return;
    }

    /**
     * Handles geo-blocking based on the provided IP data and configuration.
     * @param ipData the IP data of the player
     * @param playerName the name of the player
     * @param playerIp the IP address of the player
     * @param event the AsyncPlayerPreLoginEvent
     * @return true if the player is blocked, false otherwise
     */
    private boolean handleGeoBlocking(IpData ipData, String playerName, String playerIp, AsyncPlayerPreLoginEvent event) {
        // If true -> blacklist, if false -> whitelist
        boolean blackList = plugin.getConfig().getBoolean("geo_blocking.blacklist", true);
        List<String> countryList = plugin.getConfig().getStringList("geo_blocking.countries");

        if (blackList && countryList.contains(ipData.countryCode()) || !blackList && !countryList.contains(ipData.countryCode())) {
            plugin.getLogger().warning("Player " + playerName + " with IP: " + playerIp + " was blocked due to country: " + ipData.countryCode());
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    plugin.getMessageService().getAndFormatMsg(
                            false,
                            "geoblock_ban_message",
                            "&cYour IP address has been blocked due to suspicious activity!<br><br>&7If you believe this is an error, please contact support."
                    )
            );
            return true;
        }

        return false;
    }
}
