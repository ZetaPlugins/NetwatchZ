package com.zetaplugins.netwatchz.common.ipapi.fetchers;

import com.github.benmanes.caffeine.cache.Cache;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Fetches IP data from the ip-api.com service.
 */
public final class IpApiCom extends IpDataFetcher {
    private static final String API_URL = "http://ip-api.com/json/";

    public IpApiCom(Cache<@NotNull String, IpData> cache) {
        super(cache);
    }

    @Override
    protected String getApiUrl() {
        return API_URL;
    }

    @Override
    protected IpData parseIpData(String jsonResponse) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonResponse);

        boolean success = json.get("status").equals("success");
        if (!success) return null;

        return new IpData(
            (String) json.get("country"),
            (String) json.get("countryCode"),
            (String) json.get("regionName"),
            (String) json.get("region"),
            (String) json.get("city"),
            ((Number) json.get("lat")).doubleValue(),
            ((Number) json.get("lon")).doubleValue(),
            (String) json.get("timezone"),
            (String) json.get("isp"),
            (String) json.get("org"),
            (String) json.get("as"),
            (String) json.get("query")
        );
    }
}
