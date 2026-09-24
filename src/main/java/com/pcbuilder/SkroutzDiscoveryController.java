package com.pcbuilder;

import com.pcbuilder.pricing.LivePriceService;
import com.pcbuilder.pricing.MarketplaceSearchResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

public class SkroutzDiscoveryController {

    private static final String SKROUTZ_HOME_URL = "https://www.skroutz.gr";
    private static final String SKROUTZ_SEARCH_URL = "https://www.skroutz.gr/search?keyphrase=";

    @FXML private TextField addressField;
    @FXML private Button goButton;
    @FXML private Button backButton;
    @FXML private Button forwardButton;
    @FXML private Button refreshButton;
    @FXML private Button importButton;
    @FXML private WebView browserView;
    @FXML private Label statusLabel;

    private LivePriceService livePriceService;
    private Consumer<MarketplaceSearchResult> importHandler;
    private WebEngine webEngine;

    @FXML
    public void initialize() {
        webEngine = browserView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36");

        importButton.setDisable(true);
        backButton.setDisable(true);
        forwardButton.setDisable(true);
        statusLabel.setText("Περιηγήσου στο Skroutz, άνοιξε σελίδα προϊόντος και πάτησε Εισαγωγή Προϊόντος.");

        webEngine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (addressField != null && !addressField.isFocused()) {
                addressField.setText(newLocation);
            }
            updateNavigationState();
            updateImportAvailability();
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case RUNNING -> statusLabel.setText("Φόρτωση σελίδας Skroutz...");
                case SUCCEEDED -> {
                    updateNavigationState();
                    updateImportAvailability();
                }
                case FAILED, CANCELLED -> {
                    updateNavigationState();
                    updateImportAvailability();
                    statusLabel.setText("Η ενσωματωμένη σελίδα Skroutz δεν φορτώθηκε. Δοκίμασε ανανέωση ή άλλη σελίδα.");
                }
                default -> {
                }
            }
        });

        Platform.runLater(() -> loadUrl(SKROUTZ_HOME_URL));
    }

    public void setDependencies(LivePriceService livePriceService, Consumer<MarketplaceSearchResult> importHandler) {
        this.livePriceService = livePriceService;
        this.importHandler = importHandler;
        updateImportAvailability();
    }

    @FXML
    private void onGo() {
        String value = addressField.getText() != null ? addressField.getText().trim() : "";
        if (value.isBlank()) {
            loadUrl(SKROUTZ_HOME_URL);
            return;
        }

        if (looksLikeUrl(value)) {
            String url = value.startsWith("http://") || value.startsWith("https://") ? value : "https://" + value;
            loadUrl(url);
            return;
        }

        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        loadUrl(SKROUTZ_SEARCH_URL + encoded);
    }

    @FXML
    private void onBack() {
        WebHistory history = webEngine.getHistory();
        if (history.getCurrentIndex() > 0) {
            history.go(-1);
        }
    }

    @FXML
    private void onForward() {
        WebHistory history = webEngine.getHistory();
        if (history.getCurrentIndex() + 1 < history.getEntries().size()) {
            history.go(1);
        }
    }

    @FXML
    private void onRefresh() {
        webEngine.reload();
    }

    @FXML
    private void onImportCurrent() {
        if (livePriceService == null) {
            statusLabel.setText("Η υπηρεσία marketplace δεν είναι έτοιμη.");
            return;
        }

        String currentUrl = webEngine.getLocation();
        if (!livePriceService.isLikelyProductUrl(currentUrl)) {
            statusLabel.setText("Άνοιξε πρώτα συγκεκριμένη σελίδα προϊόντος Skroutz και μετά κάνε εισαγωγή.");
            importButton.setDisable(true);
            return;
        }

        importButton.setDisable(true);
        statusLabel.setText("Γίνεται εισαγωγή του τρέχοντος προϊόντος από Skroutz...");

        String renderedText = "";
        try {
            Object result = webEngine.executeScript("document.body ? document.body.innerText : ''");
            if (result != null) {
                renderedText = result.toString();
            }
        } catch (Exception ignored) {
            renderedText = "";
        }

        String finalRenderedText = renderedText;
        Task<Optional<MarketplaceSearchResult>> task = new Task<>() {
            @Override
            protected Optional<MarketplaceSearchResult> call() {
                return livePriceService.importFromRenderedProductPage(currentUrl, finalRenderedText);
            }
        };

        task.setOnSucceeded(event -> {
            updateImportAvailability();
            Optional<MarketplaceSearchResult> result = task.getValue();
            if (result.isEmpty()) {
                statusLabel.setText("Η σελίδα δεν μπόρεσε να μετατραπεί σε υποστηριζόμενο εξάρτημα.");
                return;
            }

            MarketplaceSearchResult imported = result.get();
            if (!imported.importable() || importHandler == null) {
                statusLabel.setText("Η σελίδα φορτώθηκε, αλλά το προϊόν δεν μπόρεσε να μετατραπεί σε υποστηριζόμενο εξάρτημα.");
                return;
            }

            importHandler.accept(imported);
            statusLabel.setText("Εισήχθη " + greekCategory(imported) + ": " + imported.title() + " | " + imported.priceText() + " | " + imported.specsSummary());
        });

        task.setOnFailed(event -> {
            updateImportAvailability();
            Throwable error = task.getException();
            statusLabel.setText("Η εισαγωγή απέτυχε" + (error != null && error.getMessage() != null ? ": " + error.getMessage() : "."));
        });

        Thread thread = new Thread(task, "pcbuilder-skroutz-import-current");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadUrl(String url) {
        webEngine.load(url);
        if (addressField != null) {
            addressField.setText(url);
        }
        updateNavigationState();
        updateImportAvailability();
    }

    private void updateNavigationState() {
        WebHistory history = webEngine.getHistory();
        int index = history.getCurrentIndex();
        backButton.setDisable(index <= 0);
        forwardButton.setDisable(index < 0 || index + 1 >= history.getEntries().size());
    }

    private void updateImportAvailability() {
        String currentUrl = webEngine != null ? webEngine.getLocation() : "";
        boolean importable = livePriceService != null && livePriceService.isLikelyProductUrl(currentUrl);
        importButton.setDisable(!importable);

        if (webEngine == null) {
            return;
        }

        String title = webEngine.getTitle() != null ? webEngine.getTitle().trim() : "";
        if (importable) {
            statusLabel.setText("Εντοπίστηκε σελίδα προϊόντος" + (title.isBlank() ? "." : ": " + title) + " Πάτησε Εισαγωγή Προϊόντος για προσθήκη στον τρέχοντα κατάλογο.");
        } else {
            statusLabel.setText("Περιηγήσου κανονικά στο Skroutz. Όταν ανοίξεις συγκεκριμένο προϊόν, η εισαγωγή θα ενεργοποιηθεί.");
        }
    }

    private String greekCategory(MarketplaceSearchResult result) {
        if (result == null || result.category() == null) {
            return "προϊόν";
        }
        return switch (result.category()) {
            case CPU -> "επεξεργαστής";
            case GPU -> "κάρτα γραφικών";
            case MOTHERBOARD -> "μητρική";
            case RAM -> "RAM";
            case STORAGE -> "αποθηκευτικός χώρος";
            case PSU -> "τροφοδοτικό";
            case CASE -> "κουτί";
            case COOLER -> "ψύξη CPU";
        };
    }

    private boolean looksLikeUrl(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.") || lower.contains("skroutz.gr") || value.startsWith("/");
    }
}
