package com.zetaplugins.netwatchz.common.ipapi.fetchers;

import com.github.benmanes.caffeine.cache.Cache;
import com.zetaplugins.netwatchz.common.CacheUtils;
import com.zetaplugins.netwatchz.common.config.CustomProviderConfig;
import com.zetaplugins.netwatchz.common.config.GeoLite2Config;
import com.zetaplugins.netwatchz.common.config.IpInfoProviderConfig;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.DataFetchException;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract class for fetching IP data from various APIs.
 * This class provides a method to fetch data from a URL and parse the response.
 */
public abstract class IpDataFetcher {
    private final Cache<@NotNull String, IpData> cache;

    public IpDataFetcher(Cache<@NotNull String, IpData> cache) {
        this.cache = cache;
    }

    /**
     * Creates an IpDataFetcher instance based on the provided configuration.
     * @param cfg configuration containing provider settings
     * @param logger logger for logging fetch results
     * @return IpDataFetcher instance
     */
    public static IpDataFetcher fromConfig(IpInfoProviderConfig cfg, Logger logger) {
        switch (cfg.provider()) {
            case IPWHOIS:
                return new IpWhois(CacheUtils.createIpApiCache());
            case GEOLITE2:
                GeoLite2Config g = cfg.geoLite2();
                if (g == null) return new IpApiCom(CacheUtils.createIpApiCache());
                return new GeoLite2Fetcher(
                        logger,
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

    protected Cache<@NotNull String, IpData> getCache() {
        return cache;
    }

    protected abstract String getApiUrl();

    /**
     * Fetches IP data for the given IP address.
     * @param ip the IP address to fetch data for
     * @throws DataFetchException if an error occurs while fetching the data
     * @return the IP data for the specified IP address
     */
    public IpData fetchIpData(String ip) throws DataFetchException {
        if (ip == null || ip.isEmpty()) {
            throw new DataFetchException("IP address cannot be null or empty");
        }

        if (getCache().getIfPresent(ip) != null) {
            return getCache().getIfPresent(ip);
        }

        try {
            URL url = new URL(getApiUrl() + ip);
            String jsonResponse = fetchFromApi(url);
            IpData data = parseIpData(jsonResponse);
            if (data == null) return null;
            getCache().put(ip, data);
            return data;
        } catch (Exception e) {
            throw new DataFetchException("Failed to fetch IP data from " + getApiUrl() + ip, e);
        }
    }

    /**
     * Parses the JSON response to create an IpData object.
     * @param jsonResponse the JSON response from the API
     * @throws ParseException if an error occurs while parsing the JSON
     * @return an IpData object containing the parsed data
     */
    protected abstract IpData parseIpData(String jsonResponse) throws ParseException;

    /**
     * Fetches data from the specified URL.
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
     * Fetches data from the specified URL.
     * @param url the URL to fetch data from
     * @throws IOException if an error occurs while fetching the data
     * @return the response as a String
     */
    protected String fetchFromApi(URL url) throws IOException {
        return fetchFromApi(url, null);
    }

    /**
     * Called when the application is shutting down.
     * Can be overridden by subclasses to perform cleanup tasks.
     */
    public void onShutDown() {}
}
