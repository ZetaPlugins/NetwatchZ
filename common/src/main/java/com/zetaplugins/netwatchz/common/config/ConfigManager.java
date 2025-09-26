package com.zetaplugins.netwatchz.common.config;

import java.net.URL;

/**
 * Abstract class for managing configuration loading. Should be extended by platform-specific implementations.
 */
public abstract class ConfigManager {

    public abstract IpInfoProviderConfig loadIpInfoProviderConfig();

    public abstract IpListConfig loadIpListConfig();

    public abstract VpnBlockConfig loadVpnBlockConfig();

    /**
     * Check if a URL is valid
     * @param url the URL to check
     * @return true if the URL is valid, false otherwise
     */
    protected static boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
