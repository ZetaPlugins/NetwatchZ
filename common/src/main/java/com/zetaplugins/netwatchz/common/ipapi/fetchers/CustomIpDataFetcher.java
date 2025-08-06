package com.zetaplugins.netwatchz.common.ipapi.fetchers;

import com.github.benmanes.caffeine.cache.Cache;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.ipapi.IpDataFetchException;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URL;
import java.util.Map;

/**
 * Custom implementation of IpDataFetcher that allows fetching IP data from a specified API URL
 * with custom headers and parsing fields.
 */
public final class CustomIpDataFetcher extends IpDataFetcher {
    private final String apiUrl;
    private final Map<String, String> headers;
    private final Map<String, String> parseFields;

    /**
     * @param cache the cache to store fetched IP data
     * @param apiUrl the API URL to fetch IP data from (if the url contains %ip%, it will be replaced with the actual IP address, otherwise the IP will be appended to the URL)
     *              Example: "https://api.example.com/ip/%ip%" or "https://api.example.com/ip/"
     * @param headers the headers to include in the API request
     *                Example: Map.of("Authorization" , "Bearer YOUR_API_KEY")
     * @param parseFields the fields to parse from the JSON response
     */
    public CustomIpDataFetcher(Cache<@NotNull String, IpData> cache, String apiUrl, Map<String, String> headers, Map<String, String> parseFields) {
        super(cache);
        this.apiUrl = apiUrl;
        this.headers = headers;
        this.parseFields = parseFields;
    }

    @Override
    public IpData fetchIpData(String ip) throws IpDataFetchException {
        if (ip == null || ip.isEmpty()) {
            throw new IpDataFetchException("IP address cannot be null or empty");
        }

        if (getCache().getIfPresent(ip) != null) {
            return getCache().getIfPresent(ip);
        }

        try {
            String formattedUrl = getApiUrl().contains("%ip%") ? getApiUrl().replace("%ip%", ip) : getApiUrl() + ip;
            URL url = new URL(formattedUrl);
            String jsonResponse = fetchFromApi(url, headers);
            IpData data = parseIpData(jsonResponse);
            getCache().put(ip, data);
            return data;
        } catch (Exception e) {
            throw new IpDataFetchException("Failed to fetch IP data from " + getApiUrl() + ip, e);
        }
    }

    @Override
    protected String getApiUrl() {
        return apiUrl;
    }

    @Override
    protected IpData parseIpData(String jsonResponse) throws ParseException {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            throw new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN, "Empty JSON response");
        }

        // Parse the JSON response using the provided parseFields map
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonResponse);

        String country = (String) json.get(parseFields.get("country"));
        String countryCode = (String) json.get(parseFields.get("countryCode"));
        String regionName = (String) json.get(parseFields.get("regionName"));
        String region = (String) json.get(parseFields.get("region"));
        String city = (String) json.get(parseFields.get("city"));
        double lat = ((Number) json.get(parseFields.get("lat"))).doubleValue();
        double lon = ((Number) json.get(parseFields.get("lon"))).doubleValue();
        String timezone = (String) json.get(parseFields.get("timezone"));
        String isp = (String) json.get(parseFields.get("isp"));
        String org = (String) json.get(parseFields.get("org"));
        String asn = (String) json.get(parseFields.get("asn"));
        String ip = (String) json.get(parseFields.get("ip"));

        return new IpData(country, countryCode, regionName, region, city, lat, lon, timezone, isp, org, asn, ip);
    }
}
