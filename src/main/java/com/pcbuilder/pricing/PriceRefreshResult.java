package com.pcbuilder.pricing;

import java.time.Instant;




public record PriceRefreshResult(
        int trustedLiveCount,
        int reviewOnlyCount,
        int failedCount,
        Instant fetchedAt
) {
    public int successCount() {
        return trustedLiveCount + reviewOnlyCount;
    }
}
