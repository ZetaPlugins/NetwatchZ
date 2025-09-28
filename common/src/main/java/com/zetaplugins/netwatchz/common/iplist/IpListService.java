package com.zetaplugins.netwatchz.common.iplist;

import com.zetaplugins.netwatchz.common.config.IpListConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
     * Creates an IpListService from the given configuration.
     * @param cfg configuration containing list names and directory
     * @param logger logger for logging messages
     * @return IpListService instance
     */
    public static IpListService fromConfig(IpListConfig cfg, Logger logger) {
        return new IpListService(cfg.listNames().stream()
                .map(cfg.ipListsDir()::resolve)
                .collect(Collectors.toList()), logger);
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
