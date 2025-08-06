package com.zetaplugins.netwatchz.common.ipapi;

import org.jetbrains.annotations.NotNull;

public record IpData(
    String country,
    String countryCode,
    String regionName,
    String regionCode,
    String city,
    double lat,
    double lon,
    String timezone,
    String isp,
    String org,
    String asn,
    String ip
) {
    public @NotNull String toString() {
        return "IpData{" +
                "country='" + country + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", regionName='" + regionName + '\'' +
                ", regionCode='" + regionCode + '\'' +
                ", city='" + city + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                ", timezone='" + timezone + '\'' +
                ", isp='" + isp + '\'' +
                ", org='" + org + '\'' +
                ", asn='" + asn + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
