package com.zetaplugins.netwatchz.common.vpnblock.providers;

import com.github.benmanes.caffeine.cache.Cache;
import com.zetaplugins.netwatchz.common.DataFetchException;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.vpnblock.VpnInfoData;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URL;
import java.util.Map;

public final class CustomVpnInfoProvider extends VpnInfoProvider {
    private final String apiUrl;
    private final Map<String, String> headers;
    private final Map<String, String> parseFields;

    public CustomVpnInfoProvider(Cache<@NotNull String, VpnInfoData> cache, String url, Map<String, String> headers, Map<String, String> parseFields) {
        super(cache);
        this.apiUrl = url;
        this.headers = headers;
        this.parseFields = parseFields;
    }

    @Override
    protected String getApiUrl() {
        return apiUrl;
    }

    @Override
    public VpnInfoData fetchVpnData(String ip) throws DataFetchException {
        if (ip == null || ip.isEmpty()) {
            throw new DataFetchException("IP address cannot be null or empty");
        }

        if (getCache().getIfPresent(ip) != null) {
            return getCache().getIfPresent(ip);
        }

        try {
            String formattedUrl = getApiUrl().contains("%ip%") ? getApiUrl().replace("%ip%", ip) : getApiUrl() + ip;
            URL url = new URL(formattedUrl);
            String jsonResponse = fetchFromApi(url, headers);
            VpnInfoData data = parseVpnData(jsonResponse);
            getCache().put(ip, data);
            return data;
        } catch (Exception e) {
            throw new DataFetchException("Failed to fetch IP data from " + getApiUrl() + ip, e);
        }
    }

    @Override
    protected VpnInfoData parseVpnData(String jsonResponse) throws ParseException {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            throw new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN, "Empty JSON response");
        }

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonResponse);

        boolean vpn = getNestedBoolean(json, parseFields.get("vpn"));
        boolean proxy = getNestedBoolean(json, parseFields.get("proxy"));
        boolean tor = getNestedBoolean(json, parseFields.get("tor"));
        boolean relay = getNestedBoolean(json, parseFields.get("relay"));
        boolean hosting = getNestedBoolean(json, parseFields.get("hosting"));

        return new VpnInfoData(vpn, proxy, tor, relay, hosting);
    }

    private boolean getNestedBoolean(JSONObject json, String keyPath) {
        if (keyPath == null || keyPath.isEmpty()) return false;

        String[] keys = keyPath.split("\\.");
        JSONObject current = json;
        Object value = null;

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            value = current.get(key);

            if (value == null) return false;

            if (i < keys.length - 1) {
                if (!(value instanceof JSONObject)) return false;
                current = (JSONObject) value;
            }
        }

        if (value instanceof Boolean) return (Boolean) value;
        return false;
    }
}
