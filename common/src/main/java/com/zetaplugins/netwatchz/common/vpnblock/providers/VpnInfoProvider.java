package com.zetaplugins.netwatchz.common.vpnblock.providers;

import com.github.benmanes.caffeine.cache.Cache;
import com.zetaplugins.netwatchz.common.CacheUtils;
import com.zetaplugins.netwatchz.common.DataFetchException;
import com.zetaplugins.netwatchz.common.config.CustomProviderConfig;
import com.zetaplugins.netwatchz.common.config.VpnBlockConfig;
import com.zetaplugins.netwatchz.common.vpnblock.VpnInfoData;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public abstract class VpnInfoProvider {
    private final Cache<@NotNull String, VpnInfoData> cache;

    public VpnInfoProvider(Cache<@NotNull String, VpnInfoData> cache) {
        this.cache = cache;
    }

    /**
     * Creates a VpnInfoProvider instance based on the provided configuration.
     * @param cfg the VPN block configuration
     * @return a VpnInfoProvider instance
     */
    public static VpnInfoProvider fromConfig(VpnBlockConfig cfg) {
        switch (cfg.provider()) {
            case PROXYCHECK:
                return new ProxyCheck(CacheUtils.createVpnInfoCache(), cfg.apiKey());
            case CUSTOM:
                CustomProviderConfig c = cfg.customProviderConfig();
                if (c == null) return new VpnApi(CacheUtils.createVpnInfoCache(), cfg.apiKey());
                return new CustomVpnInfoProvider(CacheUtils.createVpnInfoCache(), c.apiUrl(), c.headers(), c.parseFields());
            default:
                return new VpnApi(CacheUtils.createVpnInfoCache(), cfg.apiKey());
        }
    }

    protected Cache<@NotNull String, VpnInfoData> getCache() {
        return cache;
    }

    protected abstract String getApiUrl();

    /**
     * Fetches VPN data for the given IP address.
     * @param ip the IP address to fetch data for
     * @throws DataFetchException if an error occurs while fetching the data
     * @return the VPN data for the specified IP address
     */
    public VpnInfoData fetchVpnData(String ip) throws DataFetchException {
        if (ip == null || ip.isEmpty()) {
            throw new DataFetchException("IP address cannot be null or empty");
        }

        if (getCache().getIfPresent(ip) != null) {
            return getCache().getIfPresent(ip);
        }

        try {
            URL url = new URL(getApiUrl().replace("%ip%", ip));
            String jsonResponse = fetchFromApi(url);
            VpnInfoData data = parseVpnData(jsonResponse);
            if (data == null) return null;
            getCache().put(ip, data);
            return data;
        } catch (Exception e) {
            throw new DataFetchException("Failed to fetch VPN data from " + getApiUrl() + ip, e);
        }
    }

    /**
     * Parses the VPN data from the JSON response.
     * @param jsonResponse the JSON response from the API
     * @throws ParseException if an error occurs while parsing the JSON
     * @return the parsed VPN data
     */
    protected abstract VpnInfoData parseVpnData(String jsonResponse) throws ParseException;

    /**
     * Fetches data from the specified URL with optional headers.
     * @param url the URL to fetch data from
     * @param headers optional headers to include in the request
     * @throws IOException if an error occurs while fetching the data
     * @return the response as a String
     */
    protected String fetchFromApi(URL url, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        }
    }

    /**
     * Fetches data from the specified URL without additional headers.
     * @param url the URL to fetch data from
     * @throws IOException if an error occurs while fetching the data
     * @return the response as a String
     */
    protected String fetchFromApi(URL url) throws IOException {
        return fetchFromApi(url, null);
    }
}
