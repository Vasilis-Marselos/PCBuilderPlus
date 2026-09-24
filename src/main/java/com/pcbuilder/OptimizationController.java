package com.pcbuilder;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;





public class OptimizationController {

    @FXML private Label profileLabel;
    @FXML private TextArea hardwareSummaryArea;
    @FXML private TextArea windowsTipsArea;
    @FXML private TextArea biosTipsArea;

    @FXML
    private void initialize() {

        if (windowsTipsArea != null && windowsTipsArea.getText().isEmpty()) {
            windowsTipsArea.setText("Οι προτάσεις Windows θα εμφανιστούν με βάση το προφίλ σου.");
        }
        if (biosTipsArea != null && biosTipsArea.getText().isEmpty()) {
            biosTipsArea.setText("Οι προτάσεις BIOS/UEFI θα εμφανιστούν με βάση το προφίλ σου.");
        }
    }

    public void setProfileText(String text) {
        profileLabel.setText(text);
    }

    public void setHardwareSummary(String summary) {
        hardwareSummaryArea.setText(summary);
    }

    public void setWindowsTips(String tips) {
        windowsTipsArea.setText(tips);
    }

    public void setBiosTips(String tips) {
        biosTipsArea.setText(tips);
    }
}
