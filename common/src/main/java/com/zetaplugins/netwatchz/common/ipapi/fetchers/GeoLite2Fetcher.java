package com.zetaplugins.netwatchz.common.ipapi.fetchers;

import com.github.benmanes.caffeine.cache.Cache;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import com.zetaplugins.netwatchz.common.ipapi.IpDataFetchException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import java.util.zip.GZIPInputStream;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Fetcher that downloads and uses GeoLite2 ASN + City + Country databases, then merges results into a single IpData.
 *
 * Supports both direct .mmdb URLs and .tar.gz URLs (official MaxMind endpoints).
 */
public final class GeoLite2Fetcher extends IpDataFetcher {
    private final Logger logger;

    private static final String ASN_MMDB = "GeoLite2-ASN.mmdb";
    private static final String CITY_MMDB = "GeoLite2-City.mmdb";
    private static final String COUNTRY_MMDB = "GeoLite2-Country.mmdb";

    private final Path dataDir;
    private final Duration updateInterval;
    private final String asnUrl;
    private final String cityUrl;
    private final String countryUrl;

    private volatile DatabaseReader asnReader;
    private volatile DatabaseReader cityReader;
    private volatile DatabaseReader countryReader;

    private volatile Instant lastUpdatedAsn = Instant.EPOCH;
    private volatile Instant lastUpdatedCity = Instant.EPOCH;
    private volatile Instant lastUpdatedCountry = Instant.EPOCH;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * Full constructor.
     * @param logger Logger to use for messages
     * @param cache The cache to use
     * @param dataDir Directory to store mmdb files
     * @param updateInterval How often to check for updated mmdb files
     * @param asnUrl URL to download ASN DB (.mmdb or .tar.gz)
     * @param cityUrl URL to download City DB (.mmdb or .tar.gz)
     * @param countryUrl URL to download Country DB (.mmdb or .tar.gz)
     */
    public GeoLite2Fetcher(
            Logger logger,
            Cache<@NotNull String, IpData> cache,
            Path dataDir,
            Duration updateInterval,
            String asnUrl,
            String cityUrl,
            String countryUrl
    ) {
        super(cache);
        this.logger = Objects.requireNonNull(logger, "logger");
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
        this.updateInterval = Objects.requireNonNull(updateInterval, "updateInterval");
        this.asnUrl = Objects.requireNonNull(asnUrl, "asnUrl");
        this.cityUrl = Objects.requireNonNull(cityUrl, "cityUrl");
        this.countryUrl = Objects.requireNonNull(countryUrl, "countryUrl");

        if (updateInterval.isNegative() || updateInterval.isZero()) {
            throw new IllegalArgumentException("updateInterval must be positive");
        }

        try {
            ensureDatabasesReady();
        } catch (Exception e) {
            // Ignore; will try again on first lookup
        }
    }

    // Unused, but required by base class
    @Override
    protected String getApiUrl() {
        return "mmdb://local/geolite-all/";
    }

    // Unused, but required by base class
    @Override
    protected IpData parseIpData(String jsonResponse) throws ParseException {
        throw new UnsupportedOperationException("parseIpData is not used by GeoLite2Fetcher.");
    }

    @Override
    public IpData fetchIpData(String ip) throws IpDataFetchException {
        if (ip == null || ip.isEmpty()) throw new IpDataFetchException("IP address cannot be null or empty");

        IpData cached = getCache().getIfPresent(ip);
        if (cached != null) return cached;

        try {
            ensureDatabasesReady();

            InetAddress addr = InetAddress.getByName(ip);

            rwLock.readLock().lock();
            try {
                String countryName = null;
                String countryCode = null;
                String regionName = null;
                String regionCode = null;
                String cityName = null;
                Double lat = null;
                Double lon = null;
                String timezone = null;
                String isp = null; // not available in GeoLite2
                String org = null;
                String asnStr = null;

                if (asnReader != null) {
                    try {
                        AsnResponse asn = asnReader.asn(addr);
                        if (asn != null) {
                            Long n = asn.getAutonomousSystemNumber();
                            asnStr = n == null ? null : ("AS" + n);
                            org = asn.getAutonomousSystemOrganization();
                        }
                    } catch (AddressNotFoundException ignored) {
                        // No ASN info for this IP
                    }
                }

                boolean cityFilled = false;
                if (cityReader != null) {
                    try {
                        CityResponse city = cityReader.city(addr);
                        if (city != null) {
                            if (city.getCountry() != null) {
                                countryName = city.getCountry().getName();
                                countryCode = city.getCountry().getIsoCode();
                            }
                            if (city.getMostSpecificSubdivision() != null) {
                                regionName = city.getMostSpecificSubdivision().getName();
                                regionCode = city.getMostSpecificSubdivision().getIsoCode();
                            }
                            if (city.getCity() != null) {
                                cityName = city.getCity().getName();
                            }
                            if (city.getLocation() != null) {
                                lat = city.getLocation().getLatitude();
                                lon = city.getLocation().getLongitude();
                                timezone = city.getLocation().getTimeZone();
                            }
                            cityFilled = true;
                        }
                    } catch (AddressNotFoundException ignored) {
                        // Not found in City DB
                    }
                }

                // Country fallback (if City didn't fill country fields)
                if (!cityFilled && countryReader != null) {
                    try {
                        CountryResponse c = countryReader.country(addr);
                        if (c != null && c.getCountry() != null) {
                            countryName = c.getCountry().getName();
                            countryCode = c.getCountry().getIsoCode();
                        }
                    } catch (AddressNotFoundException ignored) {
                        // No country info
                    }
                }

                IpData result = new IpData(
                        countryName,
                        countryCode,
                        regionName,
                        regionCode,
                        cityName,
                        lat == null ? 0.0 : lat,
                        lon == null ? 0.0 : lon,
                        timezone,
                        isp, // GeoLite2 doesn't include ISP so remains null
                        org,
                        asnStr,
                        ip
                );

                getCache().put(ip, result);
                return result;

            } finally {
                rwLock.readLock().unlock();
            }

        } catch (IOException | GeoIp2Exception e) {
            throw new IpDataFetchException("Failed GeoLite (ASN/City/Country) lookup for " + ip, e);
        }
    }

    /**
     * Ensures that the mmdb databases are downloaded and the readers are opened and fresh.
     * @throws IOException if an I/O error occurs
     * @throws IpDataFetchException if a download or extraction error occurs
     */
    private void ensureDatabasesReady() throws IOException, IpDataFetchException {
        if (readersFresh()) return;

        rwLock.writeLock().lock();
        try {
            if (readersFresh()) return;

            Files.createDirectories(dataDir);

            Path asnPath = dataDir.resolve(ASN_MMDB);
            Path cityPath = dataDir.resolve(CITY_MMDB);
            Path countryPath = dataDir.resolve(COUNTRY_MMDB);

            if (needsDownload(asnPath, lastUpdatedAsn)) {
                downloadSmart(asnUrl, asnPath, ASN_MMDB);
                lastUpdatedAsn = Instant.now();
            } else {
                lastUpdatedAsn = Files.getLastModifiedTime(asnPath).toInstant();
            }

            if (needsDownload(cityPath, lastUpdatedCity)) {
                downloadSmart(cityUrl, cityPath, CITY_MMDB);
                lastUpdatedCity = Instant.now();
            } else {
                lastUpdatedCity = Files.getLastModifiedTime(cityPath).toInstant();
            }

            if (needsDownload(countryPath, lastUpdatedCountry)) {
                downloadSmart(countryUrl, countryPath, COUNTRY_MMDB);
                lastUpdatedCountry = Instant.now();
            } else {
                lastUpdatedCountry = Files.getLastModifiedTime(countryPath).toInstant();
            }

            // close old readers (if any) before reopening
            closeQuietly(asnReader);
            closeQuietly(cityReader);
            closeQuietly(countryReader);

            asnReader = Files.exists(asnPath)
                    ? new DatabaseReader.Builder(asnPath.toFile()).withCache(new CHMCache()).build()
                    : null;

            cityReader = Files.exists(cityPath)
                    ? new DatabaseReader.Builder(cityPath.toFile()).withCache(new CHMCache()).build()
                    : null;

            countryReader = Files.exists(countryPath)
                    ? new DatabaseReader.Builder(countryPath.toFile()).withCache(new CHMCache()).build()
                    : null;

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private boolean readersFresh() {
        Instant now = Instant.now();
        boolean asnOk     = asnReader     != null && now.isBefore(lastUpdatedAsn.plus(updateInterval));
        boolean cityOk    = cityReader    != null && now.isBefore(lastUpdatedCity.plus(updateInterval));
        boolean countryOk = countryReader != null && now.isBefore(lastUpdatedCountry.plus(updateInterval));
        return asnOk && cityOk && countryOk;
    }

    private boolean needsDownload(Path p, Instant lastUpdated) throws IOException {
        if (Files.notExists(p)) return true;
        Instant mtime = Files.getLastModifiedTime(p).toInstant();
        return mtime.isBefore(Instant.now().minus(updateInterval));
    }

    /**
     * Downloads either a .mmdb directly or a .tar.gz and extracts the first matching .mmdb.
     * @param urlStr Source URL (.mmdb or .tar.gz)
     * @param dest Destination path for the .mmdb
     * @param mmdbNameHint Preferred filename (e.g., "GeoLite2-City.mmdb") to select from tarball
     */
    private void downloadSmart(String urlStr, Path dest, String mmdbNameHint) throws IOException, IpDataFetchException {
        String lower = urlStr.toLowerCase();
        if (lower.endsWith(".mmdb")) {
            downloadDirect(urlStr, dest);
        } else if (lower.endsWith("tar.gz")) {
            downloadAndExtractTarGz(urlStr, dest, mmdbNameHint);
        } else {
            Path tmp = Files.createTempFile("geolite-", ".bin");
            try {
                httpDownload(new URL(urlStr), tmp);
                // Try reading as tar.gz; if it fails, assume raw mmdb
                try (InputStream fis = Files.newInputStream(tmp);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    bis.mark(4);
                    int b1 = bis.read();
                    int b2 = bis.read();
                    int b3 = bis.read();
                    int b4 = bis.read();
                    bis.reset();
                    boolean looksGzip = (b1 == 0x1f && b2 == 0x8b && b3 == 0x08);
                    if (looksGzip) {
                        try (GZIPInputStream gis = new GZIPInputStream(bis);
                             TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {
                            extractMmdbFromTarStream(tis, dest, mmdbNameHint);
                        }
                    } else {
                        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    }
                }
            } finally {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
        }
    }

    private void downloadDirect(String urlStr, Path dest) throws IOException, IpDataFetchException {
        logger.info("Downloading GeoLite2 mmdb from " + urlStr + " ...");
        var start = Instant.now();
        Path tmp = Files.createTempFile("geolite-", ".mmdb");
        try {
            httpDownload(new URL(urlStr), tmp);
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            var duration = Duration.between(start, Instant.now()).toMillis();
            logger.info("Downloaded GeoLite2 mmdb from " + urlStr + " in " + duration + " ms.");
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }

    private void downloadAndExtractTarGz(String urlStr, Path dest, String mmdbNameHint) throws IOException, IpDataFetchException {
        logger.info("Downloading GeoLite2 tar.gz from " + urlStr + " ...");
        var start = Instant.now();
        Path tmp = Files.createTempFile("geolite-", ".tar.gz");
        try {
            httpDownload(new URL(urlStr), tmp);
            try (InputStream fis = Files.newInputStream(tmp);
                 GZIPInputStream gis = new GZIPInputStream(fis);
                 TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {
                extractMmdbFromTarStream(tis, dest, mmdbNameHint);
                var duration = Duration.between(start, Instant.now()).toMillis();
                logger.info("Downloaded and extracted GeoLite2 mmdb from " + urlStr + " in " + duration + " ms." );
            }
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }

    private void extractMmdbFromTarStream(TarArchiveInputStream tis, Path dest, String mmdbNameHint) throws IOException, IpDataFetchException {
        TarArchiveEntry entry;
        Path tmpOut = Files.createTempFile("geolite-extract-", ".mmdb");
        boolean found = false;

        try {
            while ((entry = tis.getNextTarEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                boolean isMmdb = name.endsWith(".mmdb");
                boolean matchesHint = name.endsWith(mmdbNameHint);
                if (isMmdb && (matchesHint || mmdbNameHint == null)) {
                    try (OutputStream os = Files.newOutputStream(tmpOut, StandardOpenOption.TRUNCATE_EXISTING)) {
                        tis.transferTo(os);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IpDataFetchException("MMDB file not found in tarball (looked for: " + mmdbNameHint + ").");
            }
            Files.move(tmpOut, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            try { Files.deleteIfExists(tmpOut); } catch (Exception ignored) {}
        }
    }

    private void httpDownload(URL url, Path dest) throws IOException, IpDataFetchException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(20000);

        int code = conn.getResponseCode();
        if (code >= 400) {
            String err = readStream(conn.getErrorStream());
            throw new IpDataFetchException("Failed to download " + url + " (HTTP " + code + ") – " + err);
        }

        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }
    }

    private static String readStream(InputStream in) throws IOException {
        if (in == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static void closeQuietly(Closeable c) {
        if (c == null) return;
        try { c.close(); } catch (IOException ignored) {}
    }

    @Override
    public void onShutDown() {
        rwLock.writeLock().lock();
        try {
            closeQuietly(asnReader);
            closeQuietly(cityReader);
            closeQuietly(countryReader);
            asnReader = null;
            cityReader = null;
            countryReader = null;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}