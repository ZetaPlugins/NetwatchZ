package com.zetaplugins.netwatchz.common.vpnblock.providers;

import com.github.benmanes.caffeine.cache.Cache;
import com.zetaplugins.netwatchz.common.vpnblock.VpnInfoData;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class VpnApi extends VpnInfoProvider {
    private final String apiKey;

    public VpnApi(Cache<@NotNull String, VpnInfoData> cache, String apiKey) {
        super(cache);
        this.apiKey = apiKey;
    }

    @Override
    protected String getApiUrl() {
        return "https://vpnapi.io/api/%ip%?key=" + apiKey;
    }

    @Override
    protected VpnInfoData parseVpnData(String jsonResponse) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonResponse);

        System.out.println(json.toJSONString());

        JSONObject security = (JSONObject) json.get("security");
        if (security == null) {
            throw new ParseException(ParseException.ERROR_UNEXPECTED_TOKEN, "Missing 'security' object");
        }

        return new VpnInfoData(
                (Boolean) security.get("vpn"),
                (Boolean) security.get("proxy"),
                (Boolean) security.get("tor"),
                (Boolean) security.get("relay"),
                false
        );
    }
}
