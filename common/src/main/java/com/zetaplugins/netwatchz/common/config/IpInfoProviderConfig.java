package com.zetaplugins.netwatchz.common.config;

/**
 * Configuration for IP information providers
 * @param provider the selected provider
 * @param geoLite2 configuration, if using GeoLite2
 * @param custom configuration, if using a custom provider
 */
public record IpInfoProviderConfig(Provider provider, GeoLite2Config geoLite2, CustomProviderConfig custom) {
    public enum Provider {IP_API, IPWHOIS, GEOLITE2, CUSTOM}
}
