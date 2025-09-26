package com.zetaplugins.netwatchz.common.iplist;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a list of CIDR ranges loaded from a text file and answers membership queries.
 * Format: each line is "a.b.c.d/prefix". Blank lines and lines starting with '#' are ignored.
 */
public final class IpListManager {
    private final Path listPath;
    private final Logger logger;
    private final boolean readFileOnEachCheck;

    private volatile List<Range> mergedRanges = Collections.emptyList();

    /**
     * Create manager that loads once (and caches). You should call reload() at startup
     * or when the file changes.
     * @param listPath path to CIDR list file
     * @param logger logger for messages
     * @throws IOException if initial load fails
     */
    public IpListManager(Path listPath, Logger logger) throws IOException {
        this(listPath, logger, false);
    }

    /**
     * Create manager.
     * @param listPath path to CIDR list file
     * @param logger logger for messages
     * @param readFileOnEachCheck if true, the manager will read & parse the file on every call to isIpInList().
     *                            This is simple but slow for frequent checks; prefer false and call reload()
     *                            when the file changes.
     * @throws IOException if initial load fails
     */
    public IpListManager(Path listPath, Logger logger, boolean readFileOnEachCheck) throws IOException {
        this.listPath = listPath;
        this.logger = logger;
        this.readFileOnEachCheck = readFileOnEachCheck;
        // initial load
        reload();
    }

    public Path getListPath() {
        return listPath;
    }

    /**
     * Reload the CIDR file, parse, sort and merge ranges into memory.
     */
    public synchronized void reload() throws IOException {
        List<Range> ranges = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(listPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                Range r = cidrToRange(line);
                ranges.add(r);
            }
        }

        if (ranges.isEmpty()) {
            mergedRanges = Collections.emptyList();
            return;
        }

        ranges.sort(Comparator.comparingLong(r -> r.start));

        // merge overlapping/adjacent
        List<Range> merged = new ArrayList<>();
        Range cur = ranges.get(0);
        for (int i = 1; i < ranges.size(); i++) {
            Range nxt = ranges.get(i);
            if (nxt.start <= cur.end + 1) {
                cur = new Range(cur.start, Math.max(cur.end, nxt.end));
            } else {
                merged.add(cur);
                cur = nxt;
            }
        }
        merged.add(cur);

        mergedRanges = Collections.unmodifiableList(merged);
    }

    /**
     * Check if the provided IPv4 address is inside any of the CIDR ranges.
     * Only IPv4 dotted-decimal addresses supported (e.g. "1.2.3.4").
     *
     * If readFileOnEachCheck==true, the file will be reloaded for each call (slow).
     */
    public boolean isIpInList(String ip) {
        if (readFileOnEachCheck) {
            try {
                reload();
            } catch (IOException e) {
                logger.log(Level.WARNING,"Failed to reload IP list from " + listPath + ": " + e.getMessage(), e);
                return false;
            }
        }

        long ipVal;
        try {
            ipVal = ipToLong(ip);
        } catch (IllegalArgumentException e) {
            return false;
        }

        List<Range> list = mergedRanges;
        if (list.isEmpty()) return false;

        // binary search by range
        int lo = 0, hi = list.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            Range r = list.get(mid);
            if (ipVal < r.start) {
                hi = mid - 1;
            } else if (ipVal > r.end) {
                lo = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * A range of IPs from start to end, inclusive.
     *
     * @param start inclusive, stored unsigned in a long
     * @param end inclusive
     */
    private record Range(long start, long end) {}

    /**
     * Convert IPv4 string to unsigned long.
     * @param ip IPv4 string like "a.b.c.d"
     * @return unsigned long representation
     * @throws IllegalArgumentException if invalid format
     */
    private static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) throw new IllegalArgumentException("Invalid IPv4: " + ip);
        long val = 0;
        for (String p : parts) {
            int b = Integer.parseInt(p);
            if (b < 0 || b > 255) throw new IllegalArgumentException("Invalid IPv4 octet: " + p);
            val = (val << 8) | (b & 0xFF);
        }
        return val & 0xFFFFFFFFL;
    }

    /**
     * Convert CIDR string to Range.
     * @param cidr CIDR string like "a.b.c.d/prefix"
     * @return Range object
     * @throws IllegalArgumentException if invalid format
     */
    private static Range cidrToRange(String cidr) {
        String[] parts = cidr.split("/");
        long base = ipToLong(parts[0].trim());
        int prefix;
        if (parts.length == 1) {// No prefix -> assume /32 (single host)
            prefix = 32;
        } else if (parts.length == 2) {
            prefix = Integer.parseInt(parts[1].trim());
        } else {
            throw new IllegalArgumentException("Invalid CIDR: " + cidr);
        }

        if (prefix < 0 || prefix > 32) throw new IllegalArgumentException("Invalid prefix: " + prefix);
        long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        long start = base & mask;
        long end = start | (~mask & 0xFFFFFFFFL);
        return new Range(start, end);
    }
}