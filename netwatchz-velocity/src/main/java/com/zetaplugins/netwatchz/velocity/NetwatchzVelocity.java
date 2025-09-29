package com.zetaplugins.netwatchz.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zetaplugins.netwatchz.common.NetwatchzServices;
import com.zetaplugins.netwatchz.common.config.IpInfoProviderConfig;
import com.zetaplugins.netwatchz.common.config.IpListConfig;
import com.zetaplugins.netwatchz.common.config.VpnBlockConfig;
import com.zetaplugins.netwatchz.common.ipapi.fetchers.IpDataFetcher;
import com.zetaplugins.netwatchz.common.iplist.IpListFetcher;
import com.zetaplugins.netwatchz.common.iplist.IpListService;
import com.zetaplugins.netwatchz.common.vpnblock.providers.VpnInfoProvider;
import com.zetaplugins.netwatchz.velocity.listeners.PlayerLoginListener;
import com.zetaplugins.netwatchz.velocity.util.*;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;

@Plugin(
    id = "netwatchz",
    name = "NetwatchZ",
    version = "1.0.0"
    ,description = "Anti VPN, Geoblocking and IP blacklisting plugin for Velocity"
    ,authors = {"ZetaPlugins"}
    ,url = "zetaplugins.com"
)
public class NetwatchzVelocity {

    @Inject private Logger logger;
    @Inject private ProxyServer server;
    @Inject @DataDirectory private Path dataDirectory;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        new ConfigInitializer(logger, dataDirectory).initializeDefaultConfig();
        var configManager = new VelocityConfigManager(logger, dataDirectory);

        var localizationService = new VelocityLocalizationService(
            dataDirectory,
            logger,
            List.of("en-US", "de-DE"),
            configManager.getString("lang", "en-US")
        );
        var messageSevice = new VelocityMessageService(localizationService);

        IpInfoProviderConfig ipInfoCfg = configManager.loadIpInfoProviderConfig();
        IpListConfig ipListCfg = configManager.loadIpListConfig();
        VpnBlockConfig vpnBlockCfg = configManager.loadVpnBlockConfig();

        var ipDataFetcher = IpDataFetcher.fromConfig(ipInfoCfg, new JulFromSlf4j("NetwatchZLogger", logger));
        var ipListFetcher = IpListFetcher.fromConfig(ipListCfg, new JulFromSlf4j("NetwatchZLogger", logger));
        var ipListService = IpListService.fromConfig(ipListCfg, new JulFromSlf4j("NetwatchZLogger", logger));
        var vpnInfoProvider = VpnInfoProvider.fromConfig(vpnBlockCfg);

        var services = new NetwatchzServices(ipDataFetcher, ipListService, ipListFetcher, vpnInfoProvider);

        server.getEventManager().register(
                this,
                new PlayerLoginListener(server, services, configManager, logger, messageSevice)
        );

        logger.info("NetwatchZ has been initialized!");
    }

}
