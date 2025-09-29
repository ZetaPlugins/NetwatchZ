package com.zetaplugins.netwatchz.velocity.util;

import com.zetaplugins.netwatchz.common.config.*;
import com.zetaplugins.netwatchz.common.iplist.IpListFetchJob;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

import org.slf4j.Logger;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * Manages the loading and parsing of the configuration file for the Velocity platform.
 */
public class VelocityConfigManager extends ConfigManager {

    private final Logger logger;
    private final Path dataDirectory;
    private ConfigurationNode rootNode;

    public VelocityConfigManager(Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        Path configFile = dataDirectory.resolve("config.yml");
        try {
            if (Files.notExists(configFile)) {
                Files.createDirectories(dataDirectory);
                var configStream = getClass().getClassLoader().getResourceAsStream("config.yml");
                if (configStream == null) throw new IOException("Default config.yml not found in resources.");
                Files.copy(configStream, configFile);
            }
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configFile)
                    .build();
            rootNode = loader.load();
        } catch (IOException e) {
            logger.error("Failed to load config.yml", e);
        }
    }

    /**
     * Gets a string value from the configuration at the specified path, returning a default value if not found or on error.
     * @param path the configuration path (dot-separated)
     * @param def the default value to return if the path is not found or an error occurs
     * @return the string value from the configuration, or the default value
     */
    public String getString(String path, String def) {
        try {
            return rootNode.node((Object[]) path.split("\\.")).getString(def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Gets a list of strings from the configuration at the specified path, returning an empty list if not found or on error.
     * @param path the configuration path (dot-separated)
     * @return the list of strings from the configuration, or an empty list
     */
    public List<String> getStringList(String path) {
        try {
            return rootNode.node((Object[]) path.split("\\.")).getList(String.class, List.of());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Gets a boolean value from the configuration at the specified path, returning a default value if not found or on error.
     * @param path the configuration path (dot-separated)
     * @param def the default value to return if the path is not found or an error occurs
     * @return the boolean value from the configuration, or the default value
     */
    public boolean getBoolean(String path, boolean def) {
        try {
            return rootNode.node((Object[]) path.split("\\.")).getBoolean(def);
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Gets an integer value from the configuration at the specified path, returning a default value if not found or on error.
     * @param path the configuration path (dot-separated)
     * @param def the default value to return if the path is not found or an error occurs
     * @return the integer value from the configuration, or the default value
     */
    public int getInt(String path, int def) {
        try {
            return rootNode.node((Object[]) path.split("\\.")).getInt(def);
        } catch (Exception e) {
            return def;
        }
    }

    private ConfigurationNode getNode(String path) {
        return rootNode.node((Object[]) path.split("\\."));
    }

    @Override
    public IpInfoProviderConfig loadIpInfoProviderConfig() {
        String raw = getString("ip_info_provider.provider", "ip-api").trim().toLowerCase(Locale.ROOT);
        IpInfoProviderConfig.Provider p = switch (raw) {
            case "ipwhois" -> IpInfoProviderConfig.Provider.IPWHOIS;
            case "geolite2" -> IpInfoProviderConfig.Provider.GEOLITE2;
            case "custom" -> IpInfoProviderConfig.Provider.CUSTOM;
            default -> IpInfoProviderConfig.Provider.IP_API;
        };

        GeoLite2Config geo = null;
        if (p == IpInfoProviderConfig.Provider.GEOLITE2) {
            String asnUrl = getString("ip_info_provider.geolite2.asn_url", "");
            String cityUrl = getString("ip_info_provider.geolite2.city_url", "");
            String countryUrl = getString("ip_info_provider.geolite2.country_url", "");
            Path storage = dataDirectory.resolve("GeoLite2");

            if (!asnUrl.isBlank() && !isValidUrl(asnUrl)) {
                logger.warn("Invalid GeoLite2 ASN URL; ignoring.");
                asnUrl = "";
            }
            if (!cityUrl.isBlank() && !isValidUrl(cityUrl)) {
                logger.warn("Invalid GeoLite2 City URL; ignoring.");
                cityUrl = "";
            }
            if (!countryUrl.isBlank() && !isValidUrl(countryUrl)) {
                logger.warn("Invalid GeoLite2 Country URL; ignoring.");
                countryUrl = "";
            }
            geo = new GeoLite2Config(asnUrl, cityUrl, countryUrl, storage, Duration.ofDays(7));
        }

        CustomProviderConfig custom = null;
        if (p == IpInfoProviderConfig.Provider.CUSTOM) {
            String apiUrl = getString("ip_info_provider.custom.url", "http://ip-api.com/json/%ip%");
            ConfigurationNode headersNode = getNode("ip_info_provider.custom.headers");
            Map<String, String> headers = new HashMap<>();
            headersNode.childrenMap().forEach((k, v) -> headers.put(k.toString(), v.getString("")));

            ConfigurationNode parseNode = getNode("ip_info_provider.custom.parse_fields");
            Map<String, String> parseFields = new HashMap<>();
            parseNode.childrenMap().forEach((k, v) -> parseFields.put(k.toString(), v.getString("")));

            custom = new CustomProviderConfig(apiUrl, headers, parseFields);
        }

        return new IpInfoProviderConfig(p, geo, custom);
    }

    @Override
    public IpListConfig loadIpListConfig() {
        Path ipListsDir = dataDirectory.resolve("ipLists");
        try {
            Files.createDirectories(ipListsDir);
        } catch (IOException e) {
            logger.warn("Failed to create ipLists directory at {}", ipListsDir, e);
        }

        List<String> listNames;
        try {
            listNames = rootNode.node("ip_list", "lists").getList(String.class, List.of())
                    .stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        } catch (SerializationException e) {
            logger.warn("Failed to load IP list names from config", e);
            return new IpListConfig(ipListsDir, List.of(), Set.of());
        }

        List<IpListFetchJob> jobs = new ArrayList<>();
        ConfigurationNode jobsNode = getNode("ip_list.fetch_jobs");
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : jobsNode.childrenMap().entrySet()) {
            ConfigurationNode jobNode = entry.getValue();
            String url = jobNode.node("url").getString("").trim();
            String filename = jobNode.node("filename").getString("").trim();
            int intervalHours = Math.max(1, jobNode.node("update_interval_hours").getInt(24));

            if (!url.isEmpty() && !filename.isEmpty()) {
                jobs.add(new IpListFetchJob(url, ipListsDir.resolve(filename), Duration.ofHours(intervalHours)));
            } else {
                logger.warn("Invalid fetch job '{}', missing url or filename", entry.getKey());
            }
        }

        return new IpListConfig(ipListsDir, jobs, new HashSet<>(listNames));
    }

    @Override
    public VpnBlockConfig loadVpnBlockConfig() {
        boolean enabled = rootNode.node("vpn_block.enabled").getBoolean(false);
        String apiKey = getString("vpn_block.api_key", "").trim();

        String raw = getString("vpn_block.provider", "vpnapi").trim().toLowerCase(Locale.ROOT);
        VpnBlockConfig.Provider p = switch (raw) {
            case "custom" -> VpnBlockConfig.Provider.CUSTOM;
            case "proxycheck" -> VpnBlockConfig.Provider.PROXYCHECK;
            default -> VpnBlockConfig.Provider.VPNAPI;
        };

        CustomProviderConfig custom = null;
        if (p == VpnBlockConfig.Provider.CUSTOM) {
            String apiUrl = getString("vpn_block.custom.url", "");
            if (apiUrl.isBlank() || !isValidUrl(apiUrl)) {
                logger.warn("Invalid VPN Block custom URL; defaulting to VPNAPI.");
                p = VpnBlockConfig.Provider.VPNAPI;
            } else {
                ConfigurationNode headersNode = getNode("vpn_block.custom.headers");
                Map<String, String> headers = new HashMap<>();
                headersNode.childrenMap().forEach((k, v) -> headers.put(k.toString(), v.getString("")));

                ConfigurationNode parseNode = getNode("vpn_block.custom.parse_fields");
                Map<String, String> parseFields = new HashMap<>();
                parseNode.childrenMap().forEach((k, v) -> parseFields.put(k.toString(), v.getString("")));

                custom = new CustomProviderConfig(apiUrl, headers, parseFields);
            }
        }

        return new VpnBlockConfig(enabled, p, apiKey, custom);
    }
}