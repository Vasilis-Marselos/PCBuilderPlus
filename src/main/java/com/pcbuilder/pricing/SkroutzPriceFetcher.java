package com.pcbuilder.pricing;

import com.pcbuilder.CPU;
import com.pcbuilder.Cooler;
import com.pcbuilder.GPU;
import com.pcbuilder.Motherboard;
import com.pcbuilder.PCCase;
import com.pcbuilder.PSU;
import com.pcbuilder.RAM;
import com.pcbuilder.Storage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;












public class SkroutzPriceFetcher {

    private static final String SKROUTZ_BASE_URL = "https://www.skroutz.gr";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_CANDIDATE_URLS_PRICE = 8;
    private static final int MAX_CANDIDATE_URLS_DISCOVERY = 28;

    private static final Pattern PRODUCT_URL_PATTERN = Pattern.compile(
            "(?:https://www\\.skroutz\\.gr)?(/s/\\d+/[^\"'\\s<>]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LOW_PRICE_JSON_PATTERN = Pattern.compile(
            "\\\"lowPrice\\\"\\s*:\\s*\\\"?([0-9]+(?:\\.[0-9]{2})?)\\\"?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern STORES_PRICE_PATTERN = Pattern.compile(
            "από\\s*([0-9]{1,3}(?:[.\\s][0-9]{3})*(?:,[0-9]{2})?|[0-9]+(?:,[0-9]{2})?)\\s*€\\s*σε\\s*(\\d+)\\s*καταστήματα",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern SIMPLE_FROM_PRICE_PATTERN = Pattern.compile(
            "από\\s*([0-9]{1,3}(?:[.\\s][0-9]{3})*(?:,[0-9]{2})?|[0-9]+(?:,[0-9]{2})?)\\s*€",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern ANY_EURO_PRICE_PATTERN = Pattern.compile(
            "([0-9]{1,3}(?:[.\\s][0-9]{3})*(?:,[0-9]{2})?|[0-9]+(?:,[0-9]{2})?)\\s*€",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern STORAGE_VARIANT_PRICE_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(GB|TB)\\s+([0-9]{1,3}(?:[\\.,][0-9]{3})*(?:,[0-9]{2})|[0-9]+(?:,[0-9]{2})?)\\s*€",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern STORAGE_SPLIT_EURO_PRICE_PATTERN = Pattern.compile(
            "(?m)(\\d{2,4})\\s*(?:\\R|\\s{2,})\\s*(\\d{2})\\s*€",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern STORAGE_COMPACT_EURO_PRICE_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{2,4})(\\d{2})\\s*€",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<title>(.*?)</title>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern CPU_MODEL_PATTERN = Pattern.compile(
            "(Ryzen\\s+[3579]\\s+[0-9]{4,5}[A-Z0-9-]*|Core\\s+i[3579][- ]?[0-9]{4,5}[A-Z]{0,3}|Core\\s+Ultra\\s+[579]\\s*\\d{3}[A-Z]{0,2})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GPU_MODEL_PATTERN = Pattern.compile(
            "(RTX\\s*\\d{4}(?:\\s*Ti)?(?:\\s*SUPER)?|RX\\s*\\d{4}(?:\\s*XT|\\s*XTX)?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SOCKET_PATTERN = Pattern.compile("(AM5|AM4|LGA1700|LGA1851|TR5|sTR5)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CORES_PATTERN = Pattern.compile("(\\d{1,2})\\s*(?:Πυρήνων|cores?|Core[s]?)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern GHZ_PATTERN = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*GHz", Pattern.CASE_INSENSITIVE);
    private static final Pattern TDP_PATTERN = Pattern.compile("(\\d{2,4})\\s*W(?:att|\\s*TDP|\\s*TBP|\\s*GPU)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern VRAM_PATTERN = Pattern.compile("(\\d{1,2})\\s*GB", Pattern.CASE_INSENSITIVE);
    private static final Pattern KIT_PATTERN = Pattern.compile("(\\d)\\s*x\\s*(\\d{1,3})\\s*GB", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAM_TOTAL_GB_PATTERN = Pattern.compile(
            "(?:\\bRAM\\b\\s*)?(\\d{1,3})\\s*GB(?:\\s*RAM)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern RAM_SPEED_LABEL_PATTERN = Pattern.compile(
            "(?:DDR[345]\\s*|(?:RAM|Ταχύτητα|Speed)\\s*[:\\-]?\\s*)(\\d{4,5})\\s*(?:MHz|MT/s)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern SPEED_PATTERN = Pattern.compile("(?<!\\d)([4-9]\\d{3})(?!\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPACITY_TB_PATTERN = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*TB", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPACITY_GB_PATTERN = Pattern.compile("([0-9]{3,5})\\s*GB", Pattern.CASE_INSENSITIVE);
    private static final Pattern WATTAGE_PATTERN = Pattern.compile("(\\d{3,4})\\s*W", Pattern.CASE_INSENSITIVE);

    private static final Set<String> KNOWN_BRANDS = Set.of(
            "amd", "intel", "nvidia", "msi", "asus", "gigabyte", "corsair", "g.skill", "gskill",
            "samsung", "wd", "western digital", "crucial", "seagate", "seasonic", "be quiet", "bequiet",
            "kingston", "asrock", "cooler master", "fractal", "fractal design", "lian li", "nzxt",
            "thermaltake", "phanteks", "silverstone", "montech", "noctua", "thermalright", "arctic", "patriot"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isLikelyProductUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return PRODUCT_URL_PATTERN.matcher(url).find();
    }

    public Optional<MarketplaceSearchResult> importFromProductUrl(String productUrl) {
        if (!isLikelyProductUrl(productUrl)) {
            return Optional.empty();
        }

        Optional<MarketplaceSearchResult> directGpu = buildDirectGpuImportResult(productUrl, "");
        if (directGpu.isPresent()) {
            return directGpu;
        }

        String urlTitleForCategory = titleFromProductUrl(productUrl);
        if (looksLikeStorageProductUrl(productUrl) && !looksLikeGpuProduct(normalize(urlTitleForCategory + " " + productUrl))) {
            Optional<MarketplaceSearchResult> forcedStorage = buildRenderedStorageImportResult(
                    productUrl,
                    urlTitleForCategory,
                    ""
            );
            if (forcedStorage.isPresent()) {
                return forcedStorage;
            }
        }

        try {
            String html;
            try {
                html = fetchHtml(productUrl);
            } catch (Exception e) {
                html = "";
            }

            Document doc = Jsoup.parse(html != null ? html : "", productUrl);
            String pageTitle = extractTitle(doc, html != null ? html : "");
            if (pageTitle == null || pageTitle.isBlank()) {
                pageTitle = titleFromProductUrl(productUrl);
            }
            if (pageTitle == null || pageTitle.isBlank()) {
                return Optional.empty();
            }

            String pageText = doc.text();
            if (pageText == null || pageText.isBlank()) {
                pageText = pageTitle;
            }

            MarketplaceCategory category = inferExplicitProductCategory(pageTitle, pageText);
            if (category == null) {
                category = inferCategory(pageTitle, pageText);
            }
            if (category == null) {
                return Optional.empty();
            }

            PriceMatch match = category == MarketplaceCategory.STORAGE
                    ? extractStorageAwarePrice(doc, html != null ? html : "", pageTitle + " " + pageText)
                    : extractPrice(doc, html != null ? html : "");
            double importPrice = match != null ? match.priceEur() : 0.0;

            Object importedPart = buildImportedPart(category, pageTitle, pageText, importPrice, productUrl, pageTitle);
            if (importedPart == null) {
                return Optional.empty();
            }

            PriceQuote seedQuote = null;
            if (match != null && match.priceEur() > 0) {
                seedQuote = new PriceQuote(
                        buildImportPartId(category, pageTitle),
                        match.priceEur(),
                        "Skroutz",
                        productUrl,
                        pageTitle,
                        match.storeCount(),
                        Instant.now(),
                        true,
                        PriceConfidence.EXACT,
                        category == MarketplaceCategory.STORAGE
                                ? "Imported from Skroutz product page; storage capacity/variant price parsed when available"
                                : "Imported from explicit Skroutz product page"
                );
            }

            PriceConfidence confidence = match != null ? PriceConfidence.EXACT : PriceConfidence.LOW;
            String reason = match != null
                    ? (category == MarketplaceCategory.STORAGE
                    ? "Imported from Skroutz product page; storage capacity/variant price parsed when available"
                    : "Imported from explicit Skroutz product page")
                    : "Imported from explicit Skroutz product page; price was not visible in static HTML";

            return Optional.of(new MarketplaceSearchResult(
                    category,
                    pageTitle,
                    importPrice,
                    productUrl,
                    confidence,
                    reason,
                    buildSpecsSummary(importedPart),
                    importedPart,
                    seedQuote
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<MarketplaceSearchResult> importFromRenderedProductPage(String productUrl, String renderedText) {
        if (!isLikelyProductUrl(productUrl)) {
            return Optional.empty();
        }

        Optional<MarketplaceSearchResult> directGpu = buildDirectGpuImportResult(productUrl, renderedText != null ? renderedText : "");
        if (directGpu.isPresent()) {
            return directGpu;
        }

        Optional<MarketplaceSearchResult> samsung980Pro = buildSamsung980ProImportResult(productUrl, renderedText);
        if (samsung980Pro.isPresent()) {
            return samsung980Pro;
        }

        String urlTitleForCategory = titleFromProductUrl(productUrl);
        if (looksLikeStorageProductUrl(productUrl) && !looksLikeGpuProduct(normalize(urlTitleForCategory + " " + productUrl))) {
            Optional<MarketplaceSearchResult> forcedStorage = buildRenderedStorageImportResult(
                    productUrl,
                    bestTitleForRenderedImport(productUrl, renderedText != null ? renderedText : ""),
                    renderedText != null ? renderedText : ""
            );
            if (forcedStorage.isPresent()) {
                return forcedStorage;
            }
        }

        if (renderedText == null || renderedText.isBlank()) {
            String urlTitle = bestTitleForRenderedImport(productUrl, "");
            String combined = urlTitle + "\n" + productUrl;
            String normalizedCombined = normalize(combined);
            if (looksLikeStorageProduct(normalizedCombined) && !looksLikeGpuProduct(normalizedCombined)) {
                return buildRenderedStorageImportResult(productUrl, urlTitle, "");
            }
            return importFromProductUrl(productUrl);
        }

        String safeRenderedText = compactRenderedText(renderedText);

        try {
            String pageTitle = bestTitleForRenderedImport(productUrl, safeRenderedText);
            if (pageTitle == null || pageTitle.isBlank()) {
                return Optional.empty();
            }

            String combinedText = pageTitle + "\n" + safeRenderedText + "\n" + productUrl;
            MarketplaceCategory category = inferExplicitProductCategory(pageTitle, combinedText);
            if (category == null) {
                category = inferCategory(pageTitle, combinedText);
            }





            String normalizedCombinedText = normalize(combinedText);
            if (category == null && looksLikeStorageProduct(normalizedCombinedText) && !looksLikeGpuProduct(normalizedCombinedText)) {
                category = MarketplaceCategory.STORAGE;
            }

            if (category == null) {
                return Optional.empty();
            }

            if (category == MarketplaceCategory.STORAGE) {
                return buildRenderedStorageImportResult(productUrl, pageTitle, safeRenderedText);
            }

            Document renderedDoc = Jsoup.parse(safeRenderedText, productUrl);
            PriceMatch match = extractPrice(renderedDoc, safeRenderedText);
            double importPrice = match != null ? match.priceEur() : 0.0;

            Object importedPart = buildImportedPart(category, pageTitle, safeRenderedText, importPrice, productUrl, pageTitle);
            if (importedPart == null) {
                return Optional.empty();
            }

            PriceQuote seedQuote = null;
            if (match != null && match.priceEur() > 0) {
                seedQuote = new PriceQuote(
                        buildImportPartId(category, pageTitle),
                        match.priceEur(),
                        "Skroutz",
                        productUrl,
                        pageTitle,
                        match.storeCount(),
                        Instant.now(),
                        true,
                        PriceConfidence.EXACT,
                        "Imported from rendered Skroutz product page"
                );
            }

            PriceConfidence confidence = match != null ? PriceConfidence.EXACT : PriceConfidence.LOW;
            String reason = match != null
                    ? "Imported from rendered Skroutz product page"
                    : "Imported from rendered Skroutz product page; price was not visible";

            return Optional.of(new MarketplaceSearchResult(
                    category,
                    pageTitle,
                    importPrice,
                    productUrl,
                    confidence,
                    reason,
                    buildSpecsSummary(importedPart),
                    importedPart,
                    seedQuote
            ));
        } catch (Exception e) {



            Optional<MarketplaceSearchResult> storageFallback = buildRenderedStorageImportResult(
                    productUrl,
                    bestTitleForRenderedImport(productUrl, safeRenderedText),
                    safeRenderedText
            );
            if (storageFallback.isPresent()) {
                return storageFallback;
            }
            return importFromProductUrl(productUrl);
        }
    }

    private Optional<MarketplaceSearchResult> buildRenderedStorageImportResult(String productUrl,
                                                                               String pageTitle,
                                                                               String renderedText) {
        String title = pageTitle;
        if (title == null || title.isBlank()) {
            title = bestTitleForRenderedImport(productUrl, renderedText);
        }
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        if (looksLikeGpuProduct(normalize(title + " " + productUrl))) {
            return Optional.empty();
        }

        String combined = title + "\n" + (renderedText != null ? renderedText : "") + "\n" + productUrl;
        if (!looksLikeStorageProduct(normalize(combined)) && !looksLikeStorageProductUrl(productUrl)) {
            return Optional.empty();
        }

        PriceMatch match = extractStorageAwarePrice(Jsoup.parse(renderedText != null ? renderedText : "", productUrl),
                renderedText != null ? renderedText : "", combined);
        double importPrice = match != null ? match.priceEur() : 0.0;

        Storage importedStorage = buildStorage(title, combined, importPrice, productUrl, title);
        PriceQuote seedQuote = null;
        if (importPrice > 0.0) {
            seedQuote = new PriceQuote(
                    importedStorage.id(),
                    importPrice,
                    "Skroutz",
                    productUrl,
                    title,
                    match != null ? match.storeCount() : null,
                    Instant.now(),
                    true,
                    PriceConfidence.EXACT,
                    "Imported SSD/NVMe from rendered Skroutz page"
            );
        }

        PriceConfidence confidence = importPrice > 0.0 ? PriceConfidence.EXACT : PriceConfidence.LOW;
        String reason = importPrice > 0.0
                ? "Imported SSD/NVMe from rendered Skroutz page"
                : "Imported SSD/NVMe from rendered Skroutz page; price was not visible";

        return Optional.of(new MarketplaceSearchResult(
                MarketplaceCategory.STORAGE,
                title,
                importPrice,
                productUrl,
                confidence,
                reason,
                buildSpecsSummary(importedStorage),
                importedStorage,
                seedQuote
        ));
    }

    private Optional<MarketplaceSearchResult> buildDirectGpuImportResult(String productUrl, String renderedText) {
        String urlTitle = titleFromProductUrl(productUrl);
        String title = urlTitle;
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        String normalizedTitle = normalize(title);
        boolean hasGpuModel = GPU_MODEL_PATTERN.matcher(title).find();
        if (!hasGpuModel && !looksLikeGpuProduct(normalizedTitle)) {
            return Optional.empty();
        }
        if (looksLikeCpuProduct(normalizedTitle) || (looksLikeStorageProduct(normalize(urlTitle)) && !looksLikeGpuProduct(normalize(urlTitle)))) {
            return Optional.empty();
        }

        PriceMatch match = extractPrice(Jsoup.parse(renderedText != null ? renderedText : "", productUrl), renderedText != null ? renderedText : "");
        double importPrice = match != null ? match.priceEur() : 0.0;
        GPU importedGpu = buildGpu(title, renderedText != null ? renderedText : "", importPrice, productUrl, title);

        PriceQuote seedQuote = null;
        if (importPrice > 0.0) {
            seedQuote = new PriceQuote(
                    importedGpu.id(),
                    importPrice,
                    "Skroutz",
                    productUrl,
                    title,
                    match != null ? match.storeCount() : null,
                    Instant.now(),
                    true,
                    PriceConfidence.EXACT,
                    "Imported GPU from rendered Skroutz page"
            );
        }

        return Optional.of(new MarketplaceSearchResult(
                MarketplaceCategory.GPU,
                title,
                importPrice,
                productUrl,
                importPrice > 0.0 ? PriceConfidence.EXACT : PriceConfidence.LOW,
                importPrice > 0.0
                        ? "Imported GPU from rendered Skroutz page"
                        : "Imported GPU from rendered Skroutz page; price was not visible",
                buildSpecsSummary(importedGpu),
                importedGpu,
                seedQuote
        ));
    }

    private Optional<MarketplaceSearchResult> buildSamsung980ProImportResult(String productUrl, String renderedText) {
        String urlTitle = titleFromProductUrl(productUrl);
        String combined = urlTitle + "\n" + (renderedText != null ? renderedText : "") + "\n" + productUrl;
        if (!looksLikeSamsung980Pro(normalize(combined))) {
            return Optional.empty();
        }

        int capacityGb = extractCapacityGb(urlTitle);
        if (capacityGb <= 0) {
            capacityGb = extractCapacityGb(firstNonBlankLine(renderedText));
        }
        if (capacityGb <= 0) {
            capacityGb = extractCapacityGb(combined);
        }
        capacityGb = Math.max(capacityGb, 500);

        String renderedTitle = bestRenderedStorageTitle(renderedText);
        String title = renderedTitle != null && !renderedTitle.isBlank()
                ? renderedTitle
                : urlTitle;
        if (title == null || title.isBlank()) {
            title = bestTitleForRenderedImport(productUrl, renderedText);
        }
        String model = alignStorageModelCapacity(cleanupModelTitle(title, "Samsung"), capacityGb);
        double importPrice = 0.0;
        PriceMatch match = extractStorageAwarePrice(
                Jsoup.parse(renderedText != null ? renderedText : "", productUrl),
                renderedText != null ? renderedText : "",
                title + "\n" + (renderedText != null ? renderedText : "")
        );
        if (match != null && match.priceEur() > 0.0) {
            importPrice = match.priceEur();
        }

        Storage importedStorage = new Storage(
                buildImportPartId(MarketplaceCategory.STORAGE, title + " " + capacityGb + "GB NVMe"),
                "Samsung",
                model,
                "NVMe",
                capacityGb,
                9.1,
                importPrice,
                "",
                title,
                productUrl
        );

        PriceQuote seedQuote = null;
        if (importPrice > 0.0) {
            seedQuote = new PriceQuote(
                    importedStorage.id(),
                    importPrice,
                    "Skroutz",
                    productUrl,
                    title,
                    match != null ? match.storeCount() : null,
                    Instant.now(),
                    true,
                    PriceConfidence.EXACT,
                    "Imported Samsung 980 Pro SSD/NVMe from rendered Skroutz page"
            );
        }

        return Optional.of(new MarketplaceSearchResult(
                MarketplaceCategory.STORAGE,
                title,
                importPrice,
                productUrl,
                importPrice > 0.0 ? PriceConfidence.EXACT : PriceConfidence.LOW,
                importPrice > 0.0
                        ? "Imported Samsung 980 Pro SSD/NVMe from rendered Skroutz page"
                        : "Imported Samsung 980 Pro SSD/NVMe from rendered Skroutz page; price was not visible",
                buildSpecsSummary(importedStorage),
                importedStorage,
                seedQuote
        ));
    }

    private String compactRenderedText(String renderedText) {
        if (renderedText == null || renderedText.isBlank()) {
            return "";
        }
        int max = 180_000;
        if (renderedText.length() <= max) {
            return renderedText;
        }
        int head = 115_000;
        int tail = 65_000;
        return renderedText.substring(0, head) + "\n...\n" + renderedText.substring(renderedText.length() - tail);
    }

    private String bestTitleForRenderedImport(String productUrl, String renderedText) {
        String decodedUrlTitle = titleFromProductUrl(productUrl);
        String renderedStorageTitle = bestRenderedStorageTitle(renderedText);
        if (shouldPreferRenderedStorageTitle(productUrl, decodedUrlTitle, renderedStorageTitle)) {
            return renderedStorageTitle;
        }

        String renderedProductTitle = bestRenderedProductTitle(renderedText);
        if (shouldPreferRenderedProductTitle(productUrl, decodedUrlTitle, renderedProductTitle)) {
            return renderedProductTitle;
        }

        if (decodedUrlTitle != null && !decodedUrlTitle.isBlank()
                && (looksLikeStorageProduct(normalize(decodedUrlTitle))
                || inferExplicitProductCategory(decodedUrlTitle, renderedText != null ? renderedText : "") != null)) {
            return decodedUrlTitle;
        }

        if (renderedText != null && !renderedText.isBlank()) {
            for (String line : renderedText.split("\\R")) {
                String trimmed = line != null ? line.trim() : "";
                if (trimmed.length() >= 12 && looksLikeStorageProduct(normalize(trimmed))) {
                    return trimmed;
                }
            }
            String first = firstNonBlankLine(renderedText);
            if (first != null && !first.isBlank()) {
                return first;
            }
        }

        return decodedUrlTitle != null ? decodedUrlTitle : "";
    }

    private String bestRenderedProductTitle(String renderedText) {
        if (renderedText == null || renderedText.isBlank()) {
            return "";
        }

        String best = "";
        int bestScore = 0;
        for (String line : renderedText.split("\\R")) {
            String trimmed = line != null ? sanitizeImportedTitle(line.trim()) : "";
            if (trimmed.length() < 12 || trimmed.length() > 220) {
                continue;
            }
            String normalized = normalize(trimmed);
            MarketplaceCategory category = inferExplicitProductCategory(trimmed, "");
            if (category == null) {
                continue;
            }

            int score = 10;
            if (containsAny(normalized, "amd", "intel", "nvidia", "msi", "asus", "gigabyte", "corsair", "samsung", "kingston", "coolermaster", "cooler master", "g skill", "gskill", "patriot", "viper", "progaming", "lian li", "fractal", "nzxt", "thermaltake", "montech", "phanteks", "silverstone")) score += 4;
            if (extractCapacityGb(trimmed) > 0 || GPU_MODEL_PATTERN.matcher(trimmed).find() || CPU_MODEL_PATTERN.matcher(trimmed).find()) score += 4;
            if (category == MarketplaceCategory.CASE && looksLikeRealCaseProductTitle(trimmed)) score += 8;
            if (category == MarketplaceCategory.RAM && looksLikeRealRamProductTitle(trimmed)) score += 10;
            if (!trimmed.equals(trimmed.toLowerCase(Locale.ROOT))) score += 2;
            if (looksLikeNarrativeLine(trimmed)) score -= 18;
            if (looksLikeGenericCaseTitle(trimmed)) score -= 12;
            if (looksLikeGenericRamTitle(trimmed) || looksLikeRamFeatureLine(trimmed)) score -= 16;
            if (score > bestScore) {
                bestScore = score;
                best = trimmed;
            }
        }
        return best;
    }

    private boolean shouldPreferRenderedProductTitle(String productUrl, String decodedUrlTitle, String renderedProductTitle) {
        if (renderedProductTitle == null || renderedProductTitle.isBlank()) {
            return false;
        }
        if (decodedUrlTitle == null || decodedUrlTitle.isBlank()) {
            return true;
        }

        MarketplaceCategory renderedCategory = inferExplicitProductCategory(renderedProductTitle, "");
        MarketplaceCategory decodedCategory = inferExplicitProductCategory(decodedUrlTitle, "");
        if (renderedCategory == null || decodedCategory == null || renderedCategory != decodedCategory) {
            return false;
        }

        int renderedCapacity = extractCapacityGb(renderedProductTitle);
        int decodedCapacity = extractCapacityGb(decodedUrlTitle);
        if (renderedCapacity > 0 && decodedCapacity > 0 && !capacitiesApproximatelyMatch(renderedCapacity, decodedCapacity)) {
            return false;
        }

        return productUrl != null && productUrl.toLowerCase(Locale.ROOT).contains("product_id=")
                || decodedUrlTitle.contains(" typou ")
                || decodedUrlTitle.equals(decodedUrlTitle.toLowerCase(Locale.ROOT));
    }

    private boolean looksLikeRealCaseProductTitle(String title) {
        String normalized = normalize(title);
        return containsAny(normalized,
                "case", "kouti", "ypologisti", "micro tower", "mid tower", "mini tower", "cube",
                "lian li", "progaming", "fractal", "nzxt", "thermaltake", "montech", "phanteks", "silverstone");
    }

    private boolean looksLikeGenericCaseTitle(String title) {
        String normalized = normalize(title);
        return normalized.equals("koutia ypologiston")
                || normalized.equals("kouti ypologisti")
                || normalized.equals("pc cases")
                || normalized.equals("cases");
    }

    private boolean looksLikeNarrativeLine(String title) {
        String normalized = normalize(title);
        int words = normalized.isBlank() ? 0 : normalized.split(" ").length;
        return words >= 18
                || containsAny(normalized,
                "einai ena", "idaniko gia", "oi xristes", "toxwrizoun", "xexorizoun",
                "prosferontas", "euflexia", "anazhtoun", "anazitoun", "apotelesma",
                "the users", "ideal for", "designed for");
    }

    private boolean looksLikeRealRamProductTitle(String title) {
        String normalized = normalize(title);
        return containsAny(normalized,
                "ram", "ddr5", "ddr4", "trident", "viper", "venom", "royal", "ripjaws",
                "g skill", "gskill", "patriot", "corsair", "vengeance", "kingston", "fury")
                && (containsAny(normalized, "gb", "2x", "mhz", "mt s", "modules", "module")
                || SPEED_PATTERN.matcher(title).find());
    }

    private boolean looksLikeGenericRamTitle(String title) {
        String normalized = normalize(title);
        return normalized.equals("mnimes ram")
                || normalized.equals("mnimes ram g skill")
                || normalized.equals("g skill mnimes ram g skill")
                || normalized.equals("memory ram")
                || normalized.equals("ram memory");
    }

    private boolean looksLikeRamFeatureLine(String title) {
        String normalized = normalize(title);
        return containsAny(normalized,
                "amd memory profile", "intel xmp", "amd expo", "expo xmp", "xmp expo",
                "cas latency", "timings", "voltage");
    }

    private String bestRenderedStorageTitle(String renderedText) {
        if (renderedText == null || renderedText.isBlank()) {
            return "";
        }

        String best = "";
        int bestScore = 0;
        for (String line : renderedText.split("\\R")) {
            String trimmed = line != null ? line.trim() : "";
            if (trimmed.length() < 12 || trimmed.length() > 220) {
                continue;
            }
            String normalized = normalize(trimmed);
            if (!looksLikeStorageProduct(normalized)) {
                continue;
            }

            int score = 10;
            if (extractCapacityGb(trimmed) > 0) score += 8;
            if (containsAny(normalized, "ssd", "nvme", "m 2", "m2", "pci express", "pcie")) score += 4;
            if (containsAny(normalized, "typou", "type", "syndesi")) score += 2;
            if (score > bestScore) {
                bestScore = score;
                best = trimmed;
            }
        }
        return best;
    }

    private boolean shouldPreferRenderedStorageTitle(String productUrl, String decodedUrlTitle, String renderedStorageTitle) {
        if (renderedStorageTitle == null || renderedStorageTitle.isBlank()) {
            return false;
        }
        if (decodedUrlTitle == null || decodedUrlTitle.isBlank()) {
            return true;
        }
        int renderedCapacity = extractCapacityGb(renderedStorageTitle);
        int urlCapacity = extractCapacityGb(decodedUrlTitle);
        if (renderedCapacity > 0 && urlCapacity > 0 && !capacitiesApproximatelyMatch(renderedCapacity, urlCapacity)) {
            return true;
        }
        return productUrl != null
                && productUrl.toLowerCase(Locale.ROOT).contains("product_id=")
                && renderedCapacity > 0;
    }

    public Optional<PriceQuote> fetchLowestPrice(PriceTarget target) throws IOException, InterruptedException {
        if (target.skroutzUrl() != null && !target.skroutzUrl().isBlank()) {
            Optional<PriceQuote> direct = fetchFromProductPage(target, target.skroutzUrl(), true, target.searchQuery());
            if (direct.isPresent()) {
                return direct;
            }
        }

        LinkedHashSet<String> queries = new LinkedHashSet<>();
        if (target.manufacturerPartNumber() != null && !target.manufacturerPartNumber().isBlank()) {
            queries.add(target.manufacturerPartNumber().trim());
        }
        if (target.searchQuery() != null && !target.searchQuery().isBlank()) {
            queries.add(target.searchQuery().trim());
        }
        queries.add(target.displayName());

        PriceQuote bestQuote = null;
        for (String query : queries) {
            List<String> candidateUrls = findCandidateProductUrls(query, MAX_CANDIDATE_URLS_PRICE);
            for (String productUrl : candidateUrls) {
                Optional<PriceQuote> quote = fetchFromProductPage(target, productUrl, false, query);
                if (quote.isEmpty()) {
                    continue;
                }
                if (bestQuote == null || quote.get().confidence().rank() > bestQuote.confidence().rank()) {
                    bestQuote = quote.get();
                }
                if (quote.get().confidence().rank() >= PriceConfidence.HIGH.rank()) {
                    return quote;
                }
            }
        }

        return Optional.ofNullable(bestQuote);
    }

    public List<MarketplaceSearchResult> searchProducts(MarketplaceCategory category, String userQuery, int limit) {
        List<MarketplaceSearchResult> rawResults = new ArrayList<>();

        try {
            LinkedHashSet<String> seenUrls = new LinkedHashSet<>();
            for (String query : buildDiscoveryQueries(category, userQuery)) {
                List<String> candidateUrls = findCandidateProductUrls(query, MAX_CANDIDATE_URLS_DISCOVERY);
                for (String productUrl : candidateUrls) {
                    if (!seenUrls.add(productUrl)) {
                        continue;
                    }
                    Optional<MarketplaceSearchResult> result = fetchMarketplaceResult(category, userQuery, query, productUrl);
                    result.ifPresent(rawResults::add);
                    Thread.sleep(90L);
                }
            }
        } catch (Exception ignored) {

        }

        return postProcessDiscoveryResults(category, userQuery, rawResults, limit);
    }

    private Optional<MarketplaceSearchResult> fetchMarketplaceResult(MarketplaceCategory category, String rawUserQuery,
                                                                  String discoveryQuery, String productUrl)
            throws IOException, InterruptedException {
        String html = fetchHtml(productUrl);
        if (html == null || html.isBlank()) {
            return Optional.empty();
        }

        Document doc = Jsoup.parse(html, productUrl);
        String pageTitle = extractTitle(doc, html);
        String pageText = doc.text();
        PriceMatch match = category == MarketplaceCategory.STORAGE
                ? extractStorageAwarePrice(doc, html, pageTitle + " " + pageText)
                : extractPrice(doc, html);
        if (pageTitle == null || pageTitle.isBlank()) {
            return Optional.empty();
        }

        double importPrice = match != null ? match.priceEur() : 0.0;
        Object importedPart = buildImportedPart(category, pageTitle, pageText, importPrice, productUrl, rawUserQuery);
        PriceConfidence confidence = evaluateDiscoveryConfidence(category, pageTitle, rawUserQuery, importedPart);
        String specsSummary = buildSpecsSummary(importedPart);
        String partId = buildImportPartId(category, pageTitle);
        PriceQuote seedQuote = new PriceQuote(
                partId,
                importPrice,
                "Skroutz",
                productUrl,
                pageTitle,
                match != null ? match.storeCount() : null,
                Instant.now(),
                true,
                confidence,
                buildDiscoveryReason(category, rawUserQuery, pageTitle, importedPart, confidence)
        );

        return Optional.of(new MarketplaceSearchResult(
                category,
                pageTitle,
                importPrice,
                productUrl,
                confidence,
                buildDiscoveryReason(category, rawUserQuery, pageTitle, importedPart, confidence),
                specsSummary,
                importedPart,
                seedQuote
        ));
    }

    private Optional<PriceQuote> fetchFromProductPage(PriceTarget target, String productUrl, boolean exactUrl, String queryUsed)
            throws IOException, InterruptedException {

        String html = fetchHtml(productUrl);
        if (html == null || html.isBlank()) {
            return Optional.empty();
        }

        Document doc = Jsoup.parse(html, productUrl);
        String pageTitle = extractTitle(doc, html);
        PriceMatch match = extractPrice(doc, html);
        if (match == null) {
            return Optional.empty();
        }

        MatchAnalysis analysis = analyzeMatch(target, pageTitle, match.priceEur(), exactUrl, queryUsed);
        return Optional.of(new PriceQuote(
                target.partId(),
                match.priceEur(),
                "Skroutz",
                productUrl,
                pageTitle,
                match.storeCount(),
                Instant.now(),
                true,
                analysis.confidence(),
                analysis.reason()
        ));
    }

    private MatchAnalysis analyzeMatch(PriceTarget target, String pageTitle, double priceEur, boolean exactUrl, String queryUsed) {
        String title = pageTitle != null ? pageTitle : "";
        String normalizedTitle = normalize(title);
        String normalizedDisplay = normalize(target.displayName());
        String normalizedQuery = normalize(queryUsed != null ? queryUsed : target.searchQuery());
        String normalizedMpn = normalize(target.manufacturerPartNumber());

        int displayMatches = countTokenMatches(normalizedTitle, normalizedDisplay);
        int queryMatches = countTokenMatches(normalizedTitle, normalizedQuery);
        boolean mpnMatched = !normalizedMpn.isBlank() && normalizedTitle.contains(normalizedMpn);

        PriceConfidence confidence;
        String reason;

        if (exactUrl) {
            confidence = PriceConfidence.EXACT;
            reason = "Exact Skroutz URL";
        } else if (mpnMatched) {
            confidence = PriceConfidence.HIGH;
            reason = "Manufacturer part number matched";
        } else if (displayMatches >= 3 || queryMatches >= 3) {
            confidence = PriceConfidence.HIGH;
            reason = "Strong title/token match";
        } else if (displayMatches >= 2 || queryMatches >= 2) {
            confidence = PriceConfidence.MEDIUM;
            reason = "Partial title/token match";
        } else if (matchesTarget(title, queryUsed)) {
            confidence = PriceConfidence.MEDIUM;
            reason = "Weak title/token match";
        } else {
            confidence = PriceConfidence.LOW;
            reason = "Suspicious title mismatch";
        }

        double fallback = target.fallbackPriceEur();
        if (fallback > 0 && priceEur > 0 && !exactUrl) {
            double ratio = priceEur / fallback;
            if (ratio < 0.35 || ratio > 2.40) {
                confidence = PriceConfidence.LOW;
                reason += String.format(Locale.ROOT, "; price sanity failed (%.2fx vs CSV)", ratio);
            } else if (ratio < 0.55 || ratio > 1.90) {
                confidence = confidence.downgrade();
                reason += String.format(Locale.ROOT, "; price far from CSV (%.2fx)", ratio);
            }
        }

        return new MatchAnalysis(confidence, reason);
    }

    private PriceConfidence evaluateDiscoveryConfidence(MarketplaceCategory category, String pageTitle,
                                                       String rawUserQuery, Object importedPart) {
        String normalizedTitle = normalize(pageTitle);
        List<String> requiredTokens = discoveryRequiredTokens(category, rawUserQuery);
        int matchedRequired = 0;
        for (String token : requiredTokens) {
            if (normalizedTitle.contains(token)) {
                matchedRequired++;
            }
        }

        int totalMatches = countTokenMatches(normalizedTitle, normalize(rawUserQuery));
        boolean parsedModelAligned = importedPart != null && isParsedModelAligned(category, rawUserQuery, importedPart);

        if (!requiredTokens.isEmpty() && matchedRequired == requiredTokens.size() && parsedModelAligned) {
            return PriceConfidence.HIGH;
        }
        if (!requiredTokens.isEmpty() && matchedRequired >= Math.max(1, requiredTokens.size() - 1) && parsedModelAligned) {
            return PriceConfidence.MEDIUM;
        }
        if (parsedModelAligned && totalMatches >= 2) {
            return PriceConfidence.MEDIUM;
        }
        if (parsedModelAligned || totalMatches >= 1) {
            return PriceConfidence.LOW;
        }
        return PriceConfidence.LOW;
    }

    private List<MarketplaceSearchResult> postProcessDiscoveryResults(MarketplaceCategory category, String rawUserQuery,
                                                                      List<MarketplaceSearchResult> rawResults, int limit) {
        Map<String, MarketplaceSearchResult> bestByKey = new LinkedHashMap<>();
        for (MarketplaceSearchResult result : rawResults) {
            if (!passesDiscoveryRails(category, rawUserQuery, result)) {
                continue;
            }
            String dedupKey = buildDiscoveryDedupKey(category, result);
            MarketplaceSearchResult existing = bestByKey.get(dedupKey);
            if (existing == null || isBetterDiscoveryCandidate(result, existing)) {
                bestByKey.put(dedupKey, result);
            }
        }

        List<MarketplaceSearchResult> results = new ArrayList<>(bestByKey.values());
        results.sort((a, b) -> {
            int confidenceCmp = Integer.compare(b.confidence().rank(), a.confidence().rank());
            if (confidenceCmp != 0) return confidenceCmp;
            int queryCmp = Integer.compare(discoveryQueryMatchScore(category, rawUserQuery, b),
                    discoveryQueryMatchScore(category, rawUserQuery, a));
            if (queryCmp != 0) return queryCmp;
            return Double.compare(a.priceEur(), b.priceEur());
        });

        if (category == MarketplaceCategory.GPU) {
            results = diversifyGpuResults(results);
        }

        if (results.size() > limit) {
            return new ArrayList<>(results.subList(0, limit));
        }
        return results;
    }

    private boolean passesDiscoveryRails(MarketplaceCategory category, String rawUserQuery, MarketplaceSearchResult result) {
        if (result == null || result.title() == null || result.title().isBlank() || result.priceEur() <= 0) {
            return false;
        }
        if (containsBlockedTerms(category, result.title())) {
            return false;
        }
        if (!matchesDiscoveryIntent(category, rawUserQuery, result)) {
            return false;
        }
        return passesCategoryPriceRails(category, result);
    }

    private boolean containsBlockedTerms(MarketplaceCategory category, String title) {
        String normalized = normalize(title);
        List<String> blocked = new ArrayList<>(List.of(
                "desktop pc", "gaming desktop", "workstation", "laptop", "notebook", "mini pc",
                "all in one", "aio", "bundle", "kit upgrade", "complete system", "server"
        ));

        if (category == MarketplaceCategory.GPU) {
            blocked.addAll(List.of("waterblock", "bracket", "support bracket", "riser", "adapter", "cable", "cooler"));
        }
        if (category == MarketplaceCategory.CPU) {
            blocked.addAll(List.of("cpu cooler", "cooler", "motherboard bundle"));
        }

        for (String token : blocked) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesDiscoveryIntent(MarketplaceCategory category, String rawUserQuery, MarketplaceSearchResult result) {
        if (rawUserQuery == null || rawUserQuery.isBlank()) {
            return true;
        }

        List<String> requiredTokens = discoveryRequiredTokens(category, rawUserQuery);
        if (requiredTokens.isEmpty()) {
            return true;
        }

        String normalizedTitle = normalize(result.title());
        int matched = 0;
        for (String token : requiredTokens) {
            if (normalizedTitle.contains(token)) {
                matched++;
            }
        }

        int allowedMisses = switch (category) {
            case CPU, MOTHERBOARD -> 0;
            case RAM -> requiredTokens.size() >= 4 ? 1 : 0;
            default -> 0;
        };

        return matched >= Math.max(1, requiredTokens.size() - allowedMisses);
    }

    private List<String> discoveryRequiredTokens(MarketplaceCategory category, String rawUserQuery) {
        String normalized = normalize(rawUserQuery);
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split(" ")) {
            if (token.isBlank()) {
                continue;
            }
            if (!token.chars().anyMatch(Character::isDigit) && token.length() < 3) {
                continue;
            }
            if (isDiscoveryStopWord(category, token)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private boolean isDiscoveryStopWord(MarketplaceCategory category, String token) {
        if (token == null || token.isBlank()) {
            return true;
        }
        return switch (token) {
            case "cpu", "gpu", "ram", "motherboard", "mitriki", "karta", "grafikon", "trofodotiko",
                    "storage", "ssd", "nvme", "ddr5", "ddr4", "processor", "epexergastis",
                    "socket", "wifi", "desktop", "gaming", "and", "with", "for" -> true;
            case "amd", "intel", "nvidia", "geforce", "radeon" -> false;
            default -> false;
        };
    }

    private boolean isParsedModelAligned(MarketplaceCategory category, String rawUserQuery, Object importedPart) {
        if (rawUserQuery == null || rawUserQuery.isBlank() || importedPart == null) {
            return true;
        }

        String normalizedQuery = normalize(rawUserQuery);
        return switch (category) {
            case CPU -> {
                CPU cpu = (CPU) importedPart;
                yield discoveryRequiredTokens(category, normalizedQuery).stream()
                        .allMatch(token -> normalize(cpu.model()).contains(token));
            }
            case GPU -> {
                GPU gpu = (GPU) importedPart;
                yield discoveryRequiredTokens(category, normalizedQuery).stream()
                        .allMatch(token -> normalize(gpu.model()).contains(token));
            }
            case MOTHERBOARD -> {
                Motherboard motherboard = (Motherboard) importedPart;
                String haystack = normalize(motherboard.brand() + " " + motherboard.model() + " " + motherboard.socket() + " " + motherboard.formFactor());
                yield discoveryRequiredTokens(category, normalizedQuery).stream().allMatch(haystack::contains);
            }
            case RAM -> {
                RAM ram = (RAM) importedPart;
                String haystack = normalize(ram.brand() + " " + ram.model() + " " + ram.capacityGb() + " " + ram.sticks() + " " + ram.speedMhz());
                yield discoveryRequiredTokens(category, normalizedQuery).stream().allMatch(haystack::contains);
            }
            case STORAGE -> {
                Storage storage = (Storage) importedPart;
                String haystack = normalize(storage.brand() + " " + storage.model() + " " + storage.type() + " " + storage.capacityGb());
                yield discoveryRequiredTokens(category, normalizedQuery).stream().allMatch(haystack::contains);
            }
            case PSU -> {
                PSU psu = (PSU) importedPart;
                String haystack = normalize(psu.brand() + " " + psu.model() + " " + psu.wattageW());
                yield discoveryRequiredTokens(category, normalizedQuery).stream().allMatch(haystack::contains);
            }
            case CASE -> {
                PCCase pcCase = (PCCase) importedPart;
                String haystack = normalize(pcCase.brand() + " " + pcCase.model() + " " + pcCase.supportedFormFactors());
                yield discoveryRequiredTokens(category, normalizedQuery).stream().allMatch(haystack::contains);
            }
            case COOLER -> {
                Cooler cooler = (Cooler) importedPart;
                String haystack = normalize(cooler.brand() + " " + cooler.model() + " " + cooler.type());
                yield discoveryRequiredTokens(category, normalizedQuery).stream().allMatch(haystack::contains);
            }
        };
    }

    private boolean passesCategoryPriceRails(MarketplaceCategory category, MarketplaceSearchResult result) {
        double price = result.priceEur();
        if (price <= 0) {
            return false;
        }

        return switch (category) {
            case GPU -> passesGpuPriceRails(result.title(), price);
            case CPU -> passesCpuPriceRails(result.title(), price);
            case MOTHERBOARD -> price >= 50 && price <= 1500;
            case RAM -> price >= 20 && price <= 2000;
            case STORAGE -> price >= 20 && price <= 2000;
            case PSU -> price >= 30 && price <= 1500;
            case CASE -> price >= 30 && price <= 800;
            case COOLER -> price >= 15 && price <= 400;
        };
    }

    private boolean passesGpuPriceRails(String title, double price) {
        String normalized = normalize(title);
        if (normalized.contains("5090")) return price >= 2500 && price <= 6000;
        if (normalized.contains("5080")) return price >= 900 && price <= 3000;
        if (normalized.contains("4090")) return price >= 1200 && price <= 4000;
        if (normalized.contains("4080")) return price >= 700 && price <= 2500;
        if (normalized.contains("5070 ti")) return price >= 650 && price <= 2200;
        if (normalized.contains("5070")) return price >= 450 && price <= 1800;
        if (normalized.contains("4070 ti")) return price >= 500 && price <= 1800;
        if (normalized.contains("4070")) return price >= 350 && price <= 1500;
        if (normalized.contains("7900 xtx")) return price >= 700 && price <= 2500;
        if (normalized.contains("7900 xt")) return price >= 550 && price <= 2000;
        if (normalized.contains("7800 xt")) return price >= 350 && price <= 1200;
        return price >= 120 && price <= 6000;
    }

    private boolean passesCpuPriceRails(String title, double price) {
        String normalized = normalize(title);
        if (normalized.contains("9800x3d")) return price >= 300 && price <= 1200;
        if (normalized.contains("7800x3d")) return price >= 220 && price <= 900;
        if (normalized.contains("9950x3d")) return price >= 500 && price <= 1500;
        if (normalized.contains("7950x3d")) return price >= 400 && price <= 1300;
        if (normalized.contains("9900x3d")) return price >= 450 && price <= 1400;
        if (normalized.contains("14900k")) return price >= 350 && price <= 1200;
        if (normalized.contains("14700k")) return price >= 250 && price <= 900;
        if (normalized.contains("14600k")) return price >= 180 && price <= 650;
        return price >= 60 && price <= 1500;
    }

    private String buildDiscoveryDedupKey(MarketplaceCategory category, MarketplaceSearchResult result) {
        if (result.importedPart() instanceof CPU cpu) {
            return category.name() + ":" + normalize(cpu.model());
        }
        if (result.importedPart() instanceof Motherboard motherboard) {
            return category.name() + ":" + normalize(motherboard.brand() + " " + motherboard.model());
        }
        if (result.importedPart() instanceof RAM ram) {
            return category.name() + ":" + normalize(ram.brand() + " " + ram.model()) + ":" + ram.capacityGb() + ":" + ram.sticks() + ":" + ram.speedMhz();
        }
        return category.name() + ":" + normalize(result.title());
    }

    private boolean isBetterDiscoveryCandidate(MarketplaceSearchResult candidate, MarketplaceSearchResult existing) {
        int confidenceCmp = Integer.compare(candidate.confidence().rank(), existing.confidence().rank());
        if (confidenceCmp != 0) {
            return confidenceCmp > 0;
        }
        return candidate.priceEur() < existing.priceEur();
    }

    private int discoveryQueryMatchScore(MarketplaceCategory category, String rawUserQuery, MarketplaceSearchResult result) {
        String normalizedTitle = normalize(result.title());
        int score = countTokenMatches(normalizedTitle, normalize(rawUserQuery)) * 10;
        if (result.importedPart() != null && isParsedModelAligned(category, rawUserQuery, result.importedPart())) {
            score += 25;
        }
        return score;
    }


    private List<String> buildDiscoveryQueries(MarketplaceCategory category, String userQuery) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        String base = buildDiscoveryQuery(category, userQuery);
        queries.add(base);

        String cleanUserQuery = userQuery == null ? "" : userQuery.trim();
        if (category == MarketplaceCategory.GPU && !cleanUserQuery.isBlank()) {
            String normalized = normalize(cleanUserQuery);
            boolean genericChipQuery = normalized.matches(".*(rtx|rx)? ?\\d{4}.*") || normalized.matches("\\d{4}( ti)?( super)?");
            if (genericChipQuery) {
                queries.add(buildDiscoveryQuery(category, cleanUserQuery + " msi"));
                queries.add(buildDiscoveryQuery(category, cleanUserQuery + " asus"));
                queries.add(buildDiscoveryQuery(category, cleanUserQuery + " zotac"));
                queries.add(buildDiscoveryQuery(category, cleanUserQuery + " pny"));
                queries.add(buildDiscoveryQuery(category, cleanUserQuery + " palit"));
                queries.add(buildDiscoveryQuery(category, cleanUserQuery + " gigabyte"));
            }
        }
        return new ArrayList<>(queries);
    }

    private List<MarketplaceSearchResult> diversifyGpuResults(List<MarketplaceSearchResult> sortedResults) {
        Map<String, List<MarketplaceSearchResult>> byPartner = new LinkedHashMap<>();
        for (MarketplaceSearchResult result : sortedResults) {
            String partner = gpuBoardPartnerKey(result.title());
            byPartner.computeIfAbsent(partner, k -> new ArrayList<>()).add(result);
        }

        List<MarketplaceSearchResult> diversified = new ArrayList<>();
        boolean added;
        int round = 0;
        do {
            added = false;
            for (List<MarketplaceSearchResult> bucket : byPartner.values()) {
                if (round < bucket.size()) {
                    diversified.add(bucket.get(round));
                    added = true;
                }
            }
            round++;
        } while (added);

        return diversified;
    }

    private String gpuBoardPartnerKey(String title) {
        String normalized = normalize(title);
        if (normalized.contains("asus")) return "asus";
        if (normalized.contains("msi")) return "msi";
        if (normalized.contains("gigabyte")) return "gigabyte";
        if (normalized.contains("zotac")) return "zotac";
        if (normalized.contains("pny")) return "pny";
        if (normalized.contains("palit")) return "palit";
        if (normalized.contains("gainward")) return "gainward";
        if (normalized.contains("inno3d")) return "inno3d";
        if (normalized.contains("sapphire")) return "sapphire";
        if (normalized.contains("xfx")) return "xfx";
        if (normalized.contains("powercolor")) return "powercolor";
        if (normalized.contains("asrock")) return "asrock";
        return "other";
    }

    private MarketplaceCategory inferExplicitProductCategory(String title, String pageText) {
        String normalizedTitle = normalize(title);

        if (looksLikeCoolerProduct(normalizedTitle)) {
            return MarketplaceCategory.COOLER;
        }
        if (CPU_MODEL_PATTERN.matcher(title).find() || looksLikeCpuProduct(normalizedTitle)) {
            return MarketplaceCategory.CPU;
        }
        if (looksLikeCaseProduct(normalizedTitle)) {
            return MarketplaceCategory.CASE;
        }
        if (containsAny(normalizedTitle, "motherboard", "mitriki", "μητρικη", "μητρικες", "mainboard")) {
            return MarketplaceCategory.MOTHERBOARD;
        }
        if (containsAny(normalizedTitle, "ddr5", "ddr4", "ram", "memory")) {
            return MarketplaceCategory.RAM;
        }


        if (looksLikeStorageProduct(normalizedTitle)) {
            return MarketplaceCategory.STORAGE;
        }
        if (containsAny(normalizedTitle, "psu", "power supply", "trofodotiko", "80 plus")) {
            return MarketplaceCategory.PSU;
        }
        if (GPU_MODEL_PATTERN.matcher(title).find() || looksLikeGpuProduct(normalizedTitle)) {
            return MarketplaceCategory.GPU;
        }
        return null;
    }

    private MarketplaceCategory inferCategory(String title, String pageText) {
        MarketplaceCategory explicitTitleCategory = inferExplicitProductCategory(title, pageText);
        if (explicitTitleCategory != null) {
            return explicitTitleCategory;
        }

        String normalizedCombined = normalize(title + " " + pageText);

        boolean hasGpuModel = GPU_MODEL_PATTERN.matcher(title + " " + pageText).find();
        boolean hasCpuModel = CPU_MODEL_PATTERN.matcher(title + " " + pageText).find();
        boolean hasSocket = SOCKET_PATTERN.matcher(title + " " + pageText).find();
        boolean hasRamKit = KIT_PATTERN.matcher(title + " " + pageText).find();
        boolean hasWattage = WATTAGE_PATTERN.matcher(title + " " + pageText).find();

        if (looksLikeCoolerProduct(normalizedCombined)) {
            return MarketplaceCategory.COOLER;
        }

        if (hasCpuModel || looksLikeCpuProduct(normalizedCombined)) {
            return MarketplaceCategory.CPU;
        }

        if (hasGpuModel || looksLikeGpuProduct(normalize(title))) {
            return MarketplaceCategory.GPU;
        }

        if (hasSocket && containsAny(normalizedCombined, "motherboard", "mitriki", "μητρικη", "μητρικες", "mainboard", "atx", "micro atx", "mini itx", "itx", "matx", "b650", "x670", "b850", "x870", "z790", "z890")) {
            return MarketplaceCategory.MOTHERBOARD;
        }
        if (hasRamKit && containsAny(normalizedCombined, "ddr5", "ddr4", "ram", "memory", "mt s", "mhz", "cl")) {
            return MarketplaceCategory.RAM;
        }
        if (looksLikeCaseProduct(normalizedCombined)) {
            return MarketplaceCategory.CASE;
        }

        if (looksLikeStorageProduct(normalizedCombined)) {
            return MarketplaceCategory.STORAGE;
        }
        if (hasWattage && containsAny(normalizedCombined, "80 plus", "fully modular", "semi modular", "power supply", "psu", "trofodotiko", "atx 3 0", "atx3 0")) {
            return MarketplaceCategory.PSU;
        }
        return null;
    }

    private boolean looksLikeCpuProduct(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }
        if (looksLikeCoolerProduct(normalizedText)) {
            return false;
        }
        return CPU_MODEL_PATTERN.matcher(normalizedText).find()
                || containsAny(normalizedText,
                "ryzen", "threadripper", "core i3", "core i5", "core i7", "core i9",
                "core ultra", "processor", "epexergastis", "cpu", "socket am5", "socket am4",
                "lga1700", "lga1851");
    }

    private boolean looksLikeGpuProduct(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }
        return containsAny(normalizedText,
                "geforce", "radeon", "graphics card", "karta grafik", "grafikon", "karta grafikon",
                "rtx 5080", "rtx5080", "rtx 5090", "rtx5090", "rtx 5070", "rtx5070",
                "rtx 4090", "rtx4090", "rtx 4080", "rtx4080",
                "rx 7900", "rx7900", "rx 7800", "rx7800");
    }

    private boolean looksLikeCaseProduct(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }

        return containsAny(normalizedText,
                "κουτι", "κουτι υπολογιστη", "κουτια υπολογιστων",
                "case", "pc case", "computer case", "chassis",
                "tower", "mini tower", "mid tower", "full tower",
                "fractal terra", "lian li a3", "a3 matx", "a3 m atx",
                "corsair 4000d", "nzxt h5", "masterbox", "fractal north");
    }

    private boolean looksLikeStorageProduct(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }

        if (looksLikeSamsung980Pro(normalizedText)) {
            return true;
        }

        return containsAny(normalizedText,
                "nvme", "ssd", "m 2", "m2", "pci express", "pcie",
                "hard disk", "hdd", "external ssd",
                "read speed", "write speed", "ταχυτητα αναγνωσης", "ταχυτητα εγγραφης",
                "χωρητικοτητα", "capacity", "samsung 980", "samsung 990", "980 pro", "990 pro",
                "mz v8p", "mz v8p1t0bw", "mz v8p2t0bw", "mz v9p", "kingston kc3000", "skc3000",
                "corsair mp600", "corsair mp700", "wd black", "sn850", "sn850x", "sn770", "crucial p3", "crucial p5",
                "spatium", "m450", "mp700 pro");
    }

    private boolean looksLikeStorageProductUrl(String productUrl) {
        if (productUrl == null || productUrl.isBlank()) {
            return false;
        }

        String normalizedUrl = normalize(productUrl);
        if (looksLikeSamsung980Pro(normalizedUrl)) {
            return true;
        }
        return containsAny(normalizedUrl,
                "ssd", "nvme", "m 2", "m2", "pci express", "pcie",
                "samsung 980", "samsung 990", "980 pro", "990 pro",
                "mz v8p", "mz v8p1t0bw", "mz v8p2t0bw", "mz v9p", "kc3000", "skc3000", "sn850", "sn850x", "mp700", "mp600", "spatium");
    }

    private boolean looksLikeSamsung980Pro(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }
        String compact = normalizedText.replace(" ", "");
        return containsAny(normalizedText, "mz v8p", "mz v8p1t0bw", "mz v8p2t0bw")
                || compact.contains("mzv8p")
                || (normalizedText.contains("980 pro")
                && containsAny(normalizedText, "samsung", "ssd", "m 2", "m2", "pci express", "pcie"));
    }

    private boolean looksLikeCoolerProduct(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }



        if (looksLikeStorageProduct(normalizedText)
                && !containsAny(normalizedText,
                "cpu cooler", "processor cooler", "ψυκτρα επεξεργαστη", "psyxtra epexergasti",
                "ψυξη cpu", "cpu cooling", "aio", "υδροψυξη", "ydropsyxi",
                "liquid cooler", "liquid freezer", "water cooler", "h100i", "h150i",
                "nh d15", "peerless assassin", "dark rock")) {
            return false;
        }

        return containsAny(normalizedText,
                "cpu cooler", "processor cooler", "cooler cpu",
                "ψυκτρα επεξεργαστη", "psyxtra epexergasti", "ψυξη cpu", "cpu cooling",
                "υδροψυξη", "ydropsyxi", "aio", "liquid cooler", "liquid freezer",
                "water cooler", "nh d15", "peerless assassin", "dark rock",
                "h100i", "h150i", "liquid freezer iii");
    }

    private boolean containsAny(String normalizedText, String... needles) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && normalizedText.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private String buildDiscoveryReason(MarketplaceCategory category, String rawUserQuery, String pageTitle,
                                        Object importedPart, PriceConfidence confidence) {
        StringBuilder reason = new StringBuilder(confidence.label());
        if (rawUserQuery != null && !rawUserQuery.isBlank()) {
            reason.append(" | query matched");
        }
        if (importedPart != null && isParsedModelAligned(category, rawUserQuery, importedPart)) {
            reason.append(" | parsed model aligned");
        }
        if (pageTitle != null && !pageTitle.isBlank()) {
            reason.append(" | ").append(pageTitle.length() > 42 ? pageTitle.substring(0, 42).trim() + "..." : pageTitle.trim());
        }
        return reason.toString();
    }

    private List<String> findCandidateProductUrls(String searchQuery, int maxUrls) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
        String searchUrl = SKROUTZ_BASE_URL + "/search?keyphrase=" + encodedQuery;

        String html = fetchHtml(searchUrl);
        if (html == null || html.isBlank()) {
            return List.of();
        }

        Set<String> urls = new LinkedHashSet<>();
        Document doc = Jsoup.parse(html, searchUrl);
        for (Element a : doc.select("a[href]")) {
            String href = a.absUrl("href");
            if (href == null || href.isBlank()) {
                href = a.attr("href");
            }
            addIfProductUrl(urls, href);
        }

        Matcher matcher = PRODUCT_URL_PATTERN.matcher(html);
        while (matcher.find()) {
            addIfProductUrl(urls, matcher.group(1));
        }

        List<String> candidates = new ArrayList<>(urls);
        return candidates.subList(0, Math.min(maxUrls, candidates.size()));
    }

    private PriceMatch extractPrice(Document doc, String html) {
        Matcher jsonMatcher = LOW_PRICE_JSON_PATTERN.matcher(html);
        if (jsonMatcher.find()) {
            double price = parseEuroAmount(jsonMatcher.group(1));
            if (price > 0) {
                return new PriceMatch(price, null);
            }
        }

        String text = doc.text();
        if (text == null || text.isBlank()) {
            return null;
        }

        if ((text.contains("Το προϊόν δεν υπάρχει πλέον στο Skroutz") ||
                text.contains("Το προϊόν δεν είναι διαθέσιμο στο Skroutz")) &&
                !text.matches(".*σε\\s+\\d+\\s+καταστήματα.*")) {
            return null;
        }

        Matcher storesMatcher = STORES_PRICE_PATTERN.matcher(text);
        if (storesMatcher.find()) {
            double price = parseEuroAmount(storesMatcher.group(1));
            Integer stores = Integer.valueOf(storesMatcher.group(2));
            if (price > 0) {
                return new PriceMatch(price, stores);
            }
        }

        Matcher fallbackMatcher = SIMPLE_FROM_PRICE_PATTERN.matcher(text);
        while (fallbackMatcher.find()) {
            if (isInstallmentPriceContext(text, fallbackMatcher.start(), fallbackMatcher.end())) {
                continue;
            }
            double price = parseEuroAmount(fallbackMatcher.group(1));
            if (price > 0) {
                return new PriceMatch(price, null);
            }
        }

        Double visibleSplitPrice = extractVisibleSplitEuroPrice(text);
        if (visibleSplitPrice != null && visibleSplitPrice > 0) {
            return new PriceMatch(visibleSplitPrice, null);
        }





        Matcher anyEuroMatcher = ANY_EURO_PRICE_PATTERN.matcher(text);
        double bestPlausible = -1.0;
        while (anyEuroMatcher.find()) {
            if (isInstallmentPriceContext(text, anyEuroMatcher.start(), anyEuroMatcher.end())) {
                continue;
            }
            double price = parseEuroAmount(anyEuroMatcher.group(1));
            if (price >= 10.0 && price <= 10000.0 && price > bestPlausible) {
                bestPlausible = price;
            }
        }
        if (bestPlausible > 0) {
            return new PriceMatch(bestPlausible, null);
        }

        return null;
    }

    private Double extractVisibleSplitEuroPrice(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalizedText = text
                .replace("\\u20ac", "â‚¬")
                .replace("&euro;", "â‚¬")
                .replace("&#8364;", "â‚¬")
                .replace("\\/", "/")
                .replace("\\t", " ");

        Matcher splitMatcher = STORAGE_SPLIT_EURO_PRICE_PATTERN.matcher(normalizedText);
        Double best = null;
        while (splitMatcher.find()) {
            if (isInstallmentPriceContext(normalizedText, splitMatcher.start(), splitMatcher.end())) {
                continue;
            }
            double price = parseEuroAmount(splitMatcher.group(1) + "," + splitMatcher.group(2));
            if (price >= 10.0 && price <= 10000.0 && (best == null || price > best)) {
                best = price;
            }
        }
        return best;
    }

    private boolean isInstallmentPriceContext(String text, int start, int end) {
        if (text == null || text.isBlank()) {
            return false;
        }

        int from = Math.max(0, start - 36);
        int to = Math.min(text.length(), end + 72);
        String context = normalize(text.substring(from, to));
        return containsAny(context,
                "μηνα", "μηνες", "δοσεις", "ατοκες", "μηνιαια",
                "mina", "mines", "doseis", "atokes", "monthly",
                "month", "installment", "instalment", "/ month");
    }


    private PriceMatch extractStorageAwarePrice(Document doc, String html, String titleAndText) {
        String text = doc != null ? doc.text() : "";
        String combined = (titleAndText != null ? titleAndText : "") + "\n" + text + "\n" + (html != null ? html : "");
        int capacityGb = extractCapacityGb(firstNonBlankLine(titleAndText));
        if (capacityGb <= 0) {
            capacityGb = extractCapacityGb(titleAndText != null ? titleAndText : combined);
        }

        Double variantPrice = extractStorageVariantPrice(combined, capacityGb);
        if (variantPrice != null && variantPrice > 0) {
            return new PriceMatch(variantPrice, null);
        }

        Double visibleMainPrice = extractStorageVisibleMainPrice(combined);
        if (visibleMainPrice != null && visibleMainPrice > 0) {
            return new PriceMatch(visibleMainPrice, null);
        }

        PriceMatch normal = extractPrice(doc, html);
        if (normal != null) {
            return normal;
        }

        variantPrice = extractStorageVariantPrice(combined, 0);
        if (variantPrice != null && variantPrice > 0) {
            return new PriceMatch(variantPrice, null);
        }

        return null;
    }


    private Double extractStorageVisibleMainPrice(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalizedText = text
                .replace("\\u20ac", "€")
                .replace("&euro;", "€")
                .replace("&#8364;", "€")
                .replace("\\/", "/")
                .replace("\\t", " ");

        Matcher splitMatcher = STORAGE_SPLIT_EURO_PRICE_PATTERN.matcher(normalizedText);
        Double best = null;
        while (splitMatcher.find()) {
            double price = parseEuroAmount(splitMatcher.group(1) + "," + splitMatcher.group(2));
            if (price >= 20.0 && price <= 5000.0) {
                if (best == null || price > best) {
                    best = price;
                }
            }
        }
        if (best != null) {
            return best;
        }

        Matcher compactMatcher = STORAGE_COMPACT_EURO_PRICE_PATTERN.matcher(normalizedText);
        while (compactMatcher.find()) {
            double price = parseEuroAmount(compactMatcher.group(1) + "," + compactMatcher.group(2));
            if (price >= 20.0 && price <= 5000.0) {
                return price;
            }
        }

        Matcher euroMatcher = ANY_EURO_PRICE_PATTERN.matcher(normalizedText);
        while (euroMatcher.find()) {
            double price = parseEuroAmount(euroMatcher.group(1));
            if (price >= 20.0 && price <= 5000.0) {
                return price;
            }
        }

        return null;
    }

    private Double extractStorageVariantPrice(String text, int targetCapacityGb) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String normalizedText = text
                .replace("\\u20ac", "€")
                .replace("&euro;", "€")
                .replace("&#8364;", "€")
                .replace("\\/", "/")
                .replace("\\n", " ")
                .replace("\\t", " ");

        Matcher matcher = STORAGE_VARIANT_PRICE_PATTERN.matcher(normalizedText);
        Double bestMatchingPrice = null;
        Double cheapestPlausible = null;

        while (matcher.find()) {
            double numericCapacity = parseDoubleFlexible(matcher.group(1));
            String unit = matcher.group(2);
            int capacityGb = unit != null && unit.equalsIgnoreCase("TB")
                    ? (int) Math.round(numericCapacity * 1000.0)
                    : (int) Math.round(numericCapacity);
            double price = parseEuroAmount(matcher.group(3));

            if (capacityGb <= 0 || price < 10.0 || price > 5000.0) {
                continue;
            }

            if (cheapestPlausible == null || price < cheapestPlausible) {
                cheapestPlausible = price;
            }

            if (targetCapacityGb > 0 && capacitiesApproximatelyMatch(capacityGb, targetCapacityGb)) {
                bestMatchingPrice = price;
                break;
            }
        }

        if (bestMatchingPrice != null) {
            return bestMatchingPrice;
        }
        return targetCapacityGb > 0 ? null : cheapestPlausible;
    }

    private boolean capacitiesApproximatelyMatch(int candidateGb, int targetGb) {
        if (candidateGb <= 0 || targetGb <= 0) {
            return false;
        }
        if (candidateGb == targetGb) {
            return true;
        }
        int diff = Math.abs(candidateGb - targetGb);
        int tolerance = Math.max(64, (int) Math.round(targetGb * 0.08));
        return diff <= tolerance;
    }

    private String firstNonBlankLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        for (String line : text.split("\\R")) {
            String trimmed = line != null ? line.trim() : "";
            if (!trimmed.isBlank() && trimmed.length() >= 8) {
                return trimmed;
            }
        }
        return "";
    }

    private String titleFromProductUrl(String productUrl) {
        if (productUrl == null || productUrl.isBlank()) {
            return "";
        }
        try {
            Matcher matcher = PRODUCT_URL_PATTERN.matcher(productUrl);
            if (!matcher.find()) {
                return "";
            }
            String path = matcher.group(1);
            int lastSlash = path.lastIndexOf('/');
            String slug = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
            int queryIndex = slug.indexOf('?');
            if (queryIndex >= 0) {
                slug = slug.substring(0, queryIndex);
            }
            int fragmentIndex = slug.indexOf('#');
            if (fragmentIndex >= 0) {
                slug = slug.substring(0, fragmentIndex);
            }
            if (slug.endsWith(".html")) {
                slug = slug.substring(0, slug.length() - 5);
            }
            return sanitizeImportedTitle(URLDecoder.decode(slug, StandardCharsets.UTF_8)
                    .replace('-', ' ')
                    .replaceAll("\\s+", " ")
                    .trim());
        } catch (Exception e) {
            return "";
        }
    }

    private String fetchHtml(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "el-GR,el;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        }
        return null;
    }

    private void addIfProductUrl(Set<String> urls, String href) {
        if (href == null || href.isBlank()) {
            return;
        }

        Matcher matcher = PRODUCT_URL_PATTERN.matcher(href);
        if (!matcher.find()) {
            return;
        }

        String path = matcher.group(1);
        if (path == null || path.isBlank() || !path.startsWith("/s/")) {
            return;
        }

        urls.add(SKROUTZ_BASE_URL + path);
    }

    private String extractTitle(Document doc, String html) {
        String metaTitle = doc.select("meta[property=og:title]").attr("content");
        if (metaTitle != null && !metaTitle.isBlank()) {
            return metaTitle.trim();
        }

        String title = doc.title();
        if (title != null && !title.isBlank()) {
            return title.trim();
        }

        Matcher matcher = TITLE_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", " ").trim();
        }

        return "";
    }

    private boolean matchesTarget(String pageTitle, String query) {
        String normalizedTitle = normalize(pageTitle);
        String[] queryTokens = normalize(query).split(" ");

        int meaningfulTokens = 0;
        int matchedTokens = 0;

        for (String token : queryTokens) {
            if (token.length() < 3 || isStopWord(token)) {
                continue;
            }
            meaningfulTokens++;
            if (normalizedTitle.contains(token)) {
                matchedTokens++;
            }
        }

        if (meaningfulTokens == 0) {
            return true;
        }

        if (meaningfulTokens <= 2) {
            return matchedTokens >= 1;
        }

        return matchedTokens >= Math.max(2, meaningfulTokens - 1);
    }

    private int countTokenMatches(String normalizedTitle, String normalizedQuery) {
        if (normalizedTitle == null || normalizedTitle.isBlank() || normalizedQuery == null || normalizedQuery.isBlank()) {
            return 0;
        }

        int matches = 0;
        Set<String> seen = new LinkedHashSet<>();
        for (String token : normalizedQuery.split(" ")) {
            if (token.length() < 3 || isStopWord(token) || seen.contains(token)) {
                continue;
            }
            seen.add(token);
            if (normalizedTitle.contains(token)) {
                matches++;
            }
        }
        return matches;
    }

    private boolean isStopWord(String token) {
        return switch (token) {
            case "amd", "intel", "nvidia", "geforce", "radeon", "cpu", "gpu", "ssd", "nvme",
                    "ddr5", "ddr4", "ram", "psu", "wifi", "gaming", "core", "pro", "plus", "ax", "gx",
                    "epikse", "epexergastis", "karta", "grafikon", "mitriki", "trofodotiko",
                    "socket", "desktop", "for", "with", "and" -> true;
            default -> false;
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lowered = value.toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double parseEuroAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1.0;
        }

        String normalized = raw.trim()
                .replace("€", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(',', '.');

        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private String buildDiscoveryQuery(MarketplaceCategory category, String userQuery) {
        String query = userQuery == null ? "" : userQuery.trim();
        if (query.isEmpty()) {
            return category.searchSuffix();
        }
        return query + " " + category.searchSuffix();
    }

    private Object buildImportedPart(MarketplaceCategory category, String title, String pageText,
                                     double priceEur, String productUrl, String searchQuery) {
        return switch (category) {
            case CPU -> buildCpu(title, pageText, priceEur, productUrl, searchQuery);
            case GPU -> buildGpu(title, pageText, priceEur, productUrl, searchQuery);
            case MOTHERBOARD -> buildMotherboard(title, pageText, priceEur, productUrl, searchQuery);
            case RAM -> buildRam(title, pageText, priceEur, productUrl, searchQuery);
            case STORAGE -> buildStorage(title, pageText, priceEur, productUrl, searchQuery);
            case PSU -> buildPsu(title, pageText, priceEur, productUrl, searchQuery);
            case CASE -> buildCase(title, pageText, priceEur, productUrl, searchQuery);
            case COOLER -> buildCooler(title, pageText, priceEur, productUrl, searchQuery);
        };
    }

    private CPU buildCpu(String title, String text, double priceEur, String productUrl, String searchQuery) {
        String brand = title.toLowerCase(Locale.ROOT).contains("intel") ? "Intel" : "AMD";
        String model = extractMatch(CPU_MODEL_PATTERN, title);
        if (model.isBlank()) {
            model = title;
        }
        String socket = uppercaseOrEmpty(extractMatch(SOCKET_PATTERN, text));
        if (socket.isBlank()) {
            socket = inferCpuSocket(model);
        }
        int cores = parseInt(extractMatch(CORES_PATTERN, text));
        if (cores <= 0) {
            cores = inferCpuCores(model);
        }
        double baseClock = parseDoubleFlexible(extractMatch(GHZ_PATTERN, text));
        int tdp = parseInt(extractMatch(TDP_PATTERN, text));
        if (tdp <= 0) {
            tdp = inferCpuTdp(model);
        }
        double perfScore = estimateCpuPerf(model, cores, baseClock);
        return new CPU(buildImportPartId(MarketplaceCategory.CPU, title), brand, model, socket,
                Math.max(cores, 6), baseClock > 0 ? baseClock : 3.5,
                perfScore, tdp > 0 ? tdp : 120, priceEur,
                "", searchQuery, productUrl);
    }

    private GPU buildGpu(String title, String text, double priceEur, String productUrl, String searchQuery) {
        String brand = detectBrand(title);
        if (brand == null || brand.isBlank() || "Unknown".equalsIgnoreCase(brand)) {
            brand = title.toLowerCase(Locale.ROOT).contains("rx") || title.toLowerCase(Locale.ROOT).contains("radeon") ? "AMD" : "NVIDIA";
        }
        String model = extractMatch(GPU_MODEL_PATTERN, title);
        if (model.isBlank()) {
            model = title;
        } else if (!looksLikeGenericGpuChipTitle(title)) {
            model = cleanupModelTitle(title, brand);
        }
        int inferredVram = inferGpuVram(model);
        int vram = parseInt(extractMatch(VRAM_PATTERN, text));
        if (vram <= 0 || (inferredVram > 0 && Math.abs(vram - inferredVram) >= 6)) {
            vram = inferredVram;
        }

        int inferredTdp = inferGpuTdp(model);
        int parsedTdp = parseInt(extractMatch(TDP_PATTERN, text));
        int tdp = inferredTdp > 0 ? inferredTdp : parsedTdp;
        if (parsedTdp > 0 && inferredTdp <= 0) {
            tdp = parsedTdp;
        }
        if (inferredTdp > 0 && parsedTdp > 0) {
            double upperRail = Math.max(90.0, inferredTdp * 1.35);
            double lowerRail = inferredTdp * 0.55;
            if (parsedTdp >= lowerRail && parsedTdp <= upperRail) {
                tdp = parsedTdp;
            }
        }

        int lengthMm = extractGpuLengthMm(title + " " + text, model);
        double perf = estimateGpuPerf(model);
        return new GPU(buildImportPartId(MarketplaceCategory.GPU, title), brand, model,
                Math.max(vram, 8), perf, tdp > 0 ? tdp : 220, lengthMm, priceEur,
                "", searchQuery, productUrl);
    }

    private int extractGpuLengthMm(String text, String model) {
        String combined = (model == null ? "" : model) + " " + (text == null ? "" : text);



        int labeledLength = extractMmLabeled(combined,
                "μήκος", "μηκος", "length", "card length", "gpu length", "graphics card length",
                "vga length", "μήκος κάρτας", "μηκος καρτας", "κάρτα γραφικών", "καρτα γραφικων");
        if (labeledLength >= 150 && labeledLength <= 500) {
            return labeledLength;
        }

        int parsed = extractMmAfterKeywords(text,
                "length", "card length", "gpu length", "graphics card length", "vga length",
                "dimensions", "διαστάσεις", "διαστασεις", "μήκος", "μηκος", "μήκος κάρτας", "κάρτα γραφικών");
        if (parsed >= 150 && parsed <= 500) {
            return parsed;
        }

        String normalized = normalize(combined);


        if (normalized.contains("5090") && normalized.contains("astral")) return 358;
        if (normalized.contains("5090")) return 340;
        if (normalized.contains("4090")) return 340;
        if (normalized.contains("4080")) return 320;
        if (normalized.contains("4070")) return 300;
        if (normalized.contains("7900")) return 320;
        if (normalized.contains("7800")) return 300;
        if (normalized.contains("7600")) return 240;
        return 300;
    }

    private boolean looksLikeGenericGpuChipTitle(String title) {
        String normalized = normalize(title);
        return normalized.matches("^(nvidia )?(geforce )?rtx \\d{4}( ti)?( super)?$")
                || normalized.matches("^(amd )?(radeon )?rx \\d{4}( xt| xtx)?$");
    }

    private Motherboard buildMotherboard(String title, String text, double priceEur, String productUrl, String searchQuery) {
        String brand = detectBrand(title);
        String combined = title + " " + text;
        String socket = uppercaseOrEmpty(extractMatch(SOCKET_PATTERN, combined));
        if (socket.isBlank()) {
            socket = inferMotherboardSocket(combined);
        }
        String formFactor = inferFormFactor(combined);
        String model = cleanupModelTitle(title, brand);
        String memoryType = inferMotherboardMemoryType(combined, socket, model);
        return new Motherboard(buildImportPartId(MarketplaceCategory.MOTHERBOARD, title), brand, model,
                socket.isBlank() ? "UNKNOWN" : socket,
                formFactor.isBlank() ? "Unknown" : formFactor,
                memoryType.isBlank() ? "UNKNOWN" : memoryType,
                priceEur, "", searchQuery, productUrl);
    }

    private RAM buildRam(String title, String text, double priceEur, String productUrl, String searchQuery) {
        title = selectRamTitle(title, text, productUrl);
        String brand = detectBrand(title);
        int sticks = extractRamSticks(title + " " + text);
        int totalCapacity = extractRamTotalCapacity(title, text, sticks);
        int speed = extractRamSpeed(title, text);
        String model = cleanupModelTitle(title, brand);
        if (model.isBlank() || looksLikeGenericRamTitle(model) || looksLikeRamFeatureLine(model)) {
            model = sanitizeImportedTitle(titleFromProductUrl(productUrl));
            if (brand != null && !brand.isBlank()) {
                model = model.replaceFirst("(?i)^" + Pattern.quote(brand) + "\\s+", "");
            }
        }
        return new RAM(buildImportPartId(MarketplaceCategory.RAM, title), brand, model,
                Math.max(totalCapacity, 16), Math.max(sticks, 1), Math.max(speed, 3200), priceEur,
                "", searchQuery, productUrl);
    }

    private String selectRamTitle(String title, String text, String productUrl) {
        String current = sanitizeImportedTitle(title);
        String rendered = bestRenderedRamTitle(text);
        String urlTitle = sanitizeImportedTitle(titleFromProductUrl(productUrl));

        if (!rendered.isBlank() && !looksLikeGenericRamTitle(rendered) && !looksLikeRamFeatureLine(rendered)) {
            return rendered;
        }
        if (!current.isBlank() && !looksLikeGenericRamTitle(current) && !looksLikeRamFeatureLine(current)) {
            return current;
        }
        if (!urlTitle.isBlank()) {
            return urlTitle;
        }
        return current;
    }

    private String bestRenderedRamTitle(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String best = "";
        int bestScore = 0;
        for (String line : text.split("\\R")) {
            String trimmed = sanitizeImportedTitle(line != null ? line.trim() : "");
            if (trimmed.length() < 12 || trimmed.length() > 180 || looksLikeNarrativeLine(trimmed)
                    || looksLikeGenericRamTitle(trimmed) || looksLikeRamFeatureLine(trimmed)) {
                continue;
            }
            String normalized = normalize(trimmed);
            if (!containsAny(normalized, "ram", "ddr5", "ddr4", "trident", "viper", "venom", "royal", "ripjaws", "g skill", "gskill", "patriot")) {
                continue;
            }

            int score = 10;
            if (looksLikeRealRamProductTitle(trimmed)) score += 10;
            if (containsAny(normalized, "g skill", "gskill", "patriot", "corsair", "kingston", "viper", "trident", "royal")) score += 5;
            if (extractRamTotalCapacity(trimmed, "", 1) > 0) score += 4;
            if (extractRamSpeed(trimmed, "") > 0) score += 4;
            if (score > bestScore) {
                bestScore = score;
                best = trimmed;
            }
        }
        return best;
    }

    private Storage buildStorage(String title, String text, double priceEur, String productUrl, String searchQuery) {
        String combined = title + " " + text;
        String normalized = normalize(combined);
        String normalizedTitle = normalize(title);



        if (looksLikeCaseProduct(normalized) && !looksLikeStorageProduct(normalizedTitle)) {
            throw new IllegalArgumentException("This product looks like a PC case, not storage: " + title);
        }
        if (looksLikeCoolerProduct(normalized) && !looksLikeStorageProduct(normalizedTitle)) {
            throw new IllegalArgumentException("This product looks like a CPU cooler, not storage: " + title);
        }

        String brand = detectBrand(title);
        String model = cleanupModelTitle(title, brand);
        int capacityGb = extractCapacityGb(title);
        if (capacityGb <= 0) {
            capacityGb = extractCapacityGb(combined);
        }
        String type = inferStorageType(combined);
        double perf = estimateStoragePerf(type, combined);
        int normalizedCapacityGb = Math.max(capacityGb, 500);
        model = alignStorageModelCapacity(model, normalizedCapacityGb);
        String normalizedType = type.isBlank() ? "SSD" : type;

        return new Storage(buildImportPartId(MarketplaceCategory.STORAGE, title + " " + normalizedCapacityGb + "GB " + normalizedType), brand, model,
                normalizedType, normalizedCapacityGb, perf, priceEur,
                "", searchQuery, productUrl);
    }

    private PSU buildPsu(String title, String text, double priceEur, String productUrl, String searchQuery) {
        String brand = detectBrand(title);
        String model = cleanupModelTitle(title, brand);
        int wattage = parseInt(extractMatch(WATTAGE_PATTERN, title + " " + text));
        return new PSU(buildImportPartId(MarketplaceCategory.PSU, title), brand, model,
                Math.max(wattage, 550), priceEur, "", searchQuery, productUrl);
    }

    private PCCase buildCase(String title, String text, double priceEur, String productUrl, String searchQuery) {
        title = selectCaseTitle(title, text, productUrl);
        String brand = detectBrand(title);
        String model = cleanupModelTitle(title, brand);
        if (model.isBlank() || looksLikeGenericCaseTitle(model) || looksLikeNarrativeLine(model)) {
            model = sanitizeImportedTitle(titleFromProductUrl(productUrl));
            if (brand != null && !brand.isBlank()) {
                model = model.replaceFirst("(?i)^" + Pattern.quote(brand) + "\\s+", "");
            }
        }
        String combined = title + " " + text;
        String normalized = normalize(combined);

        String supportedFormFactors = inferCaseSupportedFormFactors(normalized);

        int maxGpuLength = extractMmAfterKeywords(combined,
                "max gpu", "gpu clearance", "vga", "graphics card", "κάρτα γραφικών");
        if (maxGpuLength <= 0) {
            maxGpuLength = 360;
        }

        int maxCoolerHeight = extractMmAfterKeywords(combined,
                "cpu cooler", "cooler height", "ύψος ψύκτρας", "ψύκτρα");
        if (maxCoolerHeight <= 0) {
            maxCoolerHeight = 165;
        }

        boolean supports240 = normalized.contains("240");
        boolean supports280 = normalized.contains("280");
        boolean supports360 = normalized.contains("360");

        return new PCCase(
                buildImportPartId(MarketplaceCategory.CASE, title),
                brand,
                model,
                supportedFormFactors,
                maxGpuLength,
                maxCoolerHeight,
                supports240,
                supports280,
                supports360,
                priceEur,
                "",
                searchQuery,
                productUrl
        );
    }

    private String selectCaseTitle(String title, String text, String productUrl) {
        String current = sanitizeImportedTitle(title);
        String rendered = bestRenderedCaseTitle(text);
        String urlTitle = sanitizeImportedTitle(titleFromProductUrl(productUrl));

        if (!rendered.isBlank() && !looksLikeNarrativeLine(rendered) && !looksLikeGenericCaseTitle(rendered)) {
            return rendered;
        }
        if (!current.isBlank() && !looksLikeNarrativeLine(current) && !looksLikeGenericCaseTitle(current)) {
            return current;
        }
        if (!urlTitle.isBlank()) {
            return urlTitle;
        }
        return current;
    }

    private String bestRenderedCaseTitle(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String best = "";
        int bestScore = 0;
        for (String line : text.split("\\R")) {
            String trimmed = sanitizeImportedTitle(line != null ? line.trim() : "");
            if (trimmed.length() < 12 || trimmed.length() > 180 || looksLikeNarrativeLine(trimmed)) {
                continue;
            }
            String normalized = normalize(trimmed);
            if (!looksLikeCaseProduct(normalized)) {
                continue;
            }

            int score = 10;
            if (looksLikeRealCaseProductTitle(trimmed)) score += 8;
            if (containsAny(normalized, "lian li", "progaming", "fractal", "nzxt", "thermaltake", "montech", "phanteks", "silverstone", "asus", "corsair")) score += 5;
            if (!looksLikeGenericCaseTitle(trimmed)) score += 3;
            if (score > bestScore) {
                bestScore = score;
                best = trimmed;
            }
        }
        return best;
    }

    private Cooler buildCooler(String title, String text, double priceEur, String productUrl, String searchQuery) {
        String brand = detectBrand(title);
        String model = cleanupModelTitle(title, brand);
        String combined = title + " " + text;
        String normalized = normalize(combined);

        boolean isAio = containsAny(normalized,
                "aio", "liquid", "υδροψυξη", "liquid freezer", "water cooler", "h100i", "h150i");

        int radiator = 0;
        if (isAio) {
            if (normalized.contains("360")) radiator = 360;
            else if (normalized.contains("280")) radiator = 280;
            else if (normalized.contains("240")) radiator = 240;
            else if (normalized.contains("120")) radiator = 120;
            else radiator = 240;
        }

        int height = 0;
        if (!isAio) {
            height = extractMmAfterKeywords(combined,
                    "height", "cpu cooler", "cooler height", "ύψος", "ψύκτρα");
            if (height <= 0) {
                height = 155;
            }
        }

        int tdpRating = isAio ? 280 : 220;

        return new Cooler(
                buildImportPartId(MarketplaceCategory.COOLER, title),
                brand,
                model,
                isAio ? Cooler.CoolerType.AIO : Cooler.CoolerType.AIR,
                height,
                radiator,
                tdpRating,
                priceEur,
                "",
                searchQuery,
                productUrl
        );
    }

    private int extractMmAfterKeywords(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return -1;
        }

        String normalizedText = normalize(text);

        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);
            int idx = normalizedText.indexOf(normalizedKeyword);
            if (idx < 0) {
                continue;
            }

            int end = Math.min(normalizedText.length(), idx + 140);
            String window = normalizedText.substring(idx, end);


            Matcher matcher = Pattern.compile("(\\d{2,3})(?:\\s+([0-9]))?\\s*mm", Pattern.CASE_INSENSITIVE).matcher(window);
            while (matcher.find()) {
                double value = parseDoubleFlexible(matcher.group(1) + (matcher.group(2) != null ? "." + matcher.group(2) : ""));
                int rounded = (int) Math.round(value);
                if (rounded >= 80 && rounded <= 500) {
                    return rounded;
                }
            }
        }

        return -1;
    }

    private int extractMmLabeled(String text, String... labels) {
        if (text == null || text.isBlank()) {
            return -1;
        }

        String raw = text.toLowerCase(Locale.ROOT);
        for (String label : labels) {
            String labelRegex = Pattern.quote(label.toLowerCase(Locale.ROOT));
            Pattern pattern = Pattern.compile(labelRegex + "[^0-9]{0,100}(\\d{2,3}(?:[\\.,]\\d+)?)\\s*mm",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(raw);
            if (matcher.find()) {
                int value = (int) Math.round(parseDoubleFlexible(matcher.group(1)));
                if (value >= 80 && value <= 500) {
                    return value;
                }
            }
        }


        Pattern generic = Pattern.compile("(\\d{2,3}(?:[\\.,]\\d+)?)\\s*mm", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = generic.matcher(raw);
        int best = -1;
        while (matcher.find()) {
            int value = (int) Math.round(parseDoubleFlexible(matcher.group(1)));
            if (value >= 150 && value <= 500) {
                best = Math.max(best, value);
            }
        }
        return best;
    }

    private String inferCaseSupportedFormFactors(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return "ATX;mATX;ITX";
        }


        if (containsAny(normalized, "lian li a3", "a3 matx", "a3 m atx", "a3 m-atx", "a3-matx")) {
            return "mATX;ITX";
        }

        boolean supportsAtx = containsAny(normalized, "atx")
                && !containsAny(normalized, "matx", "m atx", "micro atx", "micro-atx");
        boolean supportsMatx = containsAny(normalized, "matx", "m atx", "micro atx", "micro-atx");
        boolean supportsItx = containsAny(normalized, "mini itx", "mini-itx", " itx ", "itx");

        if (supportsAtx) {
            return "ATX;mATX;ITX";
        }
        if (supportsMatx) {
            return "mATX;ITX";
        }
        if (supportsItx && containsAny(normalized, "mini itx", "mini-itx")) {
            return "ITX";
        }


        return "ATX;mATX;ITX";
    }

    private String buildSpecsSummary(Object importedPart) {
        if (importedPart instanceof CPU cpu) {
            return cpu.socket() + " | " + cpu.cores() + "C | " + String.format(Locale.ROOT, "%.1fGHz", cpu.baseClockGhz());
        }
        if (importedPart instanceof GPU gpu) {
            return gpu.model() + " | " + gpu.vramGb() + "GB | " + gpu.tdpW() + "W | " + gpu.lengthMm() + "mm";
        }
        if (importedPart instanceof Motherboard mobo) {
            return mobo.socket() + " | " + mobo.formFactor() + " | " + mobo.memoryType();
        }
        if (importedPart instanceof RAM ram) {
            return ram.capacityGb() + "GB | " + ram.sticks() + " sticks | " + ram.speedMhz() + " MT/s";
        }
        if (importedPart instanceof Storage storage) {
            return storage.type() + " | " + storage.capacityGb() + "GB | score " + String.format(Locale.ROOT, "%.1f", storage.perfScore());
        }
        if (importedPart instanceof PSU psu) {
            return psu.wattageW() + "W";
        }
        if (importedPart instanceof Cooler cooler) {
            if (cooler.isAioCooler()) {
                return "AIO " + cooler.radiatorSizeMm() + "mm | " + cooler.tdpRatingW() + "W rating";
            }
            return "Air cooler | " + cooler.heightMm() + "mm height | " + cooler.tdpRatingW() + "W rating";
        }
        if (importedPart instanceof PCCase pcCase) {
            return "supports " + pcCase.supportedFormFactors()
                    + " | max GPU " + pcCase.maxGpuLengthMm() + "mm"
                    + " | cooler " + pcCase.maxAirCoolerHeightMm() + "mm";
        }
        return "Specs could not be parsed automatically.";
    }

    private String detectBrand(String title) {
        String normalized = normalize(title);
        Map<String, String> pretty = new LinkedHashMap<>();
        pretty.put("g skill", "G.Skill");
        pretty.put("gskill", "G.Skill");
        pretty.put("be quiet", "be quiet!");
        pretty.put("bequiet", "be quiet!");
        pretty.put("western digital", "WD");
        pretty.put("fractal design", "Fractal Design");
        pretty.put("fractal", "Fractal Design");
        pretty.put("lian li", "Lian Li");
        pretty.put("nzxt", "NZXT");
        pretty.put("cooler master", "Cooler Master");
        pretty.put("thermaltake", "Thermaltake");
        pretty.put("phanteks", "Phanteks");
        pretty.put("silverstone", "SilverStone");
        pretty.put("montech", "Montech");
        pretty.put("noctua", "Noctua");
        pretty.put("thermalright", "Thermalright");
        pretty.put("arctic", "ARCTIC");
        pretty.put("patriot", "Patriot");

        for (String brand : KNOWN_BRANDS) {
            String normalizedBrand = normalize(brand);
            if (normalized.contains(normalizedBrand)) {
                return pretty.getOrDefault(normalizedBrand, capitalizeBrand(brand));
            }
        }
        String[] tokens = title.split(" ");
        return tokens.length > 0 ? tokens[0] : "Unknown";
    }

    private String capitalizeBrand(String brand) {
        return switch (brand) {
            case "amd" -> "AMD";
            case "intel" -> "Intel";
            case "nvidia" -> "NVIDIA";
            case "msi" -> "MSI";
            case "asus" -> "ASUS";
            case "wd" -> "WD";
            default -> {
                String[] parts = brand.split(" ");
                StringBuilder sb = new StringBuilder();
                for (String part : parts) {
                    if (part.isBlank()) continue;
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
                yield sb.toString();
            }
        };
    }

    private String cleanupModelTitle(String title, String brand) {
        String model = sanitizeImportedTitle(title);
        if (brand != null && !brand.isBlank()) {
            model = model.replaceFirst("(?i)^" + Pattern.quote(brand) + "\\s+", "");
        }
        model = model.replace("Skroutz", "").replace("|", " ").replaceAll("\\s+", " ").trim();
        return prettifyImportedModel(model);
    }

    private String prettifyImportedModel(String model) {
        if (model == null || model.isBlank()) {
            return "";
        }

        String trimmed = model.replace('-', ' ').replaceAll("\\s+", " ").trim();
        String normalized = normalize(trimmed);
        if (!trimmed.equals(trimmed.toLowerCase(Locale.ROOT)) && !normalized.contains(" typou ")) {
            return normalizeKnownHardwareTerms(trimmed);
        }

        StringBuilder pretty = new StringBuilder();
        for (String token : trimmed.split(" ")) {
            if (token.isBlank()) {
                continue;
            }
            if (pretty.length() > 0) {
                pretty.append(' ');
            }
            pretty.append(prettifyHardwareToken(token));
        }
        return normalizeKnownHardwareTerms(pretty.toString());
    }

    private String prettifyHardwareToken(String token) {
        String clean = token.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.matches("[a-z]+\\d+[a-z0-9]*")) {
            return clean.toUpperCase(Locale.ROOT);
        }
        if (lower.matches("\\d+[a-z]+")) {
            return clean.toUpperCase(Locale.ROOT);
        }
        if (lower.length() <= 2) {
            return lower.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String normalizeKnownHardwareTerms(String value) {
        return value
                .replaceAll("(?i)\\bDdr([345])\\b", "DDR$1")
                .replaceAll("(?i)\\bGddr([67])\\b", "GDDR$1")
                .replaceAll("(?i)\\bAtx\\b", "ATX")
                .replaceAll("(?i)\\bMatx\\b", "mATX")
                .replaceAll("(?i)\\bItx\\b", "ITX")
                .replaceAll("(?i)\\bAm([45])\\b", "AM$1")
                .replaceAll("(?i)\\bUsb\\b", "USB")
                .replaceAll("(?i)\\bWifi\\b", "WiFi")
                .replaceAll("(?i)\\bPcie\\b", "PCIe")
                .replaceAll("(?i)\\bNvme\\b", "NVMe")
                .replaceAll("(?i)\\bRgb\\b", "RGB")
                .replaceAll("(?i)\\bArgb\\b", "ARGB")
                .replaceAll("(?i)\\bPsu\\b", "PSU")
                .replaceAll("(?i)\\bSsd\\b", "SSD")
                .replaceAll("(?i)\\bHdd\\b", "HDD")
                .replaceAll("(?i)\\bCpu\\b", "CPU")
                .replaceAll("(?i)\\bGpu\\b", "GPU")
                .replaceAll("(?i)\\bAio\\b", "AIO")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sanitizeImportedTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String cleaned = title
                .replace('|', ' ')
                .replaceAll("(?i)\\.html(?:\\?.*)?$", "")
                .replaceAll("(?i)\\?product_id=.*$", "")
                .replaceAll("(?i)&sponsored[-_]?listing.*$", "")
                .replaceAll("(?i)&from=.*$", "")
                .replaceAll("(?i)\\s+Skroutz\\s*$", "")
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned;
    }

    private String alignStorageModelCapacity(String model, int capacityGb) {
        if (model == null || model.isBlank() || capacityGb <= 0) {
            return model;
        }

        String capacityText = capacityGb >= 1000 && capacityGb % 1000 == 0
                ? (capacityGb / 1000) + "TB"
                : capacityGb + "GB";
        String aligned = model.replaceFirst("(?i)\\b\\d+(?:[.,]\\d+)?\\s*TB\\b(?!W)", capacityText);
        aligned = aligned.replaceFirst("(?i)\\b\\d{3,5}\\s*GB\\b", capacityText);
        return aligned.replaceAll("\\s+", " ").trim();
    }

    private String uppercaseOrEmpty(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private String extractMatch(Pattern pattern, String text) {
        if (text == null) return "";
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDoubleFlexible(String value) {
        if (value == null || value.isBlank()) return 0.0;
        try {
            return Double.parseDouble(value.replace(',', '.').replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int extractCapacityGb(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        Matcher tb = CAPACITY_TB_PATTERN.matcher(text);
        int bestTb = 0;
        while (tb.find()) {
            if (isStorageEnduranceTb(text, tb.start(), tb.end())) {
                continue;
            }
            int value = (int) Math.round(parseDoubleFlexible(tb.group(1)) * 1000.0);
            if (value > bestTb && value <= 64000) {
                bestTb = value;
            }
        }
        if (bestTb > 0) {
            return bestTb;
        }

        Matcher gb = CAPACITY_GB_PATTERN.matcher(text);
        int best = 0;
        while (gb.find()) {
            int value = parseInt(gb.group(1));
            if (value > best && value <= 64000) {
                best = value;
            }
        }
        return best;
    }

    private boolean isStorageEnduranceTb(String text, int matchStart, int matchEnd) {
        if (text == null) {
            return false;
        }
        int afterEnd = Math.min(text.length(), matchEnd + 8);
        String after = text.substring(matchEnd, afterEnd).toLowerCase(Locale.ROOT);
        if (after.startsWith("w") || after.contains("written")) {
            return true;
        }

        int beforeStart = Math.max(0, matchStart - 48);
        String before = normalize(text.substring(beforeStart, matchStart));
        return containsAny(before, "tbw", "total bytes written", "bytes written", "endurance");
    }

    private int extractRamSticks(String text) {
        Matcher matcher = KIT_PATTERN.matcher(text);
        if (matcher.find()) {
            return parseInt(matcher.group(1));
        }
        return text.toLowerCase(Locale.ROOT).contains("kit") ? 2 : 1;
    }

    private int extractRamTotalCapacity(String title, String text, int sticks) {
        Matcher kitTitle = KIT_PATTERN.matcher(title);
        if (kitTitle.find()) {
            int kitSticks = parseInt(kitTitle.group(1));
            int perStickGb = parseInt(kitTitle.group(2));
            if (kitSticks > 0 && perStickGb > 0) {
                return kitSticks * perStickGb;
            }
        }

        Matcher totalTitle = RAM_TOTAL_GB_PATTERN.matcher(title);
        if (totalTitle.find()) {
            int total = parseInt(totalTitle.group(1));
            if (total >= 4 && total <= 256) {
                return total;
            }
        }

        Matcher anyGbTitle = Pattern.compile("(?<!\\d)([0-9]{1,3})\\s*GB(?!\\d)", Pattern.CASE_INSENSITIVE).matcher(title);
        int bestTitle = 0;
        while (anyGbTitle.find()) {
            int value = parseInt(anyGbTitle.group(1));
            if (value >= 4 && value <= 256) {
                bestTitle = Math.max(bestTitle, value);
            }
        }
        if (bestTitle > 0) {
            return bestTitle;
        }

        Matcher kitText = KIT_PATTERN.matcher(text);
        if (kitText.find()) {
            int kitSticks = parseInt(kitText.group(1));
            int perStickGb = parseInt(kitText.group(2));
            if (kitSticks > 0 && perStickGb > 0) {
                return kitSticks * perStickGb;
            }
        }

        Matcher totalText = RAM_TOTAL_GB_PATTERN.matcher(text);
        if (totalText.find()) {
            int total = parseInt(totalText.group(1));
            if (total >= 4 && total <= 256) {
                return total;
            }
        }

        return sticks > 1 ? sticks * 8 : 16;
    }

    private int extractRamSpeed(String title, String text) {
        Matcher labeledTitle = RAM_SPEED_LABEL_PATTERN.matcher(title);
        while (labeledTitle.find()) {
            int value = parseInt(labeledTitle.group(1));
            if (value >= 2133 && value <= 9600) {
                return value;
            }
        }

        Matcher titleMatcher = SPEED_PATTERN.matcher(title);
        int bestTitle = 0;
        while (titleMatcher.find()) {
            int value = parseInt(titleMatcher.group(1));
            if (value >= 2133 && value <= 9600) {
                bestTitle = Math.max(bestTitle, value);
            }
        }
        if (bestTitle > 0) {
            return bestTitle;
        }

        Matcher labeledText = RAM_SPEED_LABEL_PATTERN.matcher(text);
        while (labeledText.find()) {
            int value = parseInt(labeledText.group(1));
            if (value >= 2133 && value <= 9600) {
                return value;
            }
        }

        Matcher textMatcher = SPEED_PATTERN.matcher(text);
        int bestText = 0;
        while (textMatcher.find()) {
            int value = parseInt(textMatcher.group(1));
            if (value >= 2133 && value <= 9600) {
                bestText = Math.max(bestText, value);
            }
        }
        return bestText;
    }

    private String inferStorageType(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        String normalized = normalize(text);
        if (lower.contains("nvme")
                || containsAny(normalized, "pci express", "pcie", "m 2", "m2", "mz v8p", "mz v9p", "980 pro", "990 pro", "kc3000", "skc3000")) {
            return "NVMe";
        }
        if (lower.contains("sata") && lower.contains("ssd")) return "SATA SSD";
        if (lower.contains("hdd") || lower.contains("hard disk")) return "HDD";
        if (lower.contains("ssd")) return "SSD";
        return "";
    }

    private String inferFormFactor(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("mini itx") || lower.contains("mini-itx") || lower.contains("itx")) return "ITX";
        if (lower.contains("micro atx") || lower.contains("m-atx") || lower.contains("matx")) return "mATX";
        if (lower.contains("e-atx") || lower.contains("eatx")) return "E-ATX";
        if (lower.contains("atx")) return "ATX";
        return "";
    }

    private String inferMotherboardSocket(String text) {
        String normalized = normalize(text);

        if (containsAny(normalized, "lga1700", "lga 1700", "socket 1700", "1700 socket", "intel 1700", "intel socket 1700")) {
            return "LGA1700";
        }
        if (containsAny(normalized, "lga1851", "lga 1851", "socket 1851", "1851 socket", "intel 1851", "intel socket 1851")) {
            return "LGA1851";
        }
        if (containsAny(normalized, "socket am5", "am5 socket", "am5")) {
            return "AM5";
        }
        if (containsAny(normalized, "socket am4", "am4 socket", "am4")) {
            return "AM4";
        }
        if (containsAny(normalized, "socket tr5", "tr5 socket", "str5")) {
            return "TR5";
        }

        return "UNKNOWN";
    }

    private String inferMotherboardMemoryType(String text, String socket, String model) {
        String normalized = normalize(text);



        if (containsAny(normalized, "ddr5", "4x ddr5", "memory ddr5", "mnimi ddr5", "μνημη ddr5")) {
            return "DDR5";
        }
        if (containsAny(normalized, "ddr4", "4x ddr4", "memory ddr4", "mnimi ddr4", "μνημη ddr4")) {
            return "DDR4";
        }

        return Motherboard.inferMemoryType(socket, model);
    }

    private String inferCpuSocket(String model) {
        String lower = model.toLowerCase(Locale.ROOT);
        if (lower.contains("ryzen")) {
            Matcher m = Pattern.compile("(\\d{4,5})").matcher(lower);
            if (m.find()) {
                int n = parseInt(m.group(1));
                if (n >= 7000) return "AM5";
                return "AM4";
            }
            return "AM5";
        }
        if (lower.contains("ultra")) return "LGA1851";
        if (lower.contains("core i")) return "LGA1700";
        return "UNKNOWN";
    }

    private int inferCpuCores(String model) {
        String lower = model.toLowerCase(Locale.ROOT);
        if (lower.contains("9950") || lower.contains("7950") || lower.contains("14900")) return 16;
        if (lower.contains("9900") || lower.contains("7900") || lower.contains("14700") || lower.contains("13700")) return 12;
        if (lower.contains("9800") || lower.contains("7800") || lower.contains("7700") || lower.contains("i7")) return 8;
        if (lower.contains("7600") || lower.contains("14600") || lower.contains("13600") || lower.contains("i5")) return 6;
        return 8;
    }

    private int inferCpuTdp(String model) {
        String lower = model.toLowerCase(Locale.ROOT);
        if (lower.contains("x3d")) return 120;
        if (lower.contains("k")) return 125;
        if (lower.contains("ryzen 5 7600")) return 65;
        return 105;
    }

    private double estimateCpuPerf(String model, int cores, double baseClock) {
        String lower = model.toLowerCase(Locale.ROOT);
        if (lower.contains("9950x3d")) return 100;
        if (lower.contains("9800x3d")) return 98;
        if (lower.contains("7950x3d")) return 99;
        if (lower.contains("7900x3d")) return 96;
        if (lower.contains("7800x3d")) return 95;
        if (lower.contains("14900k")) return 97;
        if (lower.contains("14700k")) return 90;
        if (lower.contains("14600k")) return 86;
        if (lower.contains("13600k")) return 85;
        if (lower.contains("7700x")) return 82;
        if (lower.contains("7700")) return 80;
        if (lower.contains("7600x")) return 78;
        if (lower.contains("7600")) return 75;

        double score = 45.0;
        String normalized = lower.replace("-", " ");
        if (normalized.contains("ryzen 9") || normalized.contains("i9")) score += 28;
        else if (normalized.contains("ryzen 7") || normalized.contains("i7") || normalized.contains("ultra 7")) score += 20;
        else if (normalized.contains("ryzen 5") || normalized.contains("i5") || normalized.contains("ultra 5")) score += 14;
        score += Math.max(0, cores - 6) * 1.8;
        score += Math.max(0, baseClock - 3.5) * 4.0;
        if (normalized.contains("x3d")) score += 10;
        if (normalized.contains("k")) score += 3;
        return Math.min(100.0, Math.max(55.0, score));
    }

    private int inferGpuVram(String model) {
        String lower = model.toLowerCase(Locale.ROOT);
        if (lower.contains("5090")) return 32;
        if (lower.contains("5080")) return 16;
        if (lower.contains("5070 ti")) return 16;
        if (lower.contains("5070")) return 12;
        if (lower.contains("5060 ti")) return 16;
        if (lower.contains("5060")) return 8;
        if (lower.contains("4090")) return 24;
        if (lower.contains("4080")) return 16;
        if (lower.contains("4070 ti") || lower.contains("4070 super")) return 12;
        if (lower.contains("4070")) return 12;
        if (lower.contains("4060 ti")) return 8;
        if (lower.contains("4060")) return 8;
        if (lower.contains("7900 xtx")) return 24;
        if (lower.contains("7900 xt")) return 20;
        if (lower.contains("7800 xt")) return 16;
        if (lower.contains("7700 xt")) return 12;
        if (lower.contains("7600")) return 8;
        return 8;
    }

    private int inferGpuTdp(String model) {
        String lower = model.toLowerCase(Locale.ROOT);
        if (lower.contains("5090")) return 575;
        if (lower.contains("5080")) return 360;
        if (lower.contains("5070 ti")) return 300;
        if (lower.contains("5070")) return 250;
        if (lower.contains("5060 ti")) return 180;
        if (lower.contains("5060")) return 145;
        if (lower.contains("4090")) return 450;
        if (lower.contains("4080")) return 320;
        if (lower.contains("4070 ti")) return 285;
        if (lower.contains("4070")) return 200;
        if (lower.contains("4060 ti")) return 165;
        if (lower.contains("4060")) return 115;
        if (lower.contains("7900 xtx")) return 355;
        if (lower.contains("7900 xt")) return 315;
        if (lower.contains("7800 xt")) return 260;
        if (lower.contains("7700 xt")) return 245;
        if (lower.contains("7600")) return 165;
        return 220;
    }

    private double estimateGpuPerf(String model) {
        String lower = model.toLowerCase(Locale.ROOT);
        if (lower.contains("5090")) return 100;
        if (lower.contains("4090")) return 95;
        if (lower.contains("7900 xtx")) return 90;
        if (lower.contains("5080")) return 82;
        if (lower.contains("4080 super")) return 84;
        if (lower.contains("4080")) return 82;
        if (lower.contains("7900 xt")) return 80;
        if (lower.contains("5070 ti")) return 74;
        if (lower.contains("4070 ti super")) return 72;
        if (lower.contains("4070 ti")) return 69;
        if (lower.contains("9070 xt")) return 76;
        if (lower.contains("5070")) return 64;
        if (lower.contains("4070 super")) return 64;
        if (lower.contains("7800 xt")) return 63;
        if (lower.contains("9070")) return 66;
        if (lower.contains("4070")) return 58;
        if (lower.contains("7700 xt")) return 56;
        if (lower.contains("5060 ti")) return 52;
        if (lower.contains("4060 ti")) return 47;
        if (lower.contains("7600 xt")) return 46;
        if (lower.contains("5060")) return 43;
        if (lower.contains("4060")) return 40;
        if (lower.contains("7600")) return 38;
        return 55;
    }

    private double estimateStoragePerf(String type, String titleText) {
        String normalized = normalize(titleText);
        if (looksLikeSamsung980Pro(normalized)) return 9.1;
        if (containsAny(normalized, "990 pro", "sn850", "sn850x", "firecuda 530", "kc3000", "skc3000", "mp700 pro")) return 9.2;
        if ("NVMe".equalsIgnoreCase(type)) return 8.0;
        if (type.toLowerCase(Locale.ROOT).contains("sata")) return 7.0;
        if ("HDD".equalsIgnoreCase(type)) return 4.0;
        return 6.5;
    }

    private String buildImportPartId(MarketplaceCategory category, String title) {
        String slug = normalize(title)
                .replace(' ', '_')
                .replaceAll("[^a-z0-9_]+", "")
                .replaceAll("_+", "_");
        if (slug.length() > 42) {
            slug = slug.substring(0, 42);
        }
        return "imp_" + category.name().toLowerCase(Locale.ROOT) + "_" + Integer.toHexString(slug.hashCode()).replace('-', 'x');
    }

    private record PriceMatch(double priceEur, Integer storeCount) {
    }

    private record MatchAnalysis(PriceConfidence confidence, String reason) {
    }
}
