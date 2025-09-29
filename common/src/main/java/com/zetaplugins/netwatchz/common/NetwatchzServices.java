package com.zetaplugins.netwatchz.common;

import com.zetaplugins.netwatchz.common.ipapi.fetchers.IpDataFetcher;
import com.zetaplugins.netwatchz.common.iplist.IpListFetcher;
import com.zetaplugins.netwatchz.common.iplist.IpListService;
import com.zetaplugins.netwatchz.common.vpnblock.providers.VpnInfoProvider;

/**
 * A container class for Netwatchz services.
 */
public record NetwatchzServices(
        IpDataFetcher ipDataFetcher,
        IpListService ipListService,
        IpListFetcher ipListFetcher,
        VpnInfoProvider vpnInfoProvider
) {}
