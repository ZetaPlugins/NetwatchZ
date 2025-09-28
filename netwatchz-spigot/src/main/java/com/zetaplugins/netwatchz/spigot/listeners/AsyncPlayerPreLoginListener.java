package com.zetaplugins.netwatchz.spigot.listeners;

import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.vpnblock.VpnInfoData;
import com.zetaplugins.netwatchz.spigot.NetwatchZSpigot;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.List;
import java.util.UUID;

public final class AsyncPlayerPreLoginListener implements Listener {
    private final NetwatchZSpigot plugin;

    public AsyncPlayerPreLoginListener(NetwatchZSpigot plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        String playerIp = NetwatchZSpigot.getIpFromInetAdress(event.getAddress());

        boolean bypassOp = plugin.getConfig().getBoolean("always_allow_ops", true);
        if (bypassOp) {
            UUID playerUuid = event.getUniqueId();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            if (offlinePlayer.isOp()) return;
        }

        boolean ipListEnabled = plugin.getConfig().getBoolean("ip_list.enabled", true);
        if (ipListEnabled && handleIpListBlocking(playerName, playerIp, event)) return;

        boolean enableGeoBlocking = plugin.getConfig().getBoolean("geo_blocking.enabled", true);
        if (enableGeoBlocking) {
            IpData ipData = plugin.getIpDataFetcher().fetchIpData(playerIp);
            if (ipData == null) {
                plugin.getLogger().warning("Failed to fetch IP data for player " + playerName + " with IP: " + playerIp);
                return;
            }
            if (handleGeoBlocking(ipData, playerName, playerIp, event)) return;
        }

        boolean enableVpnBlocking = plugin.getConfig().getBoolean("vpn_block.enabled", true);
        if (enableVpnBlocking) {
            VpnInfoData vpnInfoData = plugin.getVpnInfoProvider().fetchVpnData(playerIp);
            if (vpnInfoData == null) {
                plugin.getLogger().warning("Failed to fetch VPN info for player " + playerName + " with IP: " + playerIp);
                return;
            }
            if (handleVpnBlocking(vpnInfoData, playerName, playerIp, event)) return;
        }
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
            plugin.getLogger().info("Player " + playerName + " with IP: " + playerIp + " was blocked due to country: " + ipData.countryCode());
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

    /**
     * Handles IP list blocking based on the provided configuration.
     * @param playerName the name of the player
     * @param playerIp the IP address of the player
     * @param event the AsyncPlayerPreLoginEvent
     * @return true if the player is blocked, false otherwise
     */
    private boolean handleIpListBlocking(String playerName, String playerIp, AsyncPlayerPreLoginEvent event) {
        String ipListMode = plugin.getConfig().getString("ip_list.mode", "blacklist").toLowerCase();

        if (ipListMode.equals("blacklist") || ipListMode.equals("whitelist")) {
            boolean isInList = plugin.getIpListService().isIpInAnyList(playerIp);
            if (ipListMode.equals("blacklist") && isInList) {
                plugin.getLogger().info("Player " + playerName + " with IP: " + playerIp + " was blocked (blacklist).");
                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        getFormattedIpListBanMessage()
                );
                return true;
            } else if (ipListMode.equals("whitelist") && !isInList) {
                plugin.getLogger().info("Player " + playerName + " with IP: " + playerIp + " was blocked (not in whitelist).");
                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        getFormattedIpListBanMessage()
                );
                return true;
            }
        }

        return false;
    }

    private String getFormattedIpListBanMessage() {
        return plugin.getMessageService().getAndFormatMsg(
                false,
                "iplist_ban_message",
                "&cYour IP address has been blocked due to suspicious activity!<br><br>&7If you believe this is an error, please contact support."
        );
    }

    private boolean handleVpnBlocking(VpnInfoData ipData, String playerName, String playerIp, AsyncPlayerPreLoginEvent event) {
        String type = null;
        if (ipData.isVpn()) type = "is_vpn";
        else if (ipData.isProxy()) type = "is_proxy";
        else if (ipData.isTor()) type = "is_tor";
        else if (ipData.isRelay()) type = "is_relay";
        else if (ipData.isHosting()) type = "is_hosting";

        if (type == null) return false;

        List<String> commands = plugin.getConfig().getStringList("vpn_block." + type + ".commands");
        for (String command : commands) {
            String parsedCommand = command.replace("%player%", playerName).replace("%ip%", playerIp);

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
            });
        }

        boolean shouldBlock = plugin.getConfig().getBoolean("vpn_block." + type + ".block", true);
        if (shouldBlock) {
            plugin.getLogger().info("Player " + playerName + " with IP: " + playerIp + " was blocked due to VPN/Proxy/Tor/Relay/Hosting usage.");
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    plugin.getMessageService().getAndFormatMsg(
                            false,
                            "vpnblock_ban_message",
                            "&cYour IP address has been blocked because it is associated with a VPN service!<br>Try disabling your VPN and reconnecting.<br><br>&7If you believe this is an error, please contact support."
                    )
            );
            return true;
        }

        return false;
    }
}
