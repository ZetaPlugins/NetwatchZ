package com.zetaplugins.netwatchz.paper;

import com.zetaplugins.netwatchz.common.ipapi.fetchers.CustomIpDataFetcher;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.ipapi.fetchers.IpApiCom;
import com.zetaplugins.netwatchz.common.ipapi.fetchers.IpDataFetcher;
import com.zetaplugins.netwatchz.common.ipapi.fetchers.IpWhois;
import com.zetaplugins.netwatchz.paper.listeners.AsyncPlayerPreLoginListener;
import com.zetaplugins.netwatchz.paper.util.CommandManager;
import com.zetaplugins.netwatchz.paper.util.EventManager;
import com.zetaplugins.zetacore.services.LocalizationService;
import com.zetaplugins.zetacore.services.MessageService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
        getLogger().info("NetwatchZPaper has been disabled!");
    }

    private Cache<@NotNull String, IpData> createCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }

    private IpDataFetcher createIpDataFetcher(FileConfiguration config) {
        switch (config.getString("ip_info_provider.provider", "ip-api.com")) {
            case "ip-api":
                return new IpApiCom(createCache());
            case "ipwhois":
                return new IpWhois(createCache());
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
