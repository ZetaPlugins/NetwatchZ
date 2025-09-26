package com.zetaplugins.netwatchz.common.vpnblock.providers;

import com.github.benmanes.caffeine.cache.Cache;
import com.zetaplugins.netwatchz.common.vpnblock.VpnInfoData;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class ProxyCheck extends VpnInfoProvider {
    private final String apiKey;

    public ProxyCheck(Cache<@NotNull String, VpnInfoData> cache, String apiKey) {
        super(cache);
        this.apiKey = apiKey;
    }

    @Override
    protected String getApiUrl() {
        return "https://proxycheck.io/v3/%ip%?key=" + apiKey;
    }

    @Override
    protected VpnInfoData parseVpnData(String jsonResponse) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonResponse);

        String ipKey = (String) json.keySet().iterator().next();
        JSONObject ipData = (JSONObject) json.get(ipKey);
        JSONObject detections = (JSONObject) ipData.get("detections");

        return new VpnInfoData(
                (Boolean) detections.get("vpn"),
                (Boolean) detections.get("proxy"),
                (Boolean) detections.get("tor"),
                false,
                (Boolean) detections.get("hosting")
        );
    }
}
