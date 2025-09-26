package com.zetaplugins.netwatchz.paper.util;

import com.zetaplugins.netwatchz.common.config.*;
import com.zetaplugins.netwatchz.common.iplist.IpListFetchJob;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class PaperConfigManager extends ConfigManager {
    private final JavaPlugin plugin;
    private final FileConfiguration cfg;

    public PaperConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    @Override
    public IpInfoProviderConfig loadIpInfoProviderConfig() {
        String raw = cfg.getString("ip_info_provider.provider", "ip-api").trim().toLowerCase(Locale.ROOT);
        IpInfoProviderConfig.Provider p = switch (raw) {
            case "ipwhois" -> IpInfoProviderConfig.Provider.IPWHOIS;
            case "geolite2" -> IpInfoProviderConfig.Provider.GEOLITE2;
            case "custom" -> IpInfoProviderConfig.Provider.CUSTOM;
            default -> IpInfoProviderConfig.Provider.IP_API;
        };

        GeoLite2Config geo = null;
        if (p == IpInfoProviderConfig.Provider.GEOLITE2) {
            String asnUrl = cfg.getString("ip_info_provider.geolite2.asn_url", "");
            String cityUrl = cfg.getString("ip_info_provider.geolite2.city_url", "");
            String countryUrl = cfg.getString("ip_info_provider.geolite2.country_url", "");
            Path storage = plugin.getDataFolder().toPath().resolve("GeoLite2");

            if (!asnUrl.isBlank() && !isValidUrl(asnUrl)) {
                plugin.getLogger().warning("Invalid GeoLite2 ASN URL; ignoring.");
                asnUrl = "";
            }
            if (!cityUrl.isBlank() && !isValidUrl(cityUrl)) {
                plugin.getLogger().warning("Invalid GeoLite2 City URL; ignoring.");
                cityUrl = "";
            }
            if (!countryUrl.isBlank() && !isValidUrl(countryUrl)) {
                plugin.getLogger().warning("Invalid GeoLite2 Country URL; ignoring.");
                countryUrl = "";
            }
            geo = new GeoLite2Config(asnUrl, cityUrl, countryUrl, storage, Duration.ofDays(7));
        }

        CustomProviderConfig custom = null;
        if (p == IpInfoProviderConfig.Provider.CUSTOM) {
            String apiUrl = cfg.getString("ip_info_provider.custom.url", "http://ip-api.com/json/%ip%");
            ConfigurationSection sec = cfg.getConfigurationSection("ip_info_provider.custom.headers");
            Map<String,String> headers = sec == null ? Map.of() :
                    sec.getKeys(false).stream().collect(Collectors.toMap(k -> k, k -> sec.getString(k, "")));
            ConfigurationSection parseSec = cfg.getConfigurationSection("ip_info_provider.custom.parse_fields");
            Map<String,String> parseFields = parseSec == null ? Map.of() :
                    parseSec.getKeys(false).stream().collect(Collectors.toMap(k -> k, k -> parseSec.getString(k, "")));
            custom = new CustomProviderConfig(apiUrl, headers, parseFields);
        }

        return new IpInfoProviderConfig(p, geo, custom);
    }

    @Override
    public IpListConfig loadIpListConfig() {
        Path ipListsDir = plugin.getDataFolder().toPath().resolve("ipLists");
        if (!ipListsDir.toFile().exists() && !ipListsDir.toFile().mkdirs()) {
            plugin.getLogger().warning("Failed to create ipLists directory at " + ipListsDir);
        }

        Set<String> listNames = cfg.getStringList("ip_list.lists").stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        ConfigurationSection jobsSection = cfg.getConfigurationSection("ip_list.fetch_jobs");
        List<IpListFetchJob> jobs = List.of();
        if (jobsSection != null) {
            jobs = jobsSection.getKeys(false).stream()
                    .map(key -> {
                        String url = cfg.getString("ip_list.fetch_jobs." + key + ".url", "").trim();
                        String filename = cfg.getString("ip_list.fetch_jobs." + key + ".filename", "").trim();
                        int intervalHours = Math.max(1, cfg.getInt("ip_list.fetch_jobs." + key + ".update_interval_hours", 24));
                        if (url.isEmpty() || filename.isEmpty()) {
                            plugin.getLogger().warning("Invalid fetch job '" + key + "', missing url or filename");
                            return null;
                        }
                        return new IpListFetchJob(url, ipListsDir.resolve(filename), Duration.ofHours(intervalHours));
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return new IpListConfig(ipListsDir, jobs, listNames);
    }
}
