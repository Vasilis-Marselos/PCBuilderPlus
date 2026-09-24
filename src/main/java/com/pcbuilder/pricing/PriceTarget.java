package com.pcbuilder.pricing;




public record PriceTarget(
        String partId,
        MarketplaceCategory category,
        String displayName,
        String manufacturerPartNumber,
        String searchQuery,
        String skroutzUrl,
        double fallbackPriceEur
) {
}
