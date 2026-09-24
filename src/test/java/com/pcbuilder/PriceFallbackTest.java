package com.pcbuilder;

import com.pcbuilder.pricing.LivePriceService;
import com.pcbuilder.pricing.PriceConfidence;
import com.pcbuilder.pricing.PriceQuote;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PriceFallbackTest {
    @Test void unverifiedQuotesNeverReplaceCatalogueTotals() {
        LivePriceService prices = new LivePriceService();
        assertEquals(200.0, prices.resolvePrice("cpu", 200.0));
        for (PriceConfidence confidence : new PriceConfidence[] {
                PriceConfidence.MEDIUM, PriceConfidence.LOW, PriceConfidence.FALLBACK}) {
            prices.putQuote(new PriceQuote("cpu", 1.0, "Fixture", "https://example.com/item",
                    "CPU", 1, Instant.EPOCH, true, confidence, "Test fixture"));
            assertEquals(200.0, prices.resolvePrice("cpu", 200.0));
            assertFalse(prices.hasTrustedLivePrice("cpu"));
        }
    }

    @Test void trustedQuoteIsUsedUntilCacheIsCleared() {
        LivePriceService prices = new LivePriceService();
        prices.putQuote(new PriceQuote("cpu", 180.0, "Fixture", "https://example.com/item",
                "CPU", 1, Instant.EPOCH, true, PriceConfidence.EXACT, "Test fixture"));
        assertEquals(180.0, prices.resolvePrice("cpu", 200.0));
        prices.clearQuotes();
        assertEquals(200.0, prices.resolvePrice("cpu", 200.0));
    }
}
