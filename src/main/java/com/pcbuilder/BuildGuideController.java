package com.pcbuilder;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class BuildGuideController {

    @FXML private Label profileLabel;
    @FXML private TextArea buildSummaryArea;
    @FXML private TextArea tipsArea;

    @FXML
    public void initialize() {
        tipsArea.setText("""
                Γενικές οδηγίες συναρμολόγησης:

                1. Τοποθέτησε πρώτα τον επεξεργαστή στη μητρική πριν βάλεις τη μητρική στο κουτί.
                2. Ευθυγράμμισε το τριγωνικό σημάδι του CPU με το αντίστοιχο σημάδι του socket. Μην το πιέσεις με δύναμη.
                3. Τοποθέτησε τη RAM στις προτεινόμενες θέσεις της μητρικής, συνήθως A2/B2, μέχρι να κουμπώσουν τα clips.
                4. Βάλε τη μητρική πάνω στα standoffs του κουτιού και βίδωσέ τη προσεκτικά.
                5. Κατά την εγκατάσταση της GPU, κράτησε την κάρτα ίσια όσο βιδώνεις ώστε να αποφύγεις sagging.
                6. Σύνδεσε το 24-pin ATX και το 8-pin CPU power πριν κάνεις τελικό cable management.
                7. Έλεγξε ξανά τα front-panel connectors, όπως power switch, reset και LEDs.
                8. Κάνε ένα πρώτο boot test με ανοιχτό πλαϊνό πάνελ για να ελέγξεις ανεμιστήρες και POST ενδείξεις.
                """);
    }

    public void setBuildSummary(String summary) {
        buildSummaryArea.setText(summary);
    }

    public void setProfile(String profileName) {
        profileLabel.setText("Οδηγός για προφίλ: " + profileName);
    }
}
