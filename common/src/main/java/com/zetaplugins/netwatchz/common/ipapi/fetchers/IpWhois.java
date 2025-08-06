package com.zetaplugins.netwatchz.common.ipapi.fetchers;

import com.github.benmanes.caffeine.cache.Cache;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Fetches IP data from the IpWhois API.
 */
public final class IpWhois extends IpDataFetcher {
    private static final String API_URL = "https://ipwhois.app/json/";

    public IpWhois(Cache<@NotNull String, IpData> cache) {
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

        boolean success = (Boolean) json.get("success");
        if (!success) return null;

        return new IpData(
                (String) json.get("country"),
                (String) json.get("country_code"),
                (String) json.get("region"),
                (String) json.get("region"),
                (String) json.get("city"),
                ((Number) json.get("latitude")).doubleValue(),
                ((Number) json.get("longitude")).doubleValue(),
                (String) json.get("timezone"),
                (String) json.get("isp"),
                (String) json.get("org"),
                (String) json.get("asn"),
                (String) json.get("ip")
        );
    }

    // Implement the fetchFromApi and parseIpData methods similar to IpApiCom
}
