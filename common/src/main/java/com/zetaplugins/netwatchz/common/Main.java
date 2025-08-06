package com.zetaplugins.netwatchz.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.ipapi.fetchers.CustomIpDataFetcher;
import com.zetaplugins.netwatchz.common.ipapi.fetchers.IpDataFetcher;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Cache<@NotNull String, IpData> cache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();

        IpDataFetcher ipDataFetcher = new CustomIpDataFetcher(cache, "http://ip-api.com/json/", null, new HashMap<>() {{
            put("country", "country");
            put("countryCode", "countryCode");
            put("regionName", "region");
            put("region", "regionName");
            put("city", "city");
            put("lat", "lat");
            put("lon", "lon");
            put("isp", "isp");
            put("org", "org");
        }});
        try {
            String ip = "146.70.231.25"; // Example IP address
            var ipData = ipDataFetcher.fetchIpData(ip);
            ipDataFetcher.fetchIpData(ip);
            System.out.println("IP Data for: " + ip);
            System.out.println("Country: " + ipData.country());
            System.out.println("Country Code: " + ipData.countryCode());
            System.out.println("Region: " + ipData.regionCode());
            System.out.println("City: " + ipData.city());
            System.out.println("Latitude: " + ipData.lat());
            System.out.println("Longitude: " + ipData.lon());
            System.out.println("ISP: " + ipData.isp());
            System.out.println("Organization: " + ipData.org());
        } catch (Exception e) {
            System.err.println("Error fetching IP data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}