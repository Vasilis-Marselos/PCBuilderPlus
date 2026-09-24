package com.pcbuilder.pricing;

import java.time.Instant;




public record PriceQuote(
        String partId,
        double priceEur,
        String sourceName,
        String productUrl,
        String matchedTitle,
        Integer storeCount,
        Instant fetchedAt,
        boolean live,
        PriceConfidence confidence,
        String matchReason
) {
    public boolean usableForTotals() {
        return confidence != null && confidence.usableForTotals();
    }
}
