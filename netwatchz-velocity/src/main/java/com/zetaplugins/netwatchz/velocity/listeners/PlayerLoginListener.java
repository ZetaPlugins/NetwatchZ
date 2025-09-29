package com.zetaplugins.netwatchz.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zetaplugins.netwatchz.common.NetwatchzServices;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.vpnblock.VpnInfoData;
import com.zetaplugins.netwatchz.velocity.util.VelocityConfigManager;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;

public class PlayerLoginListener {

    private final ProxyServer server;
    private final NetwatchzServices services;
    private final Logger logger;
    private final VelocityConfigManager cfg;

    public PlayerLoginListener(ProxyServer server, NetwatchzServices services, VelocityConfigManager cfg, Logger logger) {
        this.server = server;
        this.services = services;
        this.logger = logger;
        this.cfg = cfg;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        InetSocketAddress address = event.getConnection().getRemoteAddress();
        String ip = address.getAddress().getHostAddress();
        String playerName = event.getUsername();

        logger.info("Player attempting to join from IP: {}", ip);

        if (handleIpListBlock(playerName, ip, event)) return;
        if (handleGeoBlock(playerName, ip, event)) return;
        handleVpnBlock(playerName, ip, event);
    }

    private boolean handleIpListBlock(String playerName, String ip, PreLoginEvent event) {
        boolean ipListEnabled = cfg.getBoolean("ip_list.enabled", true);
        if (!ipListEnabled) return false;

        String mode = cfg.getString("ip_list.mode", "blacklist").toLowerCase();
        boolean isInList = services.ipListService().isIpInAnyList(ip);

        if ((mode.equals("blacklist") && isInList) || (mode.equals("whitelist") && !isInList)) {
            logger.info("Blocked {} due to IP list ({})", playerName, ip);
            String msg = "Your IP address has been blocked by the server.";
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text(msg)));
            return true;
        }

        return false;
    }

    private boolean handleGeoBlock(String playerName, String ip, PreLoginEvent event) {
        boolean geoEnabled = cfg.getBoolean("geo_blocking.enabled", true);
        if (!geoEnabled) return false;

        IpData ipData = services.ipDataFetcher().fetchIpData(ip);
        if (ipData == null) {
            logger.warn("Failed to fetch IP data for {} ({})", playerName, ip);
            return false;
        }

        boolean blacklist = cfg.getBoolean("geo_blocking.blacklist", true);
        List<String> countryList = cfg.getStringList("geo_blocking.countries");

        boolean blocked = (blacklist && countryList.contains(ipData.countryCode()))
                || (!blacklist && !countryList.contains(ipData.countryCode()));

        if (blocked) {
            logger.info("Blocked {} due to geoblocking: {}", playerName, ipData.countryCode());
            String msg = "You are not allowed to join from your country.";
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text(msg)));
            return true;
        }

        return false;
    }

    private void handleVpnBlock(String playerName, String ip, PreLoginEvent event) {
        boolean vpnEnabled = cfg.getBoolean("vpn_block.enabled", true);
        if (!vpnEnabled) return;

        VpnInfoData vpnData = services.vpnInfoProvider().fetchVpnData(ip);
        if (vpnData == null) {
            logger.warn("Failed to fetch VPN info for {} ({})", playerName, ip);
            return;
        }

        if (!(vpnData.isVpn() || vpnData.isProxy() || vpnData.isTor() || vpnData.isRelay() || vpnData.isHosting())) return;

        String type = vpnData.isVpn() ? "vpn" :
                vpnData.isProxy() ? "proxy" :
                        vpnData.isTor() ? "tor" :
                                vpnData.isRelay() ? "relay" :
                                        vpnData.isHosting() ? "hosting" : "unknown";

        logger.info("Blocked {} due to {}", playerName, type);

        List<String> commands = cfg.getStringList("vpn_block." + type + ".commands");
        boolean shouldBlock = cfg.getBoolean("vpn_block." + type + ".block", true);

        for (String cmd : commands) {
            String parsed = cmd.replace("%player%", playerName).replace("%ip%", ip);
            server.getScheduler().buildTask(server, () ->
                    server.getCommandManager().executeAsync(server.getConsoleCommandSource(), parsed)
            ).schedule();
        }

        if (shouldBlock) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text("Your IP is associated with a VPN or proxy service.")));
        }
    }
}