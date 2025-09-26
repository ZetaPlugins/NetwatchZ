package com.zetaplugins.netwatchz.common.iplist;

import java.nio.file.Path;
import java.time.Duration;

public record IpListFetchJob(String url, Path destination, Duration updateInterval) {}
