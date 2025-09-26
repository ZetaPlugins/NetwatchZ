package com.zetaplugins.netwatchz.paper;

import com.zetaplugins.netwatchz.common.CacheUtils;
import com.zetaplugins.netwatchz.common.config.CustomProviderConfig;
import com.zetaplugins.netwatchz.common.config.GeoLite2Config;
import com.zetaplugins.netwatchz.common.config.IpInfoProviderConfig;
import com.zetaplugins.netwatchz.common.config.IpListConfig;
import com.zetaplugins.netwatchz.common.ipapi.fetchers.*;
import com.zetaplugins.netwatchz.common.iplist.IpListFetcher;
import com.zetaplugins.netwatchz.common.iplist.IpListService;
import com.zetaplugins.netwatchz.paper.util.CommandManager;
import com.zetaplugins.netwatchz.paper.util.PaperConfigManager;
import com.zetaplugins.netwatchz.paper.util.EventManager;
import com.zetaplugins.netwatchz.paper.util.Metrics;
import com.zetaplugins.zetacore.services.LocalizationService;
import com.zetaplugins.zetacore.services.MessageService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.stream.Collectors;

public final class NetwatchZPaper extends JavaPlugin {
    private IpDataFetcher ipDataFetcher;
    private LocalizationService localizationService;
    private MessageService messageService;
    private IpListService ipListService;
    private IpListFetcher ipListFetcher;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        var configManager = new PaperConfigManager(this);

        IpInfoProviderConfig ipInfoCfg = configManager.loadIpInfoProviderConfig();
        IpListConfig ipListCfg = configManager.loadIpListConfig();

        ipDataFetcher = createIpDataFetcher(ipInfoCfg);
        ipListFetcher = createIpListFetcher(ipListCfg);
        ipListService = createIpListService(ipListCfg);
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
        return "146.70.231.4";
        //return addr.getHostAddress();
    }

    private IpListService createIpListService(IpListConfig cfg) {
        return new IpListService(cfg.listNames().stream()
                .map(cfg.ipListsDir()::resolve)
                .collect(Collectors.toList()),
                getLogger());
    }

    private IpListFetcher createIpListFetcher(IpListConfig cfg) {
        var fetcher = new IpListFetcher(getLogger());
        if (!cfg.fetchJobs().isEmpty()) fetcher.start(cfg.fetchJobs());
        return fetcher;
    }

    private IpDataFetcher createIpDataFetcher(IpInfoProviderConfig cfg) {
        switch (cfg.provider()) {
            case IPWHOIS:
                return new IpWhois(CacheUtils.createIpApiCache());
            case GEOLITE2:
                GeoLite2Config g = cfg.geoLite2();
                if (g == null) return new IpApiCom(CacheUtils.createIpApiCache());
                return new GeoLite2Fetcher(
                        getLogger(),
                        CacheUtils.createIpApiCache(),
                        g.storageDir(),
                        g.updateIntervalDays(),
                        g.asnUrl(),
                        g.cityUrl(),
                        g.countryUrl()
                );
            case CUSTOM:
                CustomProviderConfig c = cfg.custom();
                if (c == null) return new IpApiCom(CacheUtils.createIpApiCache());
                return new CustomIpDataFetcher(CacheUtils.createIpApiCache(), c.apiUrl(), c.headers(), c.parseFields());
            default:
                return new IpApiCom(CacheUtils.createIpApiCache());
        }
    }

    private void initializeBStats() {
        int pluginId = 27376;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new Metrics.SimplePie("ip_info_provider", () -> getConfig().getString("ip_info_provider.provider")));
        metrics.addCustomChart(new Metrics.SimplePie("geo_blocking_enabled", () -> getConfig().getBoolean("geo_blocking.enabled") ? "true" : "false"));
        metrics.addCustomChart(new Metrics.SimplePie("ip_list_enabled", () -> getConfig().getBoolean("ip_list.enabled") ? "true" : "false"));
    }

    public File getPluginFile() {
        return getFile();
    }
}
