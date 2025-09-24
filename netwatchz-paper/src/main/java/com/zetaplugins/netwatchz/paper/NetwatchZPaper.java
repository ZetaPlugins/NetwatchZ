package com.zetaplugins.netwatchz.paper;

import com.zetaplugins.netwatchz.common.ipapi.fetchers.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.paper.util.CommandManager;
import com.zetaplugins.netwatchz.paper.util.EventManager;
import com.zetaplugins.zetacore.services.LocalizationService;
import com.zetaplugins.zetacore.services.MessageService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class NetwatchZPaper extends JavaPlugin {
    private IpDataFetcher ipDataFetcher;
    private LocalizationService localizationService;
    private MessageService messageService;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        ipDataFetcher = createIpDataFetcher(getConfig());
        localizationService = new LocalizationService(this, new ArrayList<>() {{
            add("en-US");
            add("de-DE");
        }});
        messageService = new MessageService(localizationService);

        new EventManager(this).registerListeners();
        new CommandManager(this).registerCommands();

        getLogger().info("NetwatchZPaper has been enabled!");
    }

    public IpDataFetcher getIpDataFetcher() {
        return ipDataFetcher;
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
}
