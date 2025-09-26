package com.zetaplugins.netwatchz.common.config;

public record VpnBlockConfig(boolean enabled, Provider provider, String apiKey,
                             CustomProviderConfig customProviderConfig) {
    public enum Provider {VPNAPI, PROXYCHECK, CUSTOM}
}
