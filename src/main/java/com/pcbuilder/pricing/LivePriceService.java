package com.pcbuilder.pricing;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;







public class LivePriceService {

    private final SkroutzPriceFetcher skroutzPriceFetcher = new SkroutzPriceFetcher();
    private final Map<String, PriceQuote> cache = new ConcurrentHashMap<>();
    private volatile Instant lastRefreshAt;

    public PriceRefreshResult refreshPrices(List<PriceTarget> targets) {
        int trustedLiveCount = 0;
        int reviewOnlyCount = 0;
        int failedCount = 0;

        for (PriceTarget target : targets) {
            try {
                Optional<PriceQuote> quote = skroutzPriceFetcher.fetchLowestPrice(target);
                if (quote.isPresent()) {
                    cache.put(target.partId(), quote.get());
                    if (quote.get().usableForTotals()) {
                        trustedLiveCount++;
                    } else {
                        reviewOnlyCount++;
                    }
                } else {
                    cache.remove(target.partId());
                    failedCount++;
                }

                Thread.sleep(120L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Live price refresh was interrupted.", e);
            } catch (Exception e) {
                cache.remove(target.partId());
                failedCount++;
            }
        }

        lastRefreshAt = Instant.now();
        return new PriceRefreshResult(trustedLiveCount, reviewOnlyCount, failedCount, lastRefreshAt);
    }

    public Optional<PriceQuote> getQuote(String partId) {
        return Optional.ofNullable(cache.get(partId));
    }

    public void putQuote(PriceQuote quote) {
        if (quote != null) {
            cache.put(quote.partId(), quote);
            lastRefreshAt = Instant.now();
        }
    }

    public void clearQuotes() {
        cache.clear();
    }

    public void retainQuotesForPartIds(Set<String> partIds) {
        if (partIds == null || partIds.isEmpty()) {
            cache.clear();
            return;
        }
        cache.keySet().removeIf(partId -> !partIds.contains(partId));
    }

    public double resolvePrice(String partId, double fallbackPriceEur) {
        PriceQuote quote = cache.get(partId);
        return quote != null && quote.usableForTotals() ? quote.priceEur() : fallbackPriceEur;
    }

    public boolean hasTrustedLivePrice(String partId) {
        PriceQuote quote = cache.get(partId);
        return quote != null && quote.usableForTotals();
    }

    public boolean hasAnyLiveQuote(String partId) {
        return cache.containsKey(partId);
    }

    public Optional<Instant> getLastRefreshAt() {
        return Optional.ofNullable(lastRefreshAt);
    }

    public List<MarketplaceSearchResult> searchMarketplace(MarketplaceCategory category, String userQuery, int limit) {
        return skroutzPriceFetcher.searchProducts(category, userQuery, limit);
    }

    public boolean isLikelyProductUrl(String url) {
        return skroutzPriceFetcher.isLikelyProductUrl(url);
    }

    public Optional<MarketplaceSearchResult> importFromProductUrl(String productUrl) {
        return skroutzPriceFetcher.importFromProductUrl(productUrl);
    }

    public Optional<MarketplaceSearchResult> importFromRenderedProductPage(String productUrl, String renderedText) {
        return skroutzPriceFetcher.importFromRenderedProductPage(productUrl, renderedText);
    }
}
