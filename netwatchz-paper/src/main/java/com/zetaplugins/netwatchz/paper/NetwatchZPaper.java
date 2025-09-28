package com.zetaplugins.netwatchz.paper;

import com.zetaplugins.netwatchz.common.config.*;
import com.zetaplugins.netwatchz.common.ipapi.fetchers.*;
import com.zetaplugins.netwatchz.common.iplist.IpListFetcher;
import com.zetaplugins.netwatchz.common.iplist.IpListService;
import com.zetaplugins.netwatchz.common.vpnblock.providers.VpnInfoProvider;
import com.zetaplugins.netwatchz.paper.util.CommandManager;
import com.zetaplugins.netwatchz.paper.util.PaperConfigManager;
import com.zetaplugins.netwatchz.paper.util.EventManager;
import com.zetaplugins.netwatchz.paper.util.Metrics;
import com.zetaplugins.zetacore.ZetaCorePlugin;
import com.zetaplugins.zetacore.services.LocalizationService;
import com.zetaplugins.zetacore.services.MessageService;

import java.net.InetAddress;
import java.util.ArrayList;

public final class NetwatchZPaper extends ZetaCorePlugin {
    private IpDataFetcher ipDataFetcher;
    private LocalizationService localizationService;
    private MessageService messageService;
    private IpListService ipListService;
    private IpListFetcher ipListFetcher;
    private VpnInfoProvider vpnInfoProvider;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        var configManager = new PaperConfigManager(this);

        IpInfoProviderConfig ipInfoCfg = configManager.loadIpInfoProviderConfig();
        IpListConfig ipListCfg = configManager.loadIpListConfig();
        VpnBlockConfig vpnBlockCfg = configManager.loadVpnBlockConfig();

        ipDataFetcher = IpDataFetcher.fromConfig(ipInfoCfg, getLogger());
        ipListFetcher = IpListFetcher.fromConfig(ipListCfg, getLogger());
        ipListService = IpListService.fromConfig(ipListCfg, getLogger());
        vpnInfoProvider = VpnInfoProvider.fromConfig(vpnBlockCfg);
        localizationService = new LocalizationService(this, new ArrayList<>() {{
            add("en-US");
            add("de-DE");
        }});
        messageService = new MessageService(localizationService);

        new EventManager(this).registerListeners();
        new CommandManager(this).registerCommands();

        initializeBStats();

        getLogger().info("NetwatchZPaper has been enabled!");
    }

    public IpDataFetcher getIpDataFetcher() {
        return ipDataFetcher;
    }

    public IpListService getIpListService() {
        return ipListService;
    }

    public IpListFetcher getIpListFetcher() {
        return ipListFetcher;
    }

    public VpnInfoProvider getVpnInfoProvider() {
        return vpnInfoProvider;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    @Override
    public void onDisable() {
        if (ipDataFetcher != null) ipDataFetcher.onShutDown();
        getLogger().info("NetwatchZPaper has been disabled!");
    }

    public static String getIpFromInetAdress(InetAddress addr) {
        //return "146.70.231.4";
        return addr.getHostAddress();
    }

    private void initializeBStats() {
        int pluginId = 27376;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new Metrics.SimplePie("ip_info_provider", () -> getConfig().getString("ip_info_provider.provider")));
        metrics.addCustomChart(new Metrics.SimplePie("geo_blocking_enabled", () -> getConfig().getBoolean("geo_blocking.enabled") ? "true" : "false"));
        metrics.addCustomChart(new Metrics.SimplePie("ip_list_enabled", () -> getConfig().getBoolean("ip_list.enabled") ? "true" : "false"));
    }
}
