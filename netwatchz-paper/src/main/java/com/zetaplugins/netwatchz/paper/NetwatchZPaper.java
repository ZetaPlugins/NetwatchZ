package com.zetaplugins.netwatchz.paper;

import com.zetaplugins.netwatchz.common.ipapi.fetchers.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.iplist.IpListFetchJob;
import com.zetaplugins.netwatchz.common.iplist.IpListFetcher;
import com.zetaplugins.netwatchz.common.iplist.IpListService;
import com.zetaplugins.netwatchz.paper.util.CommandManager;
import com.zetaplugins.netwatchz.paper.util.EventManager;
import com.zetaplugins.netwatchz.paper.util.Metrics;
import com.zetaplugins.zetacore.services.LocalizationService;
import com.zetaplugins.zetacore.services.MessageService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

        ipDataFetcher = createIpDataFetcher(getConfig());
        ipListFetcher = createIpListFetcher(getConfig());
        ipListService = createIpListService(getConfig());
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

    private Cache<@NotNull String, IpData> createCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }

    public static String getIpFromInetAdress(InetAddress addr) {
        return "146.70.231.4";
        //return addr.getHostAddress();
    }

    private static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private IpListService createIpListService(FileConfiguration config) {
        Path ipListsDir = getDataFolder().toPath().resolve("ipLists");
        if (!ipListsDir.toFile().exists() && !ipListsDir.toFile().mkdirs()) {
            getLogger().warning("Failed to create ipLists directory at " + ipListsDir);
        }

        Set<String> listNames = config.getStringList("ip_list.lists").stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        return new IpListService(
                listNames.stream()
                        .map(ipListsDir::resolve)
                        .collect(Collectors.toList()),
                getLogger()
        );
    }

    private IpListFetcher createIpListFetcher(FileConfiguration config) {
        Path ipListsDir = getDataFolder().toPath().resolve("ipLists");
        if (!ipListsDir.toFile().exists() && !ipListsDir.toFile().mkdirs()) {
            getLogger().warning("Failed to create ipLists directory at " + ipListsDir);
        }

        var jobs = config.getConfigurationSection("ip_list.fetch_jobs") != null ?
                config.getConfigurationSection("ip_list.fetch_jobs").getKeys(false) : Set.<String>of();
        var fetchJobs = jobs.stream()
                .map(jobKey -> {
                    String url = config.getString("ip_list.fetch_jobs." + jobKey + ".url", "").trim();
                    String filename = config.getString("ip_list.fetch_jobs." + jobKey + ".filename", "").trim();
                    int intervalHours = Math.max(1, config.getInt("ip_list.fetch_jobs." + jobKey + ".update_interval_hours", 24));

                    if (url.isEmpty() || filename.isEmpty()) {
                        getLogger().warning("Invalid fetch job '" + jobKey + "' in config, missing url or filename");
                        return null;
                    }

                    return new IpListFetchJob(
                            url,
                            ipListsDir.resolve(filename),
                            Duration.ofHours(intervalHours)
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        var fetcher = new IpListFetcher(getLogger());
        if (!fetchJobs.isEmpty()) fetcher.start(fetchJobs);
        return fetcher;
    }

    private IpDataFetcher createIpDataFetcher(FileConfiguration config) {
        switch (config.getString("ip_info_provider.provider", "ip-api.com")) {
            case "ip-api":
                return new IpApiCom(createCache());
            case "ipwhois":
                return new IpWhois(createCache());
            case "geolite2":
                String asnUrl = config.getString("ip_info_provider.geolite2.asn_url", "");
                if (!asnUrl.isEmpty() && !isValidUrl(asnUrl)) {
                    getLogger().warning("Invalid GeoLite2 ASN URL in config, defaulting to ip-api.com");
                    return new IpApiCom(createCache());
                }

                String cityUrl = config.getString("ip_info_provider.geolite2.city_url", "");
                if (!cityUrl.isEmpty() && !isValidUrl(cityUrl)) {
                    getLogger().warning("Invalid GeoLite2 City URL in config, defaulting to ip-api.com");
                    return new IpApiCom(createCache());
                }

                String countryUrl = config.getString("ip_info_provider.geolite2.country_url", "");
                if (!countryUrl.isEmpty() && !isValidUrl(countryUrl)) {
                    getLogger().warning("Invalid GeoLite2 Country URL in config, defaulting to ip-api.com");
                    return new IpApiCom(createCache());
                }

                return new GeoLite2Fetcher(
                        getLogger(),
                        createCache(),
                        getDataFolder().toPath().resolve("GeoLite2"),
                        Duration.ofDays(7),
                        asnUrl,
                        cityUrl,
                        countryUrl
                );
            case "custom":
                String apiUrl = config.getString("ip_info_provider.custom.url", "http://ip-api.com/json/%ip%");

                Set<String> headersKeys = config.getConfigurationSection("ip_info_provider.custom.headers") != null ?
                        config.getConfigurationSection("ip_info_provider.custom.headers").getKeys(false) : Set.of();
                Map<String, String> headers = headersKeys.stream()
                        .collect(Collectors.toMap(
                                key -> key,
                                key -> config.getString("ip_info_provider.custom.headers." + key, "")
                        ));

                Set<String> parseFieldsKeys = config.getConfigurationSection("ip_info_provider.custom.parse_fields") != null ?
                        config.getConfigurationSection("ip_info_provider.custom.parse_fields").getKeys(false) : Set.of();
                Map<String, String> parseFields = parseFieldsKeys.stream()
                        .collect(Collectors.toMap(
                                key -> key,
                                key -> config.getString("ip_info_provider.custom.parse_fields." + key, "")
                        ));

                return new CustomIpDataFetcher(createCache(), apiUrl, headers, parseFields);
            default:
                getLogger().warning("Unknown IP API fetcher specified in config, defaulting to ip-api.com");
                return new IpApiCom(createCache());
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
