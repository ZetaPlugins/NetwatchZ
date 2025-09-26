package com.zetaplugins.netwatchz.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zetaplugins.netwatchz.common.ipapi.IpData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class CacheUtils {
    private CacheUtils() {}

    public static Cache<@NotNull String, IpData> createIpApiCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }
}
