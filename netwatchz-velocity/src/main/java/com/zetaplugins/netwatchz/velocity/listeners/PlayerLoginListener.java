package com.zetaplugins.netwatchz.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zetaplugins.netwatchz.common.NetwatchzServices;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.vpnblock.VpnInfoData;
import com.zetaplugins.netwatchz.velocity.util.VelocityConfigManager;
import com.zetaplugins.netwatchz.velocity.util.VelocityMessageService;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;

public class PlayerLoginListener {

    private final ProxyServer server;
    private final NetwatchzServices services;
    private final Logger logger;
    private final VelocityConfigManager cfg;
    private final VelocityMessageService msg;

    public PlayerLoginListener(ProxyServer server, NetwatchzServices services, VelocityConfigManager cfg, Logger logger, VelocityMessageService msg) {
        this.server = server;
        this.services = services;
        this.logger = logger;
        this.cfg = cfg;
        this.msg = msg;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        InetSocketAddress address = event.getConnection().getRemoteAddress();
        String ip = address.getAddress().getHostAddress();
//        String ip = "89.36.76.135";
        String playerName = event.getUsername();

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

            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(msg.getAndFormatMsg(
                    false,
                    "iplist_ban_message",
                    "&cYour IP address has been blocked due to suspicious activity!<br><br>&7If you believe this is an error, please contact support."
            )));
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
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(msg.getAndFormatMsg(
                    false,
                    "geoblock_ban_message",
                    "&cYour IP address has been blocked due to suspicious activity!<br><br>&7If you believe this is an error, please contact support."
            )));
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
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(msg.getAndFormatMsg(
                    false,
                    "vpnblock_ban_message",
                    "&cYour IP address has been blocked because it is associated with a VPN service!<br>Try disabling your VPN and reconnecting.<br><br>&7If you believe this is an error, please contact support."
            )));
        }
    }
}