package com.pcbuilder;

import com.pcbuilder.data.CsvCatalogRepository;
import com.pcbuilder.services.BuildScoringService;
import com.pcbuilder.services.CompatibilityService;
import com.pcbuilder.services.PowerEstimatorService;
import com.pcbuilder.pricing.LivePriceService;
import com.pcbuilder.pricing.MarketplaceCategory;
import com.pcbuilder.pricing.MarketplaceSearchResult;
import com.pcbuilder.pricing.PriceQuote;
import com.pcbuilder.pricing.PriceConfidence;
import com.pcbuilder.pricing.PriceRefreshResult;
import com.pcbuilder.pricing.PriceTarget;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.control.Alert;
import javafx.concurrent.Task;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;



import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;


public class MainController {

    private static final int MIN_SAFE_HEADROOM_W = 150;
    private static final DateTimeFormatter LIVE_PRICE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm");


    private CsvCatalogRepository catalogRepository;
    private final PowerEstimatorService powerEstimator = new PowerEstimatorService();
    private final BuildScoringService scoringService = new BuildScoringService();
    private final CompatibilityService compatibilityService = new CompatibilityService(MIN_SAFE_HEADROOM_W);
    private final LivePriceService livePriceService = new LivePriceService();

    @FXML private Label cpuLabel;
    @FXML private Label gpuLabel;
    @FXML private Label moboLabel;
    @FXML private Label ramLabel;
    @FXML private Label storageLabel;
    @FXML private Label psuLabel;
    @FXML private Label caseLabel;
    @FXML private Label coolerLabel;
    @FXML private Label performanceLabel;
    @FXML private Label valueLabel;
    @FXML private Label powerLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Label compatibilityLabel;
    @FXML private Label suggestionLabel;
    @FXML private Label livePriceStatusLabel;

    @FXML private VBox socketCard;
    @FXML private VBox ramCard;
    @FXML private VBox caseFitCard;
    @FXML private VBox gpuClearanceCard;
    @FXML private VBox coolerFitCard;
    @FXML private VBox psuHeadroomCard;

    @FXML private Label socketStatusLabel;
    @FXML private Label socketDetailLabel;
    @FXML private Label ramStatusLabel;
    @FXML private Label ramDetailLabel;
    @FXML private Label caseFitStatusLabel;
    @FXML private Label caseFitDetailLabel;
    @FXML private Label gpuClearanceStatusLabel;
    @FXML private Label gpuClearanceDetailLabel;
    @FXML private Label coolerFitStatusLabel;
    @FXML private Label coolerFitDetailLabel;
    @FXML private Label psuHeadroomStatusLabel;
    @FXML private Label psuHeadroomDetailLabel;


    @FXML private TextField budgetField;


    @FXML private ToggleGroup profileGroup;
    @FXML private RadioButton gamingRadio;
    @FXML private RadioButton creatorRadio;

    @FXML private ToggleGroup psuSafetyGroup;
    @FXML private RadioButton safePsuRadio;
    @FXML private RadioButton aggressivePsuRadio;

    @FXML private Button cpuCaseButton;
    @FXML private Button gpuCaseButton;
    @FXML private Button moboCaseButton;
    @FXML private Button ramCaseButton;
    @FXML private Button storageCaseButton;
    @FXML private Button psuCaseButton;
    @FXML private Button pcCaseButton;
    @FXML private Button coolerCaseButton;
    @FXML private Button refreshLivePricesButton;


    private final List<CPU> cpuCatalog = new ArrayList<>();
    private final List<GPU> gpuCatalog = new ArrayList<>();
    private final List<Motherboard> moboCatalog = new ArrayList<>();
    private final List<RAM> ramCatalog = new ArrayList<>();
    private final List<Storage> storageCatalog = new ArrayList<>();
    private final List<PSU> psuCatalog = new ArrayList<>();
    private final List<PCCase> caseCatalog = new ArrayList<>();
    private final List<Cooler> coolerCatalog = new ArrayList<>();


    private CPU selectedCpu;
    private GPU selectedGpu;
    private Motherboard selectedMobo;
    private RAM selectedRam;
    private Storage selectedStorage;
    private PSU selectedPsu;
    private PCCase selectedCase;
    private Cooler selectedCooler;


    private boolean gamingProfile = true;
    private boolean psuSafeMode = true;


    private static class BuildOption {
        CPU cpu;
        GPU gpu;
        Motherboard mobo;
        PCCase pcCase;
        Cooler cooler;
        RAM ram;
        Storage storage;
        PSU psu;
        double perf;
        double totalPrice;
        double valueScore;
        int estimatedPower;
        int headroom;
    }

    @FXML
    public void initialize() {
        cpuLabel.setText("Επεξεργαστής: (Δεν έχει γίνει επιλογή)");
        gpuLabel.setText("Κάρτα Γραφικών: (Δεν έχει γίνει επιλογή)");
        moboLabel.setText("Μητρική: (Δεν έχει γίνει επιλογή)");
        ramLabel.setText("RAM: (Δεν έχει γίνει επιλογή)");
        storageLabel.setText("Αποθηκευτικός χώρος: (Δεν έχει γίνει επιλογή)");
        psuLabel.setText("Τροφοδοτικό: (Δεν έχει γίνει επιλογή)");
        caseLabel.setText("Κουτί: (Δεν έχει γίνει επιλογή)");
        coolerLabel.setText("Ψύξη CPU: (Δεν έχει γίνει επιλογή)");
        performanceLabel.setText("– / 100");
        valueLabel.setText("– perf/€");
        powerLabel.setText("~– W");
        totalPriceLabel.setText("Συνολική τιμή: €0.00");
        compatibilityLabel.setText("Συμβατότητα: επίλεξε εξαρτήματα για έλεγχο.");
        suggestionLabel.setText("Πρόταση: –");
        if (livePriceStatusLabel != null) livePriceStatusLabel.setText("Live τιμές: Αναμονή για επιλογή εξαρτημάτων.");

        if (cpuCaseButton != null) cpuCaseButton.setText("CPU");
        if (gpuCaseButton != null) gpuCaseButton.setText("GPU");
        if (moboCaseButton != null) moboCaseButton.setText("Μητρική");
        if (ramCaseButton != null) ramCaseButton.setText("RAM");
        if (storageCaseButton != null) storageCaseButton.setText("M.2");
        if (psuCaseButton != null) psuCaseButton.setText("PSU");
        if (pcCaseButton != null) pcCaseButton.setText("Κουτί");
        if (coolerCaseButton != null) coolerCaseButton.setText("Ψύξη");

        gamingProfile = true;
        if (gamingRadio != null) gamingRadio.setSelected(true);

        psuSafeMode = true;
        if (safePsuRadio != null) safePsuRadio.setSelected(true);

        catalogRepository = new CsvCatalogRepository(getClass());
        loadCatalogs();
        if (budgetField != null) {
            budgetField.textProperty().addListener((obs, oldValue, newValue) -> refreshSummary());
        }
        refreshSummary();


    }

    @FXML
    private void onRefreshLivePrices() {
        requestLivePriceRefresh(true);
    }

    private void requestLivePriceRefresh(boolean userTriggered) {
        if (refreshLivePricesButton != null && refreshLivePricesButton.isDisabled()) {
            return;
        }

        if (refreshLivePricesButton != null) {
            refreshLivePricesButton.setDisable(true);
        }
        if (livePriceStatusLabel != null) {
            livePriceStatusLabel.setText("Live τιμές: γίνεται ανανέωση από Skroutz...");
        }

        List<PriceTarget> targets = buildSelectedPriceTargets();
        if (targets.isEmpty()) {
            if (refreshLivePricesButton != null) {
                refreshLivePricesButton.setDisable(false);
            }
            if (livePriceStatusLabel != null) {
                livePriceStatusLabel.setText("Live τιμές: επίλεξε πρώτα εξαρτήματα για ανανέωση.");
            }
            return;
        }

        Task<PriceRefreshResult> refreshTask = new Task<>() {
            @Override
            protected PriceRefreshResult call() {
                return livePriceService.refreshPrices(targets);
            }
        };

        refreshTask.setOnSucceeded(event -> {
            if (refreshLivePricesButton != null) {
                refreshLivePricesButton.setDisable(false);
            }

            PriceRefreshResult result = refreshTask.getValue();
            refreshSummary();

            String timestamp = LIVE_PRICE_TIME_FORMAT.format(
                    result.fetchedAt().atZone(ZoneId.systemDefault())
            );

            if (livePriceStatusLabel != null) {
                livePriceStatusLabel.setText(String.format(
                        "Live τιμές: %d έμπιστες, %d για έλεγχο, %d fallback σε CSV (τελευταία ανανέωση %s).",
                        result.trustedLiveCount(), result.reviewOnlyCount(), result.failedCount(), timestamp
                ));
            }

            if (userTriggered && suggestionLabel != null) {
                suggestionLabel.setText("Πρόταση: Οι live τιμές ανανεώθηκαν. Μόνο exact/high-confidence αποτελέσματα επηρεάζουν το σύνολο.");
            }
        });

        refreshTask.setOnFailed(event -> {
            if (refreshLivePricesButton != null) {
                refreshLivePricesButton.setDisable(false);
            }

            refreshSummary();

            Throwable error = refreshTask.getException();
            String message = (error != null && error.getMessage() != null && !error.getMessage().isBlank())
                    ? error.getMessage()
                    : "see console";

            if (livePriceStatusLabel != null) {
                livePriceStatusLabel.setText("Live τιμές: αποτυχία ανανέωσης, χρήση CSV fallback (" + message + ").");
            }

            if (error != null) {
                error.printStackTrace();
            }
        });

        Thread thread = new Thread(refreshTask, "pcbuilder-skroutz-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    

    @FXML
    private void onProfileChanged() {
        if (profileGroup.getSelectedToggle() == creatorRadio) {
            gamingProfile = false;
        } else {
            gamingProfile = true;
        }
        refreshSummary();
    }

    @FXML
    private void onPsuSafetyChanged() {
        if (psuSafetyGroup.getSelectedToggle() == aggressivePsuRadio) {
            psuSafeMode = false;
        } else {
            psuSafeMode = true;
        }
        refreshSummary();
    }

    private List<PriceTarget> buildPriceTargets() {
        List<PriceTarget> targets = new ArrayList<>();

        for (CPU cpu : cpuCatalog) {
            targets.add(new PriceTarget(cpu.id(), MarketplaceCategory.CPU, cpu.brand() + " " + cpu.model(),
                    cpu.manufacturerPartNumber(), buildSkroutzQuery(cpu), cpu.skroutzUrl(), cpu.priceEur()));
        }
        for (GPU gpu : gpuCatalog) {
            targets.add(new PriceTarget(gpu.id(), MarketplaceCategory.GPU, gpu.brand() + " " + gpu.model(),
                    gpu.manufacturerPartNumber(), buildSkroutzQuery(gpu), gpu.skroutzUrl(), gpu.priceEur()));
        }
        for (Motherboard mobo : moboCatalog) {
            targets.add(new PriceTarget(mobo.id(), MarketplaceCategory.MOTHERBOARD, mobo.brand() + " " + mobo.model(),
                    mobo.manufacturerPartNumber(), buildSkroutzQuery(mobo), mobo.skroutzUrl(), mobo.priceEur()));
        }
        for (RAM ram : ramCatalog) {
            targets.add(new PriceTarget(ram.id(), MarketplaceCategory.RAM, ram.brand() + " " + ram.model(),
                    ram.manufacturerPartNumber(), buildSkroutzQuery(ram), ram.skroutzUrl(), ram.priceEur()));
        }
        for (Storage storage : storageCatalog) {
            targets.add(new PriceTarget(storage.id(), MarketplaceCategory.STORAGE, storage.brand() + " " + storage.model(),
                    storage.manufacturerPartNumber(), buildSkroutzQuery(storage), storage.skroutzUrl(), storage.priceEur()));
        }
        for (PSU psu : psuCatalog) {
            targets.add(new PriceTarget(psu.id(), MarketplaceCategory.PSU, psu.brand() + " " + psu.model(),
                    psu.manufacturerPartNumber(), buildSkroutzQuery(psu), psu.skroutzUrl(), psu.priceEur()));
        }

        for (PCCase pcCase : caseCatalog) {
            targets.add(new PriceTarget(pcCase.id(), MarketplaceCategory.CASE, pcCase.brand() + " " + pcCase.model(),
                    pcCase.manufacturerPartNumber(), buildSkroutzQuery(pcCase), pcCase.skroutzUrl(), pcCase.priceEur()));
        }

        for (Cooler cooler : coolerCatalog) {
            targets.add(new PriceTarget(cooler.id(), MarketplaceCategory.COOLER, cooler.brand() + " " + cooler.model(),
                    cooler.manufacturerPartNumber(), buildSkroutzQuery(cooler), cooler.skroutzUrl(), cooler.priceEur()));
        }

        return targets;
    }

    private List<PriceTarget> buildSelectedPriceTargets() {
        List<PriceTarget> targets = new ArrayList<>();
        if (selectedCpu != null) {
            targets.add(new PriceTarget(selectedCpu.id(), MarketplaceCategory.CPU, selectedCpu.brand() + " " + selectedCpu.model(),
                    selectedCpu.manufacturerPartNumber(), buildSkroutzQuery(selectedCpu), selectedCpu.skroutzUrl(), selectedCpu.priceEur()));
        }
        if (selectedGpu != null) {
            targets.add(new PriceTarget(selectedGpu.id(), MarketplaceCategory.GPU, selectedGpu.brand() + " " + selectedGpu.model(),
                    selectedGpu.manufacturerPartNumber(), buildSkroutzQuery(selectedGpu), selectedGpu.skroutzUrl(), selectedGpu.priceEur()));
        }
        if (selectedMobo != null) {
            targets.add(new PriceTarget(selectedMobo.id(), MarketplaceCategory.MOTHERBOARD, selectedMobo.brand() + " " + selectedMobo.model(),
                    selectedMobo.manufacturerPartNumber(), buildSkroutzQuery(selectedMobo), selectedMobo.skroutzUrl(), selectedMobo.priceEur()));
        }
        if (selectedRam != null) {
            targets.add(new PriceTarget(selectedRam.id(), MarketplaceCategory.RAM, selectedRam.brand() + " " + selectedRam.model(),
                    selectedRam.manufacturerPartNumber(), buildSkroutzQuery(selectedRam), selectedRam.skroutzUrl(), selectedRam.priceEur()));
        }
        if (selectedStorage != null) {
            targets.add(new PriceTarget(selectedStorage.id(), MarketplaceCategory.STORAGE, selectedStorage.brand() + " " + selectedStorage.model(),
                    selectedStorage.manufacturerPartNumber(), buildSkroutzQuery(selectedStorage), selectedStorage.skroutzUrl(), selectedStorage.priceEur()));
        }
        if (selectedPsu != null) {
            targets.add(new PriceTarget(selectedPsu.id(), MarketplaceCategory.PSU, selectedPsu.brand() + " " + selectedPsu.model(),
                    selectedPsu.manufacturerPartNumber(), buildSkroutzQuery(selectedPsu), selectedPsu.skroutzUrl(), selectedPsu.priceEur()));
        }
        if (selectedCase != null) {
            targets.add(new PriceTarget(selectedCase.id(), MarketplaceCategory.CASE, selectedCase.brand() + " " + selectedCase.model(),
                    selectedCase.manufacturerPartNumber(), buildSkroutzQuery(selectedCase), selectedCase.skroutzUrl(), selectedCase.priceEur()));
        }
        if (selectedCooler != null) {
            targets.add(new PriceTarget(selectedCooler.id(), MarketplaceCategory.COOLER, selectedCooler.brand() + " " + selectedCooler.model(),
                    selectedCooler.manufacturerPartNumber(), buildSkroutzQuery(selectedCooler), selectedCooler.skroutzUrl(), selectedCooler.priceEur()));
        }
        return targets;
    }

    private String buildSkroutzQuery(CPU cpu) {
        if (cpu.skroutzQuery() != null && !cpu.skroutzQuery().isBlank()) {
            return cpu.skroutzQuery();
        }
        return cpu.brand() + " " + cpu.model() + " επεξεργαστής";
    }

    private String buildSkroutzQuery(GPU gpu) {
        if (gpu.skroutzQuery() != null && !gpu.skroutzQuery().isBlank()) {
            return gpu.skroutzQuery();
        }
        return switch (gpu.id()) {
            case "rtx_4090" -> "NVIDIA GeForce RTX 4090 24GB κάρτα γραφικών";
            case "rtx_4070" -> "NVIDIA GeForce RTX 4070 12GB κάρτα γραφικών";
            case "rx_7800_xt" -> "AMD Radeon RX 7800 XT 16GB κάρτα γραφικών";
            case "rx_7600" -> "AMD Radeon RX 7600 8GB κάρτα γραφικών";
            default -> gpu.brand() + " " + gpu.model() + " κάρτα γραφικών";
        };
    }

    private String buildSkroutzQuery(Motherboard mobo) {
        if (mobo.skroutzQuery() != null && !mobo.skroutzQuery().isBlank()) {
            return mobo.skroutzQuery();
        }
        return mobo.brand() + " " + mobo.model() + " μητρική";
    }

    private String buildSkroutzQuery(RAM ram) {
        if (ram.skroutzQuery() != null && !ram.skroutzQuery().isBlank()) {
            return ram.skroutzQuery();
        }
        return ram.brand() + " " + ram.model() + " DDR5 RAM";
    }

    private String buildSkroutzQuery(Storage storage) {
        if (storage.skroutzQuery() != null && !storage.skroutzQuery().isBlank()) {
            return storage.skroutzQuery();
        }
        return storage.brand() + " " + storage.model() + " " + storage.capacityGb() + "GB " + storage.type();
    }

    private String buildSkroutzQuery(PSU psu) {
        if (psu.skroutzQuery() != null && !psu.skroutzQuery().isBlank()) {
            return psu.skroutzQuery();
        }
        return psu.brand() + " " + psu.model() + " τροφοδοτικό";
    }

    private String buildSkroutzQuery(PCCase pcCase) {
        if (pcCase.skroutzQuery() != null && !pcCase.skroutzQuery().isBlank()) {
            return pcCase.skroutzQuery();
        }
        return pcCase.brand() + " " + pcCase.model() + " κουτί υπολογιστή";
    }

    private String buildSkroutzQuery(Cooler cooler) {
        if (cooler.skroutzQuery() != null && !cooler.skroutzQuery().isBlank()) {
            return cooler.skroutzQuery();
        }
        if (cooler.isAioCooler()) {
            return cooler.brand() + " " + cooler.model() + " υδρόψυξη επεξεργαστή";
        }
        return cooler.brand() + " " + cooler.model() + " ψύκτρα επεξεργαστή";
    }

    private double resolvePrice(String partId, double fallbackPrice) {
        return livePriceService.resolvePrice(partId, fallbackPrice);
    }

    private String formatPartPrice(String partId, double fallbackPrice) {
        Optional<PriceQuote> quote = livePriceService.getQuote(partId);
        if (quote.isPresent()) {
            PriceQuote q = quote.get();
            if (q.usableForTotals()) {
                return String.format("live €%.2f [%s] | CSV €%.2f", q.priceEur(), q.confidence().label(), fallbackPrice);
            }
            return String.format("review €%.2f [%s, not used] | CSV €%.2f", q.priceEur(), q.confidence().label(), fallbackPrice);
        }
        return String.format("€%.2f (CSV)", fallbackPrice);
    }

    private String formatScore(double score) {
        return String.format(Locale.ROOT, "%.1f", score);
    }

    private String displayName(String brand, String model, String productUrl) {
        String cleanBrand = brand != null ? brand.trim() : "";
        cleanBrand = cleanBrand.replaceAll("(?i)^CoolerMaster$", "Cooler Master");
        String cleanModel = cleanDisplayModel(model);
        String urlModel = cleanDisplayModel(titleFromSkroutzUrl(productUrl));

        if (isWeakDisplayModel(cleanModel) && !urlModel.isBlank()) {
            cleanModel = cleanModelFromUrlBrand(cleanBrand, urlModel);
        }
        if (cleanModel.toLowerCase(Locale.ROOT).startsWith(cleanBrand.toLowerCase(Locale.ROOT) + " ")) {
            cleanModel = cleanModel.substring(cleanBrand.length()).trim();
        }
        if (cleanModel.isBlank()) {
            return cleanBrand.isBlank() ? "Unknown" : cleanBrand;
        }
        return cleanBrand.isBlank() ? cleanModel : cleanBrand + " " + cleanModel;
    }

    private String cleanModelFromUrlBrand(String brand, String urlModel) {
        if (brand == null || brand.isBlank() || urlModel == null) {
            return urlModel != null ? urlModel : "";
        }
        return urlModel.replaceFirst("(?i)^" + java.util.regex.Pattern.quote(brand) + "\\s+", "").trim();
    }

    private boolean isWeakDisplayModel(String model) {
        String lower = model == null ? "" : model.toLowerCase(Locale.ROOT);
        return lower.isBlank()
                || lower.contains("μνήμες ram")
                || lower.contains("mnimes ram")
                || lower.contains("karta grafikon")
                || lower.contains("kouti ypologisti")
                || lower.contains("trofodotiko ypologisti")
                || lower.matches(".*\\b(product_id|sponsored|listing)\\b.*");
    }

    private String cleanDisplayModel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String value = raw.replace('-', ' ')
                .replace('|', ' ')
                .replaceAll("(?i)\\.html(?:\\?.*)?$", "")
                .replaceAll("(?i)\\?product_id=.*$", "")
                .replaceAll("(?i)&sponsored[-_]?listing.*$", "")
                .replaceAll("(?i)\\bKarta Grafikon\\b.*$", "")
                .replaceAll("(?iu)\\s*Κάρτα\\s+Γραφικών.*$", "")
                .replaceAll("(?i)\\bMotherboard\\b", "")
                .replaceAll("(?iu)\\s*Μητρική\\s*", " ")
                .replaceAll("(?i)\\bKouti Ypologisti\\b", "")
                .replaceAll("(?iu)\\s*Κουτί\\s+Υπολογιστή\\s*", " ")
                .replaceAll("(?i)\\bTrofodotiko Ypologisti\\b", "")
                .replaceAll("(?iu)\\s*Τροφοδοτικό\\s+Υπολογιστή\\s*", " ")
                .replaceAll("(?iu)\\s*Υδρόψυξη\\s+AIO\\s+CPU.*$", "")
                .replaceAll("(?iu)\\s*Αερόψυξη\\s+CPU.*$", "")
                .replaceAll("(?i)\\btypou\\b.*$", "")
                .replaceAll("(?iu)\\s*Τύπου.*$", "")
                .replaceAll("(?i)\\bme syndesi\\b.*$", "")
                .replaceAll("(?iu)\\s*με\\s+σύνδεση.*$", "")
                .replaceAll("(?i)\\bME\\s+(AMD|Intel)\\b.*$", "")
                .replaceAll("(?i)\\bSocket\\b.*$", "")
                .replaceAll("(?i)\\bMauro\\b", "Black")
                .replaceAll("(?i)\\bLeyko\\b", "White")
                .replaceAll("(?iu)\\s*Μαύρο\\s*", " ")
                .replaceAll("(?iu)\\s*Λευκή\\s*", " ")
                .replaceAll("(?iu)\\s*Λευκό\\s*", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return normalizeDisplayHardwareTerms(value);
    }

    private String normalizeDisplayHardwareTerms(String value) {
        return value
                .replaceAll("(?i)\\bDdr([345])\\b", "DDR$1")
                .replaceAll("(?i)\\bGddr([67])\\b", "GDDR$1")
                .replaceAll("(?i)\\bAtx\\b", "ATX")
                .replaceAll("(?i)\\bMatx\\b", "mATX")
                .replaceAll("(?i)\\bAm([45])\\b", "AM$1")
                .replaceAll("(?i)\\bWifi\\b", "WiFi")
                .replaceAll("(?i)\\bNvme\\b", "NVMe")
                .replaceAll("(?i)\\bRgb\\b", "RGB")
                .replaceAll("(?i)\\bArgb\\b", "ARGB")
                .replaceAll("(?i)\\bSsd\\b", "SSD")
                .replaceAll("(?i)\\bCpu\\b", "CPU")
                .replaceAll("(?i)\\bGpu\\b", "GPU")
                .replaceAll("(?i)\\bAio\\b", "AIO")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String titleFromSkroutzUrl(String productUrl) {
        if (productUrl == null || productUrl.isBlank()) {
            return "";
        }
        try {
            int slash = productUrl.lastIndexOf('/');
            String slug = slash >= 0 ? productUrl.substring(slash + 1) : productUrl;
            int query = slug.indexOf('?');
            if (query >= 0) {
                slug = slug.substring(0, query);
            }
            if (slug.endsWith(".html")) {
                slug = slug.substring(0, slug.length() - 5);
            }
            return URLDecoder.decode(slug, StandardCharsets.UTF_8)
                    .replace('-', ' ')
                    .replaceAll("\\s+", " ")
                    .trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void loadCatalogs() {
        cpuCatalog.clear();
        gpuCatalog.clear();
        moboCatalog.clear();
        ramCatalog.clear();
        storageCatalog.clear();
        psuCatalog.clear();
        caseCatalog.clear();
        coolerCatalog.clear();

        try {
            cpuCatalog.addAll(catalogRepository.loadCpuCatalog());
        } catch (Exception e) {
            e.printStackTrace();
            if (cpuLabel != null) cpuLabel.setText("CPU: σφάλμα φόρτωσης cpu.csv (δες console).");
        }

        try {
            gpuCatalog.addAll(catalogRepository.loadGpuCatalog());
        } catch (Exception e) {
            e.printStackTrace();
            if (gpuLabel != null) gpuLabel.setText("GPU: σφάλμα φόρτωσης gpu.csv (δες console).");
        }

        try {
            moboCatalog.addAll(catalogRepository.loadMotherboardCatalog());
        } catch (Exception e) {
            e.printStackTrace();
            if (moboLabel != null) moboLabel.setText("Μητρική: σφάλμα φόρτωσης motherboard.csv (δες console).");
        }

        try {
            ramCatalog.addAll(catalogRepository.loadRamCatalog());
        } catch (Exception e) {
            e.printStackTrace();
            if (ramLabel != null) ramLabel.setText("RAM: σφάλμα φόρτωσης ram.csv (δες console).");
        }

        try {
            storageCatalog.addAll(catalogRepository.loadStorageCatalog());
        } catch (Exception e) {
            e.printStackTrace();
            if (storageLabel != null) storageLabel.setText("Αποθήκευση: σφάλμα φόρτωσης storage.csv (δες console).");
        }

        try {
            psuCatalog.addAll(catalogRepository.loadPsuCatalog());
        } catch (Exception e) {
            e.printStackTrace();
            if (psuLabel != null) psuLabel.setText("PSU: σφάλμα φόρτωσης psu.csv (δες console).");
        }

        try {
            caseCatalog.addAll(catalogRepository.loadCaseCatalog());
        } catch (Exception e) {
            e.printStackTrace();
            if (caseLabel != null) caseLabel.setText("Κουτί: σφάλμα φόρτωσης case.csv (δες console).");
        }


        try {
            coolerCatalog.addAll(catalogRepository.loadCoolerCatalog());
        } catch (Exception e) {
            e.printStackTrace();
            if (coolerLabel != null) coolerLabel.setText("Ψύξη CPU: σφάλμα φόρτωσης cooler.csv (δες console).");
        }
    }

    

    @FXML
    private void onPickCpu() {
        if (cpuCatalog.isEmpty()) {
            cpuLabel.setText("CPU: ο κατάλογος είναι άδειος!");
            return;
        }

        CPU defaultCpu = (selectedCpu != null) ? selectedCpu : cpuCatalog.get(0);

        ChoiceDialog<CPU> dialog = new ChoiceDialog<>(defaultCpu, cpuCatalog);
        dialog.setTitle("Επιλογή CPU");
        dialog.setHeaderText("Επίλεξε CPU από τον κατάλογο");
        dialog.setContentText("CPU:");

        Optional<CPU> result = dialog.showAndWait();
        result.ifPresent(cpu -> {
            selectedCpu = cpu;
            suggestionLabel.setText("Πρόταση: –");
            refreshSummary();
        });
    }

    @FXML
    private void onPickGpu() {
        if (gpuCatalog.isEmpty()) {
            gpuLabel.setText("GPU: ο κατάλογος είναι άδειος!");
            return;
        }

        GPU defaultGpu = (selectedGpu != null) ? selectedGpu : gpuCatalog.get(0);

        ChoiceDialog<GPU> dialog = new ChoiceDialog<>(defaultGpu, gpuCatalog);
        dialog.setTitle("Επιλογή GPU");
        dialog.setHeaderText("Επίλεξε GPU από τον κατάλογο");
        dialog.setContentText("GPU:");

        Optional<GPU> result = dialog.showAndWait();
        result.ifPresent(gpu -> {
            selectedGpu = gpu;
            suggestionLabel.setText("Πρόταση: –");
            refreshSummary();
        });
    }

    @FXML
    private void onPickMobo() {
        if (moboCatalog.isEmpty()) {
            moboLabel.setText("Μητρική: ο κατάλογος είναι άδειος!");
            return;
        }

        Motherboard defaultMobo = (selectedMobo != null) ? selectedMobo : moboCatalog.get(0);

        ChoiceDialog<Motherboard> dialog = new ChoiceDialog<>(defaultMobo, moboCatalog);
        dialog.setTitle("Επιλογή Μητρικής");
        dialog.setHeaderText("Επίλεξε μητρική από τον κατάλογο");
        dialog.setContentText("Μητρική:");

        Optional<Motherboard> result = dialog.showAndWait();
        result.ifPresent(mobo -> {
            selectedMobo = mobo;
            suggestionLabel.setText("Πρόταση: –");
            refreshSummary();
        });
    }


    @FXML
    private void onOpenSkroutzDiscovery() {
        try {
            new SkroutzChromiumWindow(livePriceService, this::importMarketplaceResult).show();
        } catch (Exception e) {
            e.printStackTrace();
            suggestionLabel.setText("Σφάλμα: δεν ήταν δυνατό το άνοιγμα του ενσωματωμένου Chromium (δες console).");
        }
    }

    private void importMarketplaceResult(MarketplaceSearchResult result) {
        result = promptStoragePriceIfMissing(result);
        if (result == null) {
            suggestionLabel.setText("Πρόταση: Η εισαγωγή ακυρώθηκε.");
            return;
        }
        Object imported = result.importedPart();
        boolean importedAny = false;

        if (imported instanceof CPU cpu && addCpuIfMissing(cpu)) {
            selectedCpu = cpu;
            seedImportedQuote(cpu.id(), result);
            importedAny = true;
        } else if (imported instanceof GPU gpu && addGpuIfMissing(gpu)) {
            selectedGpu = gpu;
            seedImportedQuote(gpu.id(), result);
            importedAny = true;
        } else if (imported instanceof Motherboard mobo && addMoboIfMissing(mobo)) {
            selectedMobo = mobo;
            seedImportedQuote(mobo.id(), result);
            importedAny = true;
        } else if (imported instanceof PCCase pcCase && addCaseIfMissing(pcCase)) {
            selectedCase = pcCase;
            seedImportedQuote(pcCase.id(), result);
            importedAny = true;
        } else if (imported instanceof Cooler cooler && addCoolerIfMissing(cooler)) {
            selectedCooler = cooler;
            seedImportedQuote(cooler.id(), result);
            importedAny = true;
        } else if (imported instanceof RAM ram && addRamIfMissing(ram)) {
            selectedRam = ram;
            seedImportedQuote(ram.id(), result);
            importedAny = true;
        } else if (imported instanceof Storage storage && addStorageIfMissing(storage)) {
            selectedStorage = storage;
            seedImportedQuote(storage.id(), result);
            importedAny = true;
        } else if (imported instanceof PSU psu && addPsuIfMissing(psu)) {
            selectedPsu = psu;
            seedImportedQuote(psu.id(), result);
            importedAny = true;
        }

        if (importedAny) {
            suggestionLabel.setText("Πρόταση: Το προϊόν εισήχθη στον τρέχοντα κατάλογο και επιλέχθηκε.");
            refreshSummary();
        } else {
            suggestionLabel.setText("Πρόταση: Το προϊόν υπάρχει ήδη στον τρέχοντα κατάλογο.");
        }
    }

    private MarketplaceSearchResult promptStoragePriceIfMissing(MarketplaceSearchResult result) {
        if (result == null || !(result.importedPart() instanceof Storage storage)) {
            return result;
        }
        if (storage.priceEur() > 0.0 || result.priceEur() > 0.0) {
            return result;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Χειροκίνητη τιμή αποθηκευτικού χώρου");
        dialog.setHeaderText("Δεν εντοπίστηκε αυτόματα η τιμή του SSD/NVMe από τη σελίδα Skroutz.");
        dialog.setContentText("Πληκτρολόγησε την τιμή σε ευρώ, π.χ. 134,89:");

        Optional<String> manualValue = dialog.showAndWait();
        if (manualValue.isEmpty()) {
            return null;
        }

        double manualPrice = parseManualEuroPrice(manualValue.get());
        if (manualPrice <= 0.0) {
            suggestionLabel.setText("Πρόταση: Η τιμή αποθηκευτικού χώρου δεν ήταν έγκυρη, οπότε η εισαγωγή ακυρώθηκε.");
            return null;
        }

        Storage correctedStorage = new Storage(
                storage.id(),
                storage.brand(),
                storage.model(),
                storage.type(),
                storage.capacityGb(),
                storage.perfScore(),
                manualPrice,
                storage.manufacturerPartNumber(),
                storage.skroutzQuery(),
                storage.skroutzUrl()
        );

        PriceQuote manualQuote = new PriceQuote(
                correctedStorage.id(),
                manualPrice,
                "Skroutz",
                result.productUrl(),
                result.title(),
                null,
                Instant.now(),
                true,
                PriceConfidence.EXACT,
                "Manual storage price entered after Skroutz import fallback"
        );

        return new MarketplaceSearchResult(
                result.category(),
                result.title(),
                manualPrice,
                result.productUrl(),
                PriceConfidence.EXACT,
                "Manual storage price entered after Skroutz import fallback",
                correctedStorage.type() + ", " + correctedStorage.capacityGb() + "GB, score " + correctedStorage.perfScore(),
                correctedStorage,
                manualQuote
        );
    }

    private double parseManualEuroPrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1.0;
        }

        String normalized = raw.trim()
                .replace("€", "")
                .replace(" ", "");

        int lastComma = normalized.lastIndexOf(',');
        int lastDot = normalized.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                normalized = normalized.replace(".", "").replace(',', '.');
            } else {
                normalized = normalized.replace(",", "");
            }
        } else if (lastComma >= 0) {
            normalized = normalized.replace(".", "").replace(',', '.');
        } else if (lastDot >= 0 && normalized.matches("\\d{1,3}(\\.\\d{3})+")) {
            normalized = normalized.replace(".", "");
        }

        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private void seedImportedQuote(String partId, MarketplaceSearchResult result) {
        PriceQuote seed = result.seedQuote();
        if (seed != null) {
            livePriceService.putQuote(new PriceQuote(
                    partId,
                    seed.priceEur(),
                    seed.sourceName(),
                    seed.productUrl(),
                    seed.matchedTitle(),
                    seed.storeCount(),
                    seed.fetchedAt(),
                    seed.live(),
                    seed.confidence(),
                    seed.matchReason()
            ));
        }
    }

    private boolean addCpuIfMissing(CPU cpu) {
        for (CPU existing : cpuCatalog) {
            if (existing.id().equals(cpu.id())) return false;
        }
        cpuCatalog.add(cpu);
        return true;
    }

    private boolean addGpuIfMissing(GPU gpu) {
        for (GPU existing : gpuCatalog) {
            if (existing.id().equals(gpu.id())) return false;
        }
        gpuCatalog.add(gpu);
        return true;
    }

    private boolean addMoboIfMissing(Motherboard mobo) {
        for (Motherboard existing : moboCatalog) {
            if (existing.id().equals(mobo.id())) return false;
        }
        moboCatalog.add(mobo);
        return true;
    }

    private boolean addRamIfMissing(RAM ram) {
        for (RAM existing : ramCatalog) {
            if (existing.id().equals(ram.id())) return false;
        }
        ramCatalog.add(ram);
        return true;
    }

    private boolean addStorageIfMissing(Storage storage) {
        for (int i = 0; i < storageCatalog.size(); i++) {
            Storage existing = storageCatalog.get(i);
            if (existing.id().equals(storage.id())) {
                if (storage.priceEur() > 0.0 && existing.priceEur() <= 0.0) {
                    storageCatalog.set(i, storage);
                    return true;
                }
                return false;
            }
        }
        storageCatalog.add(storage);
        return true;
    }

    private boolean addPsuIfMissing(PSU psu) {
        for (PSU existing : psuCatalog) {
            if (existing.id().equals(psu.id())) return false;
        }
        psuCatalog.add(psu);
        return true;
    }

    private boolean addCaseIfMissing(PCCase pcCase) {
        for (PCCase existing : caseCatalog) {
            if (existing.id().equals(pcCase.id())) return false;
        }
        caseCatalog.add(pcCase);
        return true;
    }

    private boolean addCoolerIfMissing(Cooler cooler) {
        for (Cooler existing : coolerCatalog) {
            if (existing.id().equals(cooler.id())) return false;
        }
        coolerCatalog.add(cooler);
        return true;
    }

    @FXML
    private void onOpenTutorialLibrary() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("TutorialLibraryView.fxml"));
            Parent root = loader.load();

            TutorialLibraryController controller = loader.getController();
            String profileName = gamingProfile ? "Gaming" : "Creator";
            controller.setProfile(profileName);

            Stage stage = new Stage();
            stage.setTitle("Βιβλιοθήκη Εκπαιδευτικών Οδηγών");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            if (suggestionLabel != null) {
                suggestionLabel.setText("Σφάλμα: δεν ήταν δυνατό το άνοιγμα της βιβλιοθήκης tutorial (δες console).");
            }
        }
    }


    @FXML
    private void onPickRam() {
        if (ramCatalog.isEmpty()) {
            ramLabel.setText("RAM: ο κατάλογος είναι άδειος!");
            return;
        }

        RAM defaultRam = (selectedRam != null) ? selectedRam : ramCatalog.get(0);

        ChoiceDialog<RAM> dialog = new ChoiceDialog<>(defaultRam, ramCatalog);
        dialog.setTitle("Επιλογή RAM");
        dialog.setHeaderText("Επίλεξε kit RAM από τον κατάλογο");
        dialog.setContentText("RAM:");

        Optional<RAM> result = dialog.showAndWait();
        result.ifPresent(ram -> {
            selectedRam = ram;
            suggestionLabel.setText("Πρόταση: –");
            refreshSummary();
        });
    }

    @FXML
    private void onOpenOptimizationPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("OptimizationView.fxml"));
            Parent root = loader.load();

            OptimizationController controller = loader.getController();

            String profileName = gamingProfile ? "Gaming" : "Creator";
            String psuMode = psuSafeMode ? "Safe PSU headroom" : "Aggressive PSU headroom";

            controller.setProfileText("Profile: " + profileName + " (" + psuMode + ")");
            controller.setHardwareSummary(buildCurrentSummaryText());
            controller.setWindowsTips(buildWindowsOptimizationTips());
            controller.setBiosTips(buildBiosOptimizationTips());

            Stage stage = new Stage();
            stage.setTitle("Βελτιστοποίηση & Ρυθμίσεις");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            suggestionLabel.setText("Σφάλμα: δεν ήταν δυνατό το άνοιγμα του πίνακα βελτιστοποίησης (δες console).");
        }
    }


    @FXML
    private void onOpenBuildGuide() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("BuildGuide.fxml"));
            Parent root = loader.load();

            BuildGuideController controller = loader.getController();


            String profileName = gamingProfile ? "Gaming" : "Creator";
            controller.setProfile(profileName);


            controller.setBuildSummary(buildCurrentSummaryText());

            Stage stage = new Stage();
            stage.setTitle("Οδηγός Συναρμολόγησης");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void onPickStorage() {
        if (storageCatalog.isEmpty()) {
            storageLabel.setText("Αποθήκευση: ο κατάλογος είναι άδειος!");
            return;
        }

        Storage defaultStorage = (selectedStorage != null) ? selectedStorage : storageCatalog.get(0);

        ChoiceDialog<Storage> dialog = new ChoiceDialog<>(defaultStorage, storageCatalog);
        dialog.setTitle("Επιλογή Αποθηκευτικού Χώρου");
        dialog.setHeaderText("Επίλεξε δίσκο από τον κατάλογο");
        dialog.setContentText("Αποθήκευση:");

        Optional<Storage> result = dialog.showAndWait();
        result.ifPresent(storage -> {
            selectedStorage = storage;
            suggestionLabel.setText("Πρόταση: –");
            refreshSummary();
        });
    }

    @FXML
    private void onExplainPerformance() {
        String profileName = gamingProfile ? "Gaming" : "Creator";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Σκορ Απόδοσης");
        alert.setHeaderText("Τι σημαίνει το σκορ απόδοσης;");
        alert.setContentText(
                "• Συνδυάζει την απόδοση CPU και GPU από τον κατάλογο,\n" +
                        "  μαζί με μικρά bonus από RAM και αποθήκευση.\n" +
                        "• Στο προφίλ " + profileName + " αλλάζουν τα βάρη υπολογισμού\n" +
                        "  (Gaming = περισσότερη έμφαση στη GPU, Creator = CPU/RAM).\n\n" +
                        "Το αποτέλεσμα κανονικοποιείται περίπου σε κλίμακα 0–100,\n" +
                        "ώστε να συγκρίνεις εύκολα διαφορετικές συνθέσεις."
        );
        alert.showAndWait();
    }

    @FXML
    private void onExplainValue() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Σκορ Αξίας");
        alert.setHeaderText("Τι σημαίνει το σκορ αξίας;");
        alert.setContentText(
                "• Το σκορ αξίας ≈ σκορ απόδοσης / συνολική τιμή.\n" +
                        "• Υψηλότερη τιμή σημαίνει περισσότερη απόδοση ανά ευρώ.\n" +
                        "• Χρησιμοποιείται κυρίως στην επιλογή 'Καλύτερο Value'."
        );
        alert.showAndWait();
    }

    @FXML
    private void onExplainPower() {
        String psuModeText = psuSafeMode
                ? "Ασφαλές mode (προτιμά μεγαλύτερο περιθώριο PSU)"
                : "Οριακό mode (επιτρέπει μικρότερο περιθώριο όταν χρειάζεται)";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Εκτιμώμενη ισχύς & PSU");
        alert.setHeaderText("Τι σημαίνει η εκτιμώμενη ισχύς;");
        alert.setContentText(
                "• Η εκτιμώμενη ισχύς υπολογίζεται από CPU, GPU και τα υπόλοιπα εξαρτήματα.\n" +
                        "• Η εφαρμογή τη συγκρίνει με τα Watt του PSU για να βρει το περιθώριο ασφαλείας.\n" +
                        "• Στο " + psuModeText + ", το περιθώριο αυτό επηρεάζει τις προτεινόμενες συνθέσεις.\n\n" +
                        "Αν το περιθώριο είναι πολύ χαμηλό, το PSU μπορεί να δουλεύει πιο ζεστά ή πιο θορυβώδη."
        );
        alert.showAndWait();
    }

    @FXML
    private void onExplainBudget() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Budget");
        alert.setHeaderText("Πώς χρησιμοποιείται το budget;");
        alert.setContentText(
                "• Αν βάλεις budget, οι προτάσεις εξετάζουν μόνο συνθέσεις\n" +
                        "  με συνολική τιμή ίση ή χαμηλότερη από αυτό.\n" +
                        "• Αν δεν υπάρχει συμβατή σύνθεση στο budget, η εφαρμογή\n" +
                        "  εμφανίζει ότι δεν βρέθηκε έγκυρη επιλογή.\n\n" +
                        "Άφησέ το κενό αν δεν θέλεις όριο budget."
        );
        alert.showAndWait();
    }

    @FXML
    private void onPickCase() {
        if (caseCatalog.isEmpty()) {
            caseLabel.setText("Κουτί: ο κατάλογος είναι άδειος!");
            return;
        }

        PCCase defaultCase = (selectedCase != null) ? selectedCase : caseCatalog.get(0);

        ChoiceDialog<PCCase> dialog = new ChoiceDialog<>(defaultCase, caseCatalog);
        dialog.setTitle("Επιλογή Κουτιού");
        dialog.setHeaderText("Επίλεξε κουτί από τον κατάλογο");
        dialog.setContentText("Κουτί:");

        Optional<PCCase> result = dialog.showAndWait();
        result.ifPresent(pcCase -> {
            selectedCase = pcCase;
            suggestionLabel.setText("Πρόταση: –");
            refreshSummary();
        });
    }

    @FXML
    private void onPickCooler() {
        if (coolerCatalog.isEmpty()) {
            coolerLabel.setText("Ψύξη CPU: ο κατάλογος είναι άδειος!");
            return;
        }

        Cooler defaultCooler = (selectedCooler != null) ? selectedCooler : coolerCatalog.get(0);

        ChoiceDialog<Cooler> dialog = new ChoiceDialog<>(defaultCooler, coolerCatalog);
        dialog.setTitle("Επιλογή Ψύξης CPU");
        dialog.setHeaderText("Επίλεξε ψύξη CPU από τον κατάλογο");
        dialog.setContentText("Ψύξη CPU:");

        Optional<Cooler> result = dialog.showAndWait();
        result.ifPresent(cooler -> {
            selectedCooler = cooler;
            suggestionLabel.setText("Πρόταση: –");
            refreshSummary();
        });
    }


    @FXML
    private void onPickPsu() {
        if (psuCatalog.isEmpty()) {
            psuLabel.setText("PSU: ο κατάλογος είναι άδειος!");
            return;
        }

        PSU defaultPsu = (selectedPsu != null) ? selectedPsu : psuCatalog.get(0);

        ChoiceDialog<PSU> dialog = new ChoiceDialog<>(defaultPsu, psuCatalog);
        dialog.setTitle("Επιλογή PSU");
        dialog.setHeaderText("Επίλεξε PSU από τον κατάλογο");
        dialog.setContentText("PSU:");

        Optional<PSU> result = dialog.showAndWait();
        result.ifPresent(psu -> {
            selectedPsu = psu;
            suggestionLabel.setText("Πρόταση: –");
            refreshSummary();
        });
    }

    

    @FXML
    private void onSuggestMaxPerformance() {
        BuildOption best = findBestBuildBy("perf");
        if (best == null) {
            suggestionLabel.setText("Δεν βρέθηκε έγκυρη σύνθεση με το τρέχον budget και τις ρυθμίσεις PSU.");
            return;
        }
        applyBuild(best);
        String mode = psuSafeMode ? "Safe" : "Aggressive";
        suggestionLabel.setText(String.format(
                "Πρόταση (%s): επιλέχθηκε σύνθεση μέγιστης απόδοσης (perf %.1f / 100, €%.2f).",
                mode, best.perf, best.totalPrice
        ));
    }

    @FXML
    private void onSuggestLowestPrice() {
        BuildOption best = findBestBuildBy("price");
        if (best == null) {
            suggestionLabel.setText("Δεν βρέθηκε έγκυρη σύνθεση με το τρέχον budget και τις ρυθμίσεις PSU.");
            return;
        }
        applyBuild(best);
        String mode = psuSafeMode ? "Safe" : "Aggressive";
        suggestionLabel.setText(String.format(
                "Πρόταση (%s): επιλέχθηκε σύνθεση χαμηλότερης τιμής (perf %.1f / 100, €%.2f).",
                mode, best.perf, best.totalPrice
        ));
    }

    @FXML
    private void onSuggestBestValue() {
        BuildOption best = findBestBuildBy("value");
        if (best == null) {
            suggestionLabel.setText("Δεν βρέθηκε έγκυρη σύνθεση με το τρέχον budget και τις ρυθμίσεις PSU.");
            return;
        }
        applyBuild(best);
        String mode = psuSafeMode ? "Safe" : "Aggressive";
        suggestionLabel.setText(String.format(
                "Πρόταση (%s): επιλέχθηκε σύνθεση καλύτερου value (value %.2f, perf %.1f / 100, €%.2f).",
                mode, best.valueScore, best.perf, best.totalPrice
        ));
    }

    private void applyBuild(BuildOption b) {
        selectedCpu = b.cpu;
        selectedGpu = b.gpu;
        selectedMobo = b.mobo;
        selectedCase = b.pcCase;
        selectedCooler = b.cooler;
        selectedRam = b.ram;
        selectedStorage = b.storage;
        selectedPsu = b.psu;
        livePriceService.clearQuotes();
        refreshSummary();
    }

    




    private double getBudgetLimit() {
        if (budgetField == null) return 0.0;

        String text = budgetField.getText();
        if (text == null) return 0.0;

        text = text.trim();
        if (text.isEmpty()) return 0.0;

        try {
            double value = Double.parseDouble(text);
            return value > 0 ? value : 0.0;
        } catch (NumberFormatException e) {

            return 0.0;
        }
    }

    

    private BuildOption findBestBuildBy(String criterion) {
        List<BuildOption> options = generateValidBuilds();
        if (options.isEmpty()) return null;


        List<BuildOption> working;

        if (psuSafeMode) {
            List<BuildOption> safe = new ArrayList<>();
            List<BuildOption> borderline = new ArrayList<>();

            for (BuildOption b : options) {
                if (b.headroom >= MIN_SAFE_HEADROOM_W) {
                    safe.add(b);
                } else {
                    borderline.add(b);
                }
            }


            working = !safe.isEmpty() ? safe : borderline;
        } else {
            working = options;
        }

        if (working.isEmpty()) return null;


        double budget = getBudgetLimit();
        if (budget > 0) {
            List<BuildOption> underBudget = new ArrayList<>();
            for (BuildOption b : working) {
                if (b.totalPrice <= budget) {
                    underBudget.add(b);
                }
            }

            working = underBudget;
            if (working.isEmpty()) {
                return null;
            }
        }


        BuildOption best = null;

        switch (criterion) {
            case "perf" -> {
                double bestPerf = -1.0;
                for (BuildOption b : working) {
                    if (b.perf > bestPerf) {
                        bestPerf = b.perf;
                        best = b;
                    }
                }
            }
            case "price" -> {
                double bestPrice = Double.MAX_VALUE;
                for (BuildOption b : working) {
                    if (b.totalPrice < bestPrice) {
                        bestPrice = b.totalPrice;
                        best = b;
                    }
                }
            }
            case "value" -> {

                double maxValue = -1.0;
                for (BuildOption b : working) {
                    if (b.valueScore > maxValue) {
                        maxValue = b.valueScore;
                    }
                }

                double threshold = maxValue * 0.99;
                double bestPrice = Double.MAX_VALUE;
                for (BuildOption b : working) {
                    if (b.valueScore >= threshold && b.totalPrice < bestPrice) {
                        bestPrice = b.totalPrice;
                        best = b;
                    }
                }
            }
            default -> {

            }
        }

        return best;
    }

    @FXML
    private void onCopyBuildSummary() {
        String summary = "PCBuilder+ Σύνοψη Σύνθεσης\n\n" + buildCurrentSummaryText();

        ClipboardContent content = new ClipboardContent();
        content.putString(summary);
        Clipboard.getSystemClipboard().setContent(content);

        if (suggestionLabel != null) {
            suggestionLabel.setText("Η σύνοψη αντιγράφηκε στο πρόχειρο.");
        }
    }

    @FXML
    private void onSaveBuild() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Αποθήκευση σύνθεσης PCBuilder+");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PCBuilder build (*.pcbuild)", "*.pcbuild")
        );

        File file = chooser.showSaveDialog(getWindow());
        if (file == null) return;

        Properties props = new Properties();

        props.setProperty("cpuId",    selectedCpu != null ? selectedCpu.id() : "");
        props.setProperty("gpuId",    selectedGpu != null ? selectedGpu.id() : "");
        props.setProperty("moboId",   selectedMobo != null ? selectedMobo.id() : "");
        props.setProperty("ramId",    selectedRam != null ? selectedRam.id() : "");
        props.setProperty("storageId",selectedStorage != null ? selectedStorage.id() : "");
        props.setProperty("psuId",    selectedPsu != null ? selectedPsu.id() : "");
        props.setProperty("caseId", selectedCase != null ? selectedCase.id() : "");
        props.setProperty("coolerId", selectedCooler != null ? selectedCooler.id() : "");

        props.setProperty("profile",  gamingProfile ? "gaming" : "creator");
        props.setProperty("psuSafe",  Boolean.toString(psuSafeMode));
        props.setProperty("budget",   budgetField != null ? budgetField.getText().trim() : "");

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "PCBuilder+ build file");
            suggestionLabel.setText("Η σύνθεση αποθηκεύτηκε στο " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
            suggestionLabel.setText("Σφάλμα: δεν ήταν δυνατή η αποθήκευση (δες console).");
        }
    }

    @FXML
    private void onLoadBuild() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Φόρτωση σύνθεσης PCBuilder+");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PCBuilder build (*.pcbuild)", "*.pcbuild")
        );

        File file = chooser.showOpenDialog(getWindow());
        if (file == null) return;

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            suggestionLabel.setText("Σφάλμα: δεν ήταν δυνατή η φόρτωση (δες console).");
            return;
        }


        selectedCpu      = findCpuById(props.getProperty("cpuId", ""));
        selectedGpu      = findGpuById(props.getProperty("gpuId", ""));
        selectedMobo     = findMoboById(props.getProperty("moboId", ""));
        selectedRam      = findRamById(props.getProperty("ramId", ""));
        selectedStorage  = findStorageById(props.getProperty("storageId", ""));
        selectedPsu      = findPsuById(props.getProperty("psuId", ""));
        selectedCase     = findCaseById(props.getProperty("caseId", ""));
        selectedCooler   = findCoolerById(props.getProperty("coolerId", ""));


        String profile = props.getProperty("profile", "gaming");
        if ("creator".equalsIgnoreCase(profile)) {
            gamingProfile = false;
            if (creatorRadio != null) creatorRadio.setSelected(true);
            if (gamingRadio != null) gamingRadio.setSelected(false);
        } else {
            gamingProfile = true;
            if (gamingRadio != null) gamingRadio.setSelected(true);
            if (creatorRadio != null) creatorRadio.setSelected(false);
        }


        psuSafeMode = Boolean.parseBoolean(props.getProperty("psuSafe", "true"));
        if (psuSafeMode) {
            if (safePsuRadio != null) safePsuRadio.setSelected(true);
            if (aggressivePsuRadio != null) aggressivePsuRadio.setSelected(false);
        } else {
            if (safePsuRadio != null) safePsuRadio.setSelected(false);
            if (aggressivePsuRadio != null) aggressivePsuRadio.setSelected(true);
        }


        String budgetText = props.getProperty("budget", "");
        if (budgetField != null) {
            budgetField.setText(budgetText);
        }

        refreshSummary();
        suggestionLabel.setText("Η σύνθεση φορτώθηκε από " + file.getName());
    }

    @FXML
    private void onExportBuildReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Εξαγωγή αναφοράς σύνθεσης");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text file (*.txt)", "*.txt")
        );

        File file = chooser.showSaveDialog(getWindow());
        if (file == null) return;

        StringBuilder report = new StringBuilder();

        report.append("PCBuilder+ Build Report\n");
        report.append("========================\n\n");

        report.append("Profile: ").append(gamingProfile ? "Gaming" : "Creator").append("\n");
        report.append("PSU mode: ").append(psuSafeMode ? "Safe" : "Aggressive").append("\n");
        if (budgetField != null && !budgetField.getText().trim().isEmpty()) {
            report.append("Budget: €").append(budgetField.getText().trim()).append("\n");
        } else {
            report.append("Budget: (no limit)\n");
        }
        report.append("\n");

        report.append(buildCurrentSummaryText());

        try {
            Files.writeString(file.toPath(), report.toString(), StandardCharsets.UTF_8);
            suggestionLabel.setText("Η αναφορά εξήχθη στο " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
            suggestionLabel.setText("Σφάλμα: δεν ήταν δυνατή η εξαγωγή αναφοράς (δες console).");
        }
    }

    @FXML
    private void onResetBuild() {

        selectedCpu = null;
        selectedGpu = null;
        selectedMobo = null;
        selectedRam = null;
        selectedStorage = null;
        selectedPsu = null;
        selectedCase = null;
        selectedCooler = null;
        livePriceService.clearQuotes();


        gamingProfile = true;
        if (gamingRadio != null) {
            gamingRadio.setSelected(true);
        }
        if (creatorRadio != null) {
            creatorRadio.setSelected(false);
        }


        psuSafeMode = true;
        if (safePsuRadio != null) {
            safePsuRadio.setSelected(true);
        }
        if (aggressivePsuRadio != null) {
            aggressivePsuRadio.setSelected(false);
        }


        if (budgetField != null) {
            budgetField.clear();
        }


        refreshSummary();

        if (suggestionLabel != null) {
            suggestionLabel.setText("Η σύνθεση επανήλθε στις αρχικές επιλογές.");
        }
    }


    @FXML
    private void onLoadPresetBuild() {

        List<String> options = List.of(
                "Entry Gaming (~€1100)",
                "1440p High Gaming (~€1700)",
                "4K Creator (High-End, ~€3200+)"
        );

        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("Έτοιμες Συνθέσεις");
        dialog.setHeaderText("Επίλεξε προφίλ έτοιμης σύνθεσης");
        dialog.setContentText("Έτοιμη σύνθεση:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String choice = result.get();


        String criterion;

        if (choice.startsWith("Entry Gaming")) {

            gamingProfile = true;
            if (gamingRadio != null) gamingRadio.setSelected(true);
            if (creatorRadio != null) creatorRadio.setSelected(false);

            psuSafeMode = true;
            if (safePsuRadio != null) safePsuRadio.setSelected(true);
            if (aggressivePsuRadio != null) aggressivePsuRadio.setSelected(false);

            if (budgetField != null) budgetField.setText("1100");

            criterion = "value";

        } else if (choice.startsWith("1440p High Gaming")) {

            gamingProfile = true;
            if (gamingRadio != null) gamingRadio.setSelected(true);
            if (creatorRadio != null) creatorRadio.setSelected(false);

            psuSafeMode = true;
            if (safePsuRadio != null) safePsuRadio.setSelected(true);
            if (aggressivePsuRadio != null) aggressivePsuRadio.setSelected(false);

            if (budgetField != null) budgetField.setText("1700");

            criterion = "perf";

        } else {

            gamingProfile = false;
            if (gamingRadio != null) gamingRadio.setSelected(false);
            if (creatorRadio != null) creatorRadio.setSelected(true);

            psuSafeMode = true;
            if (safePsuRadio != null) safePsuRadio.setSelected(true);
            if (aggressivePsuRadio != null) aggressivePsuRadio.setSelected(false);


            if (budgetField != null) budgetField.setText("3200");

            criterion = "perf";
        }


        BuildOption best = findBestBuildBy(criterion);
        if (best == null) {
            suggestionLabel.setText("Preset: δεν βρέθηκε έγκυρη σύνθεση για αυτό το preset (έλεγξε τους καταλόγους).");
            return;
        }

        applyBuild(best);

        String mode = psuSafeMode ? "ασφαλές" : "οριακό";

        suggestionLabel.setText(String.format(
                "Επιλέχθηκε έτοιμη σύνθεση: %s με %s PSU. Αξία: %.2f, απόδοση: %.1f / 100, κόστος: €%.2f.",
                choice, mode, best.valueScore, best.perf, best.totalPrice
        ));
    }

    private String buildWindowsOptimizationTips() {
        StringBuilder sb = new StringBuilder();

        if (gamingProfile) {
            sb.append("""
                    Ρυθμίσεις Windows για Gaming:
                    - Ενεργοποίησε το Game Mode από Ρυθμίσεις Windows → Gaming.
                    - Απενεργοποίησε τα Xbox Game Bar overlays αν δεν τα χρησιμοποιείς.
                    - Στις ρυθμίσεις γραφικών, όρισε τα βασικά παιχνίδια σε «Υψηλή απόδοση».
                    - Ρύθμισε την οθόνη στο μέγιστο refresh rate από Οθόνη → Σύνθετες ρυθμίσεις οθόνης.
                    - Κλείσε βαριές εφαρμογές στο background, όπως browser tabs ή launchers που δεν χρειάζεσαι.
                    - Κράτησε ενημερωμένους τους GPU και chipset drivers από τις επίσημες σελίδες των κατασκευαστών.
                    
                    Πλάνο ενέργειας:
                    - Χρησιμοποίησε Balanced ή προσαρμοσμένο power plan με ελάχιστη CPU 5–10% και μέγιστη 100%.
                    - Σε desktop σύστημα μπορείς να δοκιμάσεις και «Υψηλή απόδοση» κατά τη διάρκεια gaming sessions.
                    """);
        } else {
            sb.append("""
                    Ρυθμίσεις Windows για Creator / Editing / 3D:
                    - Εγκατέστησε πρώτα chipset, GPU και storage drivers από επίσημες πηγές.
                    - Χρησιμοποίησε Balanced ή High performance power plan κατά τη διάρκεια βαριών εργασιών.
                    - Τοποθέτησε project files και cache/scratch folders στον πιο γρήγορο δίσκο, ιδανικά NVMe.
                    - Κράτησε τουλάχιστον 15–20% ελεύθερο χώρο στους δίσκους εργασίας για αποφυγή επιβραδύνσεων.
                    - Απενεργοποίησε overlays που δεν χρειάζεσαι, όπως game overlays ή screen recorders.
                    - Ρύθμισε τις κύριες εφαρμογές σου ώστε να χρησιμοποιούν GPU acceleration όπου υποστηρίζεται.
                    
                    Πρακτικές ρυθμίσεις:
                    - Για πολύωρη εργασία, μπορείς να χρησιμοποιήσεις Night light ή σωστό color management.
                    - Βεβαιώσου ότι η οθόνη λειτουργεί στη native ανάλυση και στο σωστό color profile.
                    """);
        }

        sb.append("Γενικές συμβουλές:");
        sb.append("""
                - Κάνε τακτικά Windows Update, αλλά απόφυγε τυχαία driver updater προγράμματα.
                - Χρησιμοποίησε αξιόπιστο antivirus. Το Windows Defender είναι αρκετό για τους περισσότερους χρήστες.
                - Απόφυγε registry cleaners ή optimizer tools που υπόσχονται τεράστια αύξηση FPS.
                """);

        return sb.toString();
    }

    private String buildBiosOptimizationTips() {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                Βασικοί έλεγχοι BIOS / UEFI:
                - Ενημέρωσε το BIOS σε σταθερή έκδοση που προτείνεται από τον κατασκευαστή της μητρικής.
                - Ενεργοποίησε XMP (Intel) ή EXPO (AMD), ώστε η RAM να λειτουργεί στην ονομαστική της ταχύτητα.
                - Έλεγξε ότι αναγνωρίζονται σωστά CPU, συνολική RAM και όλοι οι δίσκοι.
                - Όρισε σωστό boot order, με πρώτο τον system SSD αφού εγκατασταθεί το λειτουργικό.
                """);

        if (gamingProfile) {
            sb.append("""
                    
                    Επιλογές για Gaming:
                    - Ενεργοποίησε Resizable BAR / Smart Access Memory αν υποστηρίζεται από GPU και πλατφόρμα.
                    - Άφησε ενεργά τα CPU turbo/boost features, που συνήθως είναι ενεργά από προεπιλογή.
                    - Απόφυγε ακραίο overclock αν δεν παρακολουθείς θερμοκρασίες και σταθερότητα.
                    - Ρύθμισε τα case fans για ισορροπία ανάμεσα σε θόρυβο και θερμοκρασίες CPU/GPU.
                    """);
        } else {
            sb.append("""
                    
                    Επιλογές για Creator χρήση:
                    - Η σταθερότητα είναι πιο σημαντική από μικρά κέρδη απόδοσης, οπότε απόφυγε ασταθές overclock.
                    - Βεβαιώσου ότι η RAM λειτουργεί στην ονομαστική ταχύτητα και κάνε stability test αν αλλάξεις ρυθμίσεις.
                    - Αν χρησιμοποιείς virtual machines ή Docker, ενεργοποίησε virtualization (Intel VT-x / AMD-V).
                    - Ρύθμισε fan curves ώστε ο θόρυβος να παραμένει αποδεκτός σε πολύωρα renders.
                    """);
        }

        sb.append("""
                    
                Ασφάλεια PSU / ισχύος:
                - Άφησε ενεργές τις αυτόματες προστασίες της μητρικής για CPU και VRM.
                - Αν αλλάξεις power limits, παρακολούθησε προσεκτικά θερμοκρασίες CPU και GPU.
                """);

        return sb.toString();
    }


    private CPU findCpuById(String id) {
        if (id == null || id.isBlank()) return null;
        for (CPU c : cpuCatalog) {
            if (id.equals(c.id())) return c;
        }
        return null;
    }

    private GPU findGpuById(String id) {
        if (id == null || id.isBlank()) return null;
        for (GPU g : gpuCatalog) {
            if (id.equals(g.id())) return g;
        }
        return null;
    }

    private Motherboard findMoboById(String id) {
        if (id == null || id.isBlank()) return null;
        for (Motherboard m : moboCatalog) {
            if (id.equals(m.id())) return m;
        }
        return null;
    }

    private RAM findRamById(String id) {
        if (id == null || id.isBlank()) return null;
        for (RAM r : ramCatalog) {
            if (id.equals(r.id())) return r;
        }
        return null;
    }

    private Storage findStorageById(String id) {
        if (id == null || id.isBlank()) return null;
        for (Storage s : storageCatalog) {
            if (id.equals(s.id())) return s;
        }
        return null;
    }

    private PSU findPsuById(String id) {
        if (id == null || id.isBlank()) return null;
        for (PSU p : psuCatalog) {
            if (id.equals(p.id())) return p;
        }
        return null;
    }

    private PCCase findCaseById(String id) {
        if (id == null || id.isBlank()) return null;
        for (PCCase c : caseCatalog) {
            if (id.equals(c.id())) return c;
        }
        return null;
    }

    private Cooler findCoolerById(String id) {
        if (id == null || id.isBlank()) return null;
        for (Cooler c : coolerCatalog) {
            if (id.equals(c.id())) return c;
        }
        return null;
    }


    private Window getWindow() {
        if (cpuLabel != null && cpuLabel.getScene() != null) {
            return cpuLabel.getScene().getWindow();
        }
        return null;
    }




    private List<BuildOption> generateValidBuilds() {
        List<BuildOption> list = new ArrayList<>();
        if (cpuCatalog.isEmpty() || gpuCatalog.isEmpty() || moboCatalog.isEmpty()
                || ramCatalog.isEmpty() || storageCatalog.isEmpty()
                || psuCatalog.isEmpty() || caseCatalog.isEmpty() || coolerCatalog.isEmpty()) {
            return list;
        }

        for (CPU cpu : cpuCatalog) {
            for (GPU gpu : gpuCatalog) {
                for (Motherboard mobo : moboCatalog) {
                    if (!cpu.socket().equalsIgnoreCase(mobo.socket())) {
                        continue;
                    }

                    for (RAM ram : ramCatalog) {
                        if (!isRamCompatibleWithMotherboard(ram, mobo)) {
                            continue;
                        }

                        for (Storage storage : storageCatalog) {
                            for (PCCase pcCase : caseCatalog) {
                                if (!pcCase.supportsMotherboardFormFactor(mobo.formFactor())) {
                                    continue;
                                }
                                if (gpu.lengthMm() > pcCase.maxGpuLengthMm()) {
                                    continue;
                                }

                                for (Cooler cooler : coolerCatalog) {
                                    if (!isCoolerCompatibleWithCase(cooler, pcCase)) {
                                        continue;
                                    }

                                    for (PSU psu : psuCatalog) {
                                        int estimatedPower = powerEstimator.estimatePower(cpu, gpu, mobo, ram, storage);
                                        int headroom = psu.wattageW() - estimatedPower;

                                        if (headroom < 0) {
                                            continue;
                                        }

                                        double perf = Math.min(scoringService.computeOverallPerf(cpu, gpu, ram, storage, gamingProfile), 100.0);
                                        double price = resolvePrice(cpu.id(), cpu.priceEur())
                                                + resolvePrice(gpu.id(), gpu.priceEur())
                                                + resolvePrice(mobo.id(), mobo.priceEur())
                                                + resolvePrice(pcCase.id(), pcCase.priceEur())
                                                + resolvePrice(cooler.id(), cooler.priceEur())
                                                + resolvePrice(ram.id(), ram.priceEur())
                                                + resolvePrice(storage.id(), storage.priceEur())
                                                + resolvePrice(psu.id(), psu.priceEur());

                                        double value = (price > 0 && perf > 0) ? (perf / price) * 100.0 : 0.0;

                                        BuildOption opt = new BuildOption();
                                        opt.cpu = cpu;
                                        opt.gpu = gpu;
                                        opt.mobo = mobo;
                                        opt.pcCase = pcCase;
                                        opt.cooler = cooler;
                                        opt.ram = ram;
                                        opt.storage = storage;
                                        opt.psu = psu;
                                        opt.perf = perf;
                                        opt.totalPrice = price;
                                        opt.valueScore = value;
                                        opt.estimatedPower = estimatedPower;
                                        opt.headroom = headroom;

                                        list.add(opt);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    private boolean isCoolerCompatibleWithCase(Cooler cooler, PCCase pcCase) {
        if (cooler == null || pcCase == null) return false;
        if (cooler.isAirCooler()) {
            return cooler.heightMm() <= pcCase.maxAirCoolerHeightMm();
        }
        if (cooler.isAioCooler()) {
            return switch (cooler.radiatorSizeMm()) {
                case 240 -> pcCase.supports240Aio();
                case 280 -> pcCase.supports280Aio();
                case 360 -> pcCase.supports360Aio();
                default -> false;
            };
        }
        return false;
    }

    

    private void refreshSummary() {

        if (selectedCpu == null) {
            cpuLabel.setText("Επεξεργαστής: (Δεν έχει γίνει επιλογή)");
            if (cpuCaseButton != null) cpuCaseButton.setText("CPU");
        } else {
            cpuLabel.setText(
                    "Επεξεργαστής: " + displayName(selectedCpu.brand(), selectedCpu.model(), selectedCpu.skroutzUrl()) +
                            " | socket " + selectedCpu.socket() +
                            " | " + selectedCpu.cores() + "C" +
                            " | " + selectedCpu.baseClockGhz() + " GHz" +
                            " | score " + formatScore(selectedCpu.perfScore()) +
                            " | " + selectedCpu.tdpW() + "W" +
                            " | " + formatPartPrice(selectedCpu.id(), selectedCpu.priceEur())
            );
            if (cpuCaseButton != null) cpuCaseButton.setText("CPU");
        }


        if (selectedGpu == null) {
            gpuLabel.setText("Κάρτα Γραφικών: (Δεν έχει γίνει επιλογή)");
            if (gpuCaseButton != null) gpuCaseButton.setText("GPU");
        } else {
            gpuLabel.setText(
                    "Κάρτα Γραφικών: " + displayName(selectedGpu.brand(), selectedGpu.model(), selectedGpu.skroutzUrl()) +
                            " | " + selectedGpu.vramGb() + "GB" +
                            " | score " + formatScore(selectedGpu.perfScore()) +
                            " | " + selectedGpu.tdpW() + "W" +
                            " | length " + selectedGpu.lengthMm() + "mm" +
                            " | " + formatPartPrice(selectedGpu.id(), selectedGpu.priceEur())
            );
            if (gpuCaseButton != null) gpuCaseButton.setText("GPU");
        }


        if (selectedMobo == null) {
            moboLabel.setText("Μητρική: (Δεν έχει γίνει επιλογή)");
            if (moboCaseButton != null) moboCaseButton.setText("Μητρική");
        } else {
            moboLabel.setText(
                    "Μητρική: " + displayName(selectedMobo.brand(), selectedMobo.model(), selectedMobo.skroutzUrl()) +
                            " | socket " + selectedMobo.socket() +
                            " | " + selectedMobo.formFactor() +
                            " | " + selectedMobo.memoryType() +
                            " | " + formatPartPrice(selectedMobo.id(), selectedMobo.priceEur())
            );
            if (moboCaseButton != null) moboCaseButton.setText("Μητρική");
        }


        if (selectedRam == null) {
            ramLabel.setText("RAM: (Δεν έχει γίνει επιλογή)");
            if (ramCaseButton != null) ramCaseButton.setText("RAM");
        } else {
            ramLabel.setText(
                    "RAM: " + displayName(selectedRam.brand(), selectedRam.model(), selectedRam.skroutzUrl()) +
                            " | " + selectedRam.capacityGb() + "GB" +
                            " | " + selectedRam.sticks() + " sticks" +
                            " | " + selectedRam.speedMhz() + " MHz" +
                            " | " + formatPartPrice(selectedRam.id(), selectedRam.priceEur())
            );
            if (ramCaseButton != null) ramCaseButton.setText("RAM");
        }


        if (selectedStorage == null) {
            storageLabel.setText("Αποθηκευτικός χώρος: (Δεν έχει γίνει επιλογή)");
            if (storageCaseButton != null) storageCaseButton.setText("M.2");
        } else {
            storageLabel.setText(
                    "Αποθηκευτικός χώρος: " + displayName(selectedStorage.brand(), selectedStorage.model(), selectedStorage.skroutzUrl()) +
                            " | " + selectedStorage.type() +
                            " | " + selectedStorage.capacityGb() + "GB" +
                            " | score " + formatScore(selectedStorage.perfScore()) +
                            " | " + formatPartPrice(selectedStorage.id(), selectedStorage.priceEur())
            );
            if (storageCaseButton != null) storageCaseButton.setText("M.2");
        }


        if (selectedPsu == null) {
            psuLabel.setText("Τροφοδοτικό: (Δεν έχει γίνει επιλογή)");
            if (psuCaseButton != null) psuCaseButton.setText("PSU");
        } else {
            psuLabel.setText(
                    "Τροφοδοτικό: " + displayName(selectedPsu.brand(), selectedPsu.model(), selectedPsu.skroutzUrl()) +
                            " | " + selectedPsu.wattageW() + "W" +
                            " | " + formatPartPrice(selectedPsu.id(), selectedPsu.priceEur())
            );
            if (psuCaseButton != null) psuCaseButton.setText("PSU");
        }


        if (selectedCase == null) {
            caseLabel.setText("Κουτί: (Δεν έχει γίνει επιλογή)");
            if (pcCaseButton != null) pcCaseButton.setText("Κουτί");
        } else {
            caseLabel.setText(
                    "Κουτί: " + displayName(selectedCase.brand(), selectedCase.model(), selectedCase.skroutzUrl()) +
                            " | supports " + selectedCase.supportedFormFactors() +
                            " | max GPU " + selectedCase.maxGpuLengthMm() + "mm" +
                            " | max cooler " + selectedCase.maxAirCoolerHeightMm() + "mm" +
                            " | " + formatPartPrice(selectedCase.id(), selectedCase.priceEur())
            );
            if (pcCaseButton != null) pcCaseButton.setText("Κουτί");
        }


        if (selectedCooler == null) {
            coolerLabel.setText("Ψύξη CPU: (Δεν έχει γίνει επιλογή)");
            if (coolerCaseButton != null) coolerCaseButton.setText("Ψύξη");
        } else {
            if (selectedCooler.isAirCooler()) {
                coolerLabel.setText(
                        "Ψύξη CPU: " + displayName(selectedCooler.brand(), selectedCooler.model(), selectedCooler.skroutzUrl()) +
                                " | Air" +
                                " | height " + selectedCooler.heightMm() + "mm" +
                                " | " + selectedCooler.tdpRatingW() + "W rating" +
                                " | " + formatPartPrice(selectedCooler.id(), selectedCooler.priceEur())
                );
            } else {
                coolerLabel.setText(
                        "Ψύξη CPU: " + displayName(selectedCooler.brand(), selectedCooler.model(), selectedCooler.skroutzUrl()) +
                                " | AIO " + selectedCooler.radiatorSizeMm() + "mm" +
                                " | " + selectedCooler.tdpRatingW() + "W rating" +
                                " | " + formatPartPrice(selectedCooler.id(), selectedCooler.priceEur())
                );
            }
            if (coolerCaseButton != null) coolerCaseButton.setText("Ψύξη");
        }


        double totalPrice = 0.0;
        if (selectedCpu != null) totalPrice += resolvePrice(selectedCpu.id(), selectedCpu.priceEur());
        if (selectedGpu != null) totalPrice += resolvePrice(selectedGpu.id(), selectedGpu.priceEur());
        if (selectedMobo != null) totalPrice += resolvePrice(selectedMobo.id(), selectedMobo.priceEur());
        if (selectedRam != null) totalPrice += resolvePrice(selectedRam.id(), selectedRam.priceEur());
        if (selectedStorage != null) totalPrice += resolvePrice(selectedStorage.id(), selectedStorage.priceEur());
        if (selectedPsu != null) totalPrice += resolvePrice(selectedPsu.id(), selectedPsu.priceEur());
        if (selectedCase != null) totalPrice += resolvePrice(selectedCase.id(), selectedCase.priceEur());
        if (selectedCooler != null) totalPrice += resolvePrice(selectedCooler.id(), selectedCooler.priceEur());

        totalPriceLabel.setText(String.format("Συνολική τιμή: €%.2f", totalPrice));


        double rawPerf = scoringService.computeOverallPerf(selectedCpu, selectedGpu, selectedRam, selectedStorage, gamingProfile);
        double perf = Math.min(rawPerf, 100.0);
        String profileLabel = gamingProfile ? "Gaming" : "Creator";

        if (selectedCpu == null && selectedGpu == null) {
            performanceLabel.setText("– / 100");
        } else {
            performanceLabel.setText(String.format("%.1f / 100", perf));
        }


        if (totalPrice > 0 && perf > 0) {
            double valueScore = (perf / totalPrice) * 100.0;
            valueLabel.setText(String.format("%.2f perf/€", valueScore));
        } else {
            valueLabel.setText("– perf/€");
        }



        int estimatedPower = powerEstimator.estimatePower(selectedCpu, selectedGpu, selectedMobo, selectedRam, selectedStorage);
        if (estimatedPower > 0) {
            powerLabel.setText("~" + estimatedPower + " W");
        } else {
            powerLabel.setText("~– W");
        }

        updateCompatibility(estimatedPower);
    }

    private void updateCompatibility(int estimatedPower) {
        String msg = compatibilityService.buildCompatibilityMessage(
                selectedCpu,
                selectedMobo,
                selectedCase,
                selectedCooler,
                selectedRam,
                selectedGpu,
                selectedPsu,
                estimatedPower
        );
        compatibilityLabel.setText("Συμβατότητα: " + msg);
        updateCompatibilityCards(estimatedPower);
    }


    private void updateCompatibilityCards(int estimatedPower) {
        updateSocketCard();
        updateRamCard();
        updateCaseFitCard();
        updateGpuClearanceCard();
        updateCoolerFitCard();
        updatePsuHeadroomCard(estimatedPower);
    }

    private void updateSocketCard() {
        if (selectedCpu == null || selectedMobo == null) {
            setCompatibilityCard(socketCard, socketStatusLabel, socketDetailLabel,
                    "Αναμονή • Socket", "Επιλογή CPU and μητρική.", "PENDING");
            return;
        }

        if (selectedCpu.socket().equalsIgnoreCase(selectedMobo.socket())) {
            setCompatibilityCard(socketCard, socketStatusLabel, socketDetailLabel,
                    "OK • Socket", "Socket CPU " + selectedCpu.socket() + " ταιριάζει με socket μητρικής " + selectedMobo.socket() + ".", "OK");
        } else {
            setCompatibilityCard(socketCard, socketStatusLabel, socketDetailLabel,
                    "Πρόβλημα • Socket", "Socket CPU " + selectedCpu.socket() + " δεν ταιριάζει με socket μητρικής " + selectedMobo.socket() + ".", "ISSUE");
        }
    }

    private void updateRamCard() {
        if (selectedRam == null || selectedMobo == null) {
            setCompatibilityCard(ramCard, ramStatusLabel, ramDetailLabel,
                    "Αναμονή • Τύπος RAM", "Επιλογή RAM and μητρική.", "PENDING");
            return;
        }

        String ramType = inferRamType(selectedRam);
        String moboType = inferMotherboardMemoryType(selectedMobo);

        if ("UNKNOWN".equals(ramType) || "UNKNOWN".equals(moboType)) {
            setCompatibilityCard(ramCard, ramStatusLabel, ramDetailLabel,
                    "Έλεγχος • Τύπος RAM", "Δεν ήταν δυνατή η πλήρης επιβεβαίωση τύπου RAM για αυτή τη μητρική.", "WARN");
        } else if (ramType.equals(moboType)) {
            setCompatibilityCard(ramCard, ramStatusLabel, ramDetailLabel,
                    "OK • Τύπος RAM", ramType + " RAM ταιριάζει με την υποστήριξη της μητρικής.", "OK");
        } else {
            setCompatibilityCard(ramCard, ramStatusLabel, ramDetailLabel,
                    "Πρόβλημα • Τύπος RAM", ramType + " RAM δεν ταιριάζει με την υποστήριξη της μητρικής " + moboType + ".", "ISSUE");
        }
    }

    private void updateCaseFitCard() {
        if (selectedCase == null || selectedMobo == null) {
            setCompatibilityCard(caseFitCard, caseFitStatusLabel, caseFitDetailLabel,
                    "Αναμονή • Κουτί", "Επίλεξε κουτί και μητρική.", "PENDING");
            return;
        }

        if (selectedCase.supportsMotherboardFormFactor(selectedMobo.formFactor())) {
            setCompatibilityCard(caseFitCard, caseFitStatusLabel, caseFitDetailLabel,
                    "OK • Κουτί", "Το κουτί υποστηρίζει " + selectedMobo.formFactor() + " μητρική.", "OK");
        } else {
            setCompatibilityCard(caseFitCard, caseFitStatusLabel, caseFitDetailLabel,
                    "Πρόβλημα • Κουτί", "Το κουτί δεν υποστηρίζει " + selectedMobo.formFactor() + " μητρική.", "ISSUE");
        }
    }

    private void updateGpuClearanceCard() {
        if (selectedGpu == null || selectedCase == null) {
            setCompatibilityCard(gpuClearanceCard, gpuClearanceStatusLabel, gpuClearanceDetailLabel,
                    "Αναμονή • Χώρος GPU", "Επιλογή GPU and case.", "PENDING");
            return;
        }

        if (selectedGpu.lengthMm() <= selectedCase.maxGpuLengthMm()) {
            setCompatibilityCard(gpuClearanceCard, gpuClearanceStatusLabel, gpuClearanceDetailLabel,
                    "OK • Χώρος GPU", selectedGpu.lengthMm() + "mm GPU χωράει στο όριο " + selectedCase.maxGpuLengthMm() + "mm του κουτιού.", "OK");
        } else {
            setCompatibilityCard(gpuClearanceCard, gpuClearanceStatusLabel, gpuClearanceDetailLabel,
                    "Πρόβλημα • Χώρος GPU", selectedGpu.lengthMm() + "mm GPU ξεπερνά το όριο " + selectedCase.maxGpuLengthMm() + "mm του κουτιού.", "ISSUE");
        }
    }

    private void updateCoolerFitCard() {
        if (selectedCooler == null || selectedCase == null) {
            setCompatibilityCard(coolerFitCard, coolerFitStatusLabel, coolerFitDetailLabel,
                    "Αναμονή • Ψύξη CPU", "Επίλεξε ψύξη CPU και κουτί.", "PENDING");
            return;
        }

        if (selectedCooler.isAirCooler()) {
            if (selectedCooler.heightMm() <= selectedCase.maxAirCoolerHeightMm()) {
                setCompatibilityCard(coolerFitCard, coolerFitStatusLabel, coolerFitDetailLabel,
                        "OK • Ψύξη CPU", "Ύψος αερόψυξης " + selectedCooler.heightMm() + "mm χωράει στο όριο " + selectedCase.maxAirCoolerHeightMm() + "mm.", "OK");
            } else {
                setCompatibilityCard(coolerFitCard, coolerFitStatusLabel, coolerFitDetailLabel,
                        "Πρόβλημα • Ψύξη CPU", "Ύψος αερόψυξης " + selectedCooler.heightMm() + "mm ξεπερνά το όριο " + selectedCase.maxAirCoolerHeightMm() + "mm.", "ISSUE");
            }
            return;
        }

        boolean aioSupported = switch (selectedCooler.radiatorSizeMm()) {
            case 240 -> selectedCase.supports240Aio();
            case 280 -> selectedCase.supports280Aio();
            case 360 -> selectedCase.supports360Aio();
            default -> false;
        };

        if (aioSupported) {
            setCompatibilityCard(coolerFitCard, coolerFitStatusLabel, coolerFitDetailLabel,
                    "OK • Ψύξη CPU", selectedCooler.radiatorSizeMm() + "mm AIO υποστηρίζεται.", "OK");
        } else {
            setCompatibilityCard(coolerFitCard, coolerFitStatusLabel, coolerFitDetailLabel,
                    "Πρόβλημα • Ψύξη CPU", "Το κουτί ίσως δεν υποστηρίζει " + selectedCooler.radiatorSizeMm() + "mm AIO.", "ISSUE");
        }
    }

    private void updatePsuHeadroomCard(int estimatedPower) {
        if (selectedPsu == null || estimatedPower <= 0) {
            setCompatibilityCard(psuHeadroomCard, psuHeadroomStatusLabel, psuHeadroomDetailLabel,
                    "Αναμονή • Περιθώριο PSU", "Επιλογή PSU and major components.", "PENDING");
            return;
        }

        int headroom = selectedPsu.wattageW() - estimatedPower;
        if (headroom < 0) {
            setCompatibilityCard(psuHeadroomCard, psuHeadroomStatusLabel, psuHeadroomDetailLabel,
                    "Πρόβλημα • Περιθώριο PSU", selectedPsu.wattageW() + "W PSU είναι κάτω από την εκτίμηση " + estimatedPower + "W κατανάλωσης.", "ISSUE");
        } else if (headroom < MIN_SAFE_HEADROOM_W) {
            setCompatibilityCard(psuHeadroomCard, psuHeadroomStatusLabel, psuHeadroomDetailLabel,
                    "Προσοχή • Περιθώριο PSU", "Μόνο ~" + headroom + "W περιθώριο (" + selectedPsu.wattageW() + "W PSU vs ~" + estimatedPower + "W draw).", "WARN");
        } else {
            setCompatibilityCard(psuHeadroomCard, psuHeadroomStatusLabel, psuHeadroomDetailLabel,
                    "OK • Περιθώριο PSU", selectedPsu.wattageW() + "W PSU δίνει ~" + headroom + "W περιθώριο.", "OK");
        }
    }

    private void setCompatibilityCard(VBox card, Label titleLabel, Label detailLabel,
                                      String title, String detail, String state) {
        if (titleLabel != null) titleLabel.setText(title);
        if (detailLabel != null) detailLabel.setText(detail);
        if (card == null) return;

        String style = "-fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 10; ";
        switch (state) {
            case "OK" -> style += "-fx-background-color: #ecfdf5; -fx-border-color: #86efac;";
            case "WARN" -> style += "-fx-background-color: #fffbeb; -fx-border-color: #fbbf24;";
            case "ISSUE" -> style += "-fx-background-color: #fef2f2; -fx-border-color: #fca5a5;";
            default -> style += "-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1;";
        }
        card.setStyle(style);
    }

    private String inferRamType(RAM ram) {
        if (ram == null) return "UNKNOWN";
        String text = (ram.brand() + " " + ram.model()).toLowerCase();
        int speed = ram.speedMhz();

        if (text.contains("ddr5") || speed >= 4800) return "DDR5";
        if (text.contains("ddr4") || speed <= 4000) return "DDR4";
        return "UNKNOWN";
    }

    private String inferMotherboardMemoryType(Motherboard mobo) {
        if (mobo == null) return "UNKNOWN";
        String explicit = mobo.memoryType();
        if (explicit != null && !explicit.isBlank() && !explicit.equalsIgnoreCase("UNKNOWN")) {
            return explicit.trim().toUpperCase();
        }

        String socket = mobo.socket() != null ? mobo.socket().toLowerCase() : "";
        String model = mobo.model() != null ? mobo.model().toLowerCase() : "";

        if (socket.equals("am5")) return "DDR5";
        if (socket.equals("am4")) return "DDR4";
        if (socket.equals("lga1700")) {
            if (model.contains("ddr4") || model.contains(" d4") || model.endsWith("d4")) return "DDR4";
            if (model.contains("ddr5") || model.contains(" d5") || model.endsWith("d5")) return "DDR5";
        }
        return "UNKNOWN";
    }

    private boolean isRamCompatibleWithMotherboard(RAM ram, Motherboard mobo) {
        String ramType = inferRamType(ram);
        String moboType = inferMotherboardMemoryType(mobo);
        return "UNKNOWN".equals(ramType) || "UNKNOWN".equals(moboType) || ramType.equals(moboType);
    }

    private String buildCurrentSummaryText() {
        StringBuilder sb = new StringBuilder();

        appendLabelLine(sb, cpuLabel);
        appendLabelLine(sb, gpuLabel);
        appendLabelLine(sb, moboLabel);
        appendLabelLine(sb, ramLabel);
        appendLabelLine(sb, storageLabel);
        appendLabelLine(sb, psuLabel);
        appendLabelLine(sb, caseLabel);
        appendLabelLine(sb, coolerLabel);

        sb.append("\n");

        appendLabelLine(sb, performanceLabel);
        appendLabelLine(sb, valueLabel);
        appendLabelLine(sb, powerLabel);
        appendLabelLine(sb, totalPriceLabel);
        appendLabelLine(sb, compatibilityLabel);
        appendLabelLine(sb, suggestionLabel);

        return sb.toString();
    }

    private void appendLabelLine(StringBuilder sb, Label label) {
        if (label != null) {
            sb.append(label.getText()).append("\n");
        }
    }



}
