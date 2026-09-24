package com.pcbuilder.pricing;

public record MarketplaceSearchResult(
        MarketplaceCategory category,
        String title,
        double priceEur,
        String productUrl,
        PriceConfidence confidence,
        String matchReason,
        String specsSummary,
        Object importedPart,
        PriceQuote seedQuote
) {
    public boolean importable() {
        return importedPart != null;
    }

    public String priceText() {
        return priceEur > 0 ? String.format("€%.2f", priceEur) : "–";
    }

    public String confidenceText() {
        return confidence != null ? confidence.label() : "unknown";
    }

    public String importStatus() {
        return importable() ? "ready" : "needs manual mapping";
    }
}
