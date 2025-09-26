package com.zetaplugins.netwatchz.common.config;

import com.zetaplugins.netwatchz.common.iplist.IpListFetchJob;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Configuration for IP lists
 * @param ipListsDir directory to store IP lists
 * @param fetchJobs jobs to fetch IP lists
 * @param listNames names of the IP lists
 */
public record IpListConfig(Path ipListsDir, List<IpListFetchJob> fetchJobs, Set<String> listNames) {
    public IpListConfig(Path ipListsDir, List<IpListFetchJob> fetchJobs, Set<String> listNames) {
        this.ipListsDir = ipListsDir;
        this.fetchJobs = List.copyOf(fetchJobs);
        this.listNames = Set.copyOf(listNames);
    }
}
