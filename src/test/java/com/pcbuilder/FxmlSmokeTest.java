package com.pcbuilder;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in desktop test: mvn -Dpcbuilder.uiTests=true test */
@EnabledIfSystemProperty(named = "pcbuilder.uiTests", matches = "true")
class FxmlSmokeTest {
    @Test void localViewsLoadAndInitializeOnJavaFxThread() throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.startup(() -> {
            try {
                for (String view : new String[] {"MainView.fxml", "BuildGuide.fxml",
                        "OptimizationView.fxml", "TutorialLibraryView.fxml"}) {
                    assertNotNull(new FXMLLoader(App.class.getResource(view)).load(), view);
                }
                result.complete(null);
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        try {
            result.get(30, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }
}
