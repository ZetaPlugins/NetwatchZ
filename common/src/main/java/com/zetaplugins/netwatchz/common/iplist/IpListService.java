package com.zetaplugins.netwatchz.common.iplist;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Service that manages multiple IpListManagers.
 */
public final class IpListService {
    private final List<IpListManager> ipListManagers;

    /**
     * Creates an IpListService that manages multiple IpListManagers, each initialized with a path from the provided list.
     * @param listPaths list of paths to IP list files
     * @param logger logger for logging messages
     */
    public IpListService(List<Path> listPaths, Logger logger) {
        this.ipListManagers = listPaths.stream()
            .map(path -> {
                try {
                    return new IpListManager(path, logger, false);
                } catch (Exception e) {
                    logger.warning("Failed to load IP list from " + path + ": " + e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Checks if the given IP address is in any of the managed IP lists.
     * @param ip the IP address to check
     * @return true if the IP is in any list, false otherwise
     */
    public boolean isIpInAnyList(String ip) {
        for (IpListManager manager : ipListManagers) {
            if (manager.isIpInList(ip)) return true;
        }
        return false;
    }
}
