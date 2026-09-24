package com.pcbuilder;

import com.pcbuilder.pricing.LivePriceService;
import com.pcbuilder.pricing.MarketplaceSearchResult;
import javafx.application.Platform;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;




public class SkroutzChromiumWindow {

    private static final String SKROUTZ_HOME_URL = "https://www.skroutz.gr/";
    private static final String SKROUTZ_SEARCH_URL = "https://www.skroutz.gr/search?keyphrase=";
    private static final ScheduledExecutorService IMPORT_TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "pcbuilder-jcef-import-timeout");
        t.setDaemon(true);
        return t;
    });

    private final LivePriceService livePriceService;
    private final Consumer<MarketplaceSearchResult> importHandler;

    private JFrame frame;
    private JTextField addressField;
    private JButton backButton;
    private JButton forwardButton;
    private JButton refreshButton;
    private JButton getProductButton;
    private JLabel statusLabel;

    private CefClient client;
    private CefBrowser browser;

    public SkroutzChromiumWindow(LivePriceService livePriceService,
                                 Consumer<MarketplaceSearchResult> importHandler) {
        this.livePriceService = livePriceService;
        this.importHandler = importHandler;
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            try {
                createAndShow();
            } catch (Exception e) {
                throw new RuntimeException("Could not start embedded Chromium browser.", e);
            }
        });
    }

    private void createAndShow() throws Exception {
        if (frame != null) {
            frame.toFront();
            frame.requestFocus();
            return;
        }

        CefApp cefApp = ChromiumBrowserManager.getOrCreateApp();
        client = cefApp.createClient();
        browser = client.createBrowser(SKROUTZ_HOME_URL, false, false);

        addressField = new JTextField(SKROUTZ_HOME_URL, 48);
        JButton goButton = new JButton("Μετάβαση");
        backButton = new JButton("◀");
        forwardButton = new JButton("▶");
        refreshButton = new JButton("Ανανέωση");
        getProductButton = new JButton("Εισαγωγή Προϊόντος");
        getProductButton.setPreferredSize(new Dimension(170, 30));
        getProductButton.setEnabled(false);
        statusLabel = new JLabel("Άνοιξε συγκεκριμένη σελίδα προϊόντος Skroutz και πάτησε Εισαγωγή Προϊόντος.");

        goButton.addActionListener(event -> navigate(addressField.getText()));
        addressField.addActionListener(event -> navigate(addressField.getText()));
        backButton.addActionListener(event -> {
            if (browser != null && browser.canGoBack()) {
                browser.goBack();
            }
        });
        forwardButton.addActionListener(event -> {
            if (browser != null && browser.canGoForward()) {
                browser.goForward();
            }
        });
        refreshButton.addActionListener(event -> {
            if (browser != null) {
                browser.reload();
            }
        });
        getProductButton.addActionListener(event -> importCurrentProduct());

        client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser cefBrowser, CefFrame frame, String url) {
                SwingUtilities.invokeLater(() -> {
                    addressField.setText(url != null ? url : "");
                    updateState();
                });
            }

            @Override
            public void onTitleChange(CefBrowser cefBrowser, String title) {
                SwingUtilities.invokeLater(() -> {
                    if (SkroutzChromiumWindow.this.frame != null) {
                        SkroutzChromiumWindow.this.frame.setTitle(
                                title == null || title.isBlank()
                                        ? "Περιήγηση Skroutz στο Chromium"
                                        : title + " - Skroutz Browser");
                    }
                    updateState();
                });
            }
        });

        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser cefBrowser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                SwingUtilities.invokeLater(() -> {
                    backButton.setEnabled(canGoBack);
                    forwardButton.setEnabled(canGoForward);
                    refreshButton.setEnabled(true);
                    updateState();
                    if (isLoading) {
                        statusLabel.setText("Φόρτωση σελίδας στο ενσωματωμένο Chromium...");
                    }
                });
            }

            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                SwingUtilities.invokeLater(() -> {
                    updateState();
                    String url = currentUrl();
                    if (livePriceService.isLikelyProductUrl(url)) {
                        statusLabel.setText("Εντοπίστηκε σελίδα προϊόντος. Πάτησε Εισαγωγή Προϊόντος για προσθήκη στην εφαρμογή.");
                    } else {
                        statusLabel.setText("Περιηγήσου στο Skroutz μέσα από το Chromium. Άνοιξε προϊόν για ενεργοποίηση της εισαγωγής.");
                    }
                });
            }

            @Override
            public void onLoadError(CefBrowser cefBrowser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(
                        "Το Chromium δεν μπόρεσε να φορτώσει τη σελίδα" + (failedUrl != null ? ": " + failedUrl : ".") +
                                (errorText != null && !errorText.isBlank() ? " [" + errorText + "]" : "")));
            }
        });



        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        toolbar.add(new JLabel("Αναζήτηση ή URL:"), BorderLayout.WEST);
        toolbar.add(addressField, BorderLayout.CENTER);

        JPanel toolbarButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        toolbarButtons.add(goButton);
        toolbarButtons.add(backButton);
        toolbarButtons.add(forwardButton);
        toolbarButtons.add(refreshButton);
        toolbarButtons.add(getProductButton);
        toolbar.add(toolbarButtons, BorderLayout.EAST);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        footer.add(statusLabel);
        

        JPanel root = new JPanel(new BorderLayout());
        root.add(toolbar, BorderLayout.NORTH);
        root.add(browser.getUIComponent(), BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        frame = new JFrame("Περιήγηση Skroutz στο Chromium");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(root);
        frame.setSize(1280, 860);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disposeBrowser();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                disposeBrowser();
            }
        });
        frame.setVisible(true);
        updateState();
    }

    private void navigate(String value) {
        if (browser == null) {
            return;
        }
        String text = value != null ? value.trim() : "";
        if (text.isBlank()) {
            browser.loadURL(SKROUTZ_HOME_URL);
            return;
        }
        if (looksLikeUrl(text)) {
            String url = text.startsWith("http://") || text.startsWith("https://") ? text : "https://" + text;
            browser.loadURL(url);
            return;
        }
        browser.loadURL(SKROUTZ_SEARCH_URL + URLEncoder.encode(text, StandardCharsets.UTF_8));
    }

    private void importCurrentProduct() {
        String url = currentUrl();
        if (!livePriceService.isLikelyProductUrl(url)) {
            statusLabel.setText("Άνοιξε πρώτα συγκεκριμένη σελίδα προϊόντος Skroutz και μετά πάτησε Εισαγωγή Προϊόντος.");
            updateState();
            return;
        }

        getProductButton.setEnabled(false);
        statusLabel.setText("Γίνεται ανάγνωση της τρέχουσας σελίδας προϊόντος...");

        AtomicBoolean handled = new AtomicBoolean(false);
        ScheduledFuture<?> timeout = IMPORT_TIMEOUT_EXECUTOR.schedule(() -> {
            if (handled.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(() -> statusLabel.setText(
                        "Δεν διαβάστηκε έγκαιρα το κείμενο της σελίδας. Γίνεται εισαγωγή με fallback από το URL..."));
                importProductWithRenderedText(url, "");
            }
        }, 3500L, TimeUnit.MILLISECONDS);

        try {
            browser.getText(new CefStringVisitor() {
                @Override
                public void visit(String renderedText) {
                    if (handled.compareAndSet(false, true)) {
                        timeout.cancel(false);
                        SwingUtilities.invokeLater(() -> statusLabel.setText("Γίνεται εισαγωγή προϊόντος από το τρέχον URL..."));
                        importProductWithRenderedText(url, renderedText != null ? renderedText : "");
                    }
                }
            });
        } catch (Exception e) {
            timeout.cancel(false);
            if (handled.compareAndSet(false, true)) {
                statusLabel.setText("Δεν ήταν δυνατή η ανάγνωση της σελίδας. Γίνεται εισαγωγή με fallback από το URL...");
                importProductWithRenderedText(url, "");
            }
        }
    }

    private void importProductWithRenderedText(String url, String renderedText) {
        AtomicBoolean finished = new AtomicBoolean(false);
        ScheduledFuture<?> timeout = IMPORT_TIMEOUT_EXECUTOR.schedule(() -> {
            if (finished.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(() -> {
                    updateState();
                    statusLabel.setText("Η εισαγωγή καθυστέρησε. Δοκίμασε ξανά ή άνοιξε ξανά τη σελίδα προϊόντος.");
                });
            }
        }, 9000L, TimeUnit.MILLISECONDS);

        Thread thread = new Thread(() -> {
            Optional<MarketplaceSearchResult> result;
            try {
                result = livePriceService.importFromRenderedProductPage(url, limitRenderedText(renderedText));
            } catch (Exception e) {
                result = Optional.empty();
            }

            if (!finished.compareAndSet(false, true)) {
                return;
            }
            timeout.cancel(false);

            Optional<MarketplaceSearchResult> finalResult = result;
            SwingUtilities.invokeLater(() -> {
                updateState();
                if (finalResult.isEmpty()) {
                    statusLabel.setText("Η σελίδα δεν μπόρεσε να μετατραπεί σε υποστηριζόμενο εξάρτημα.");
                    return;
                }
                MarketplaceSearchResult imported = finalResult.get();
                if (!imported.importable()) {
                    statusLabel.setText("Η σελίδα προϊόντος φορτώθηκε, αλλά το αποτέλεσμα δεν μπορεί να εισαχθεί ως εξάρτημα.");
                    return;
                }
                if (importHandler != null) {
                    Platform.runLater(() -> importHandler.accept(imported));
                }
                statusLabel.setText("Εισήχθη " + greekCategory(imported) + ": " + imported.title() + " | " + imported.priceText());
            });
        }, "pcbuilder-jcef-import");
        thread.setDaemon(true);
        thread.start();
    }

    private String limitRenderedText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int max = 180_000;
        if (text.length() <= max) {
            return text;
        }
        int head = 115_000;
        int tail = 65_000;
        return text.substring(0, head) + "\n...\n" + text.substring(text.length() - tail);
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

    private String currentUrl() {
        return browser != null && browser.getURL() != null ? browser.getURL() : "";
    }

    private void updateState() {
        String url = currentUrl();
        boolean importable = livePriceService != null && livePriceService.isLikelyProductUrl(url);
        getProductButton.setEnabled(importable);
        if (browser != null) {
            backButton.setEnabled(browser.canGoBack());
            forwardButton.setEnabled(browser.canGoForward());
        }
    }

    private boolean looksLikeUrl(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.") || lower.contains("skroutz.gr") || value.startsWith("/");
    }

    private void disposeBrowser() {
        if (browser != null) {
            browser.close(true);
            browser = null;
        }
        if (client != null) {
            client.dispose();
            client = null;
        }
        frame = null;
    }
}
