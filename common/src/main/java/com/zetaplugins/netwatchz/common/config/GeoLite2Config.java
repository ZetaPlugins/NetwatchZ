package com.zetaplugins.netwatchz.common.config;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Configuration for GeoLite2
 * @param asnUrl URL for GeoLite2 ASN database
 * @param cityUrl URL for GeoLite2 City database
 * @param countryUrl URL for GeoLite2 Country database
 * @param storageDir Directory to store GeoLite2 databases locally
 * @param updateIntervalDays Interval to update GeoLite2 databases
 */
public record GeoLite2Config(String asnUrl, String cityUrl, String countryUrl, Path storageDir,
                             Duration updateIntervalDays) {
}
