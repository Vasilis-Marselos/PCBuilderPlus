package com.pcbuilder;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;





public class TutorialLibraryController {

    @FXML private Label profileLabel;
    @FXML private ListView<String> topicsList;
    @FXML private Label topicTitleLabel;
    @FXML private Label topicCategoryLabel;
    @FXML private TextArea topicBodyArea;
    @FXML private Label videoPlaceholderLabel;
    @FXML private Button openVideoButton;

    private final ObservableList<String> topics = FXCollections.observableArrayList();
    private final Map<String, TutorialInfo> tutorials = new HashMap<>();

    private String currentVideoUrl;

    private record TutorialInfo(
            String title,
            String category,
            String body,
            String videoDescription,
            String videoUrl
    ) {}

    @FXML
    private void initialize() {
        topicTitleLabel.setText("Επίλεξε θέμα");
        topicCategoryLabel.setText("");
        topicBodyArea.setText("Επίλεξε ένα θέμα από τη λίστα για να δεις λεπτομέρειες.");
        videoPlaceholderLabel.setText("Βίντεο: δεν έχει επιλεγεί θέμα.");
        if (openVideoButton != null) {
            openVideoButton.setDisable(true);
            openVideoButton.setText("Άνοιγμα προτεινόμενου βίντεο");
        }

        loadTutorials();

        topicsList.setItems(topics);
        topicsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> showTutorial(newVal)
        );
    }

    private void loadTutorials() {
        addTutorial("1. Εγκατάσταση CPU", "Συναρμολόγηση hardware", """
                Βασικά βήματα:
                1. Τοποθέτησε τη μητρική πάνω στο κουτί της ή σε αντιστατική επιφάνεια.
                2. Άνοιξε τον μηχανισμό συγκράτησης του socket.
                3. Ευθυγράμμισε το μικρό τρίγωνο της CPU με το αντίστοιχο σημάδι στο socket.
                4. Άφησε απαλά την CPU να κάτσει στη θέση της, χωρίς πίεση ή μετακίνηση.
                5. Κλείσε το socket cover και ασφάλισε τον μηχανισμό.
                6. Βάλε μικρή ποσότητα θερμοαγώγιμης πάστας στο κέντρο.
                7. Τοποθέτησε την ψύκτρα σύμφωνα με το manual και σφίξε σταυρωτά τις βίδες.
                """,
                "Προτεινόμενο βίντεο: τοποθέτηση CPU και θερμοαγώγιμης πάστας.",
                "https://www.youtube.com/results?search_query=how+to+install+cpu+thermal+paste+pc+build"
        );

        addTutorial("2. Εγκατάσταση RAM", "Συναρμολόγηση hardware", """
                Σημαντικά σημεία:
                - Έλεγξε το manual της μητρικής για τις σωστές θέσεις RAM, συνήθως A2 + B2.
                - Άνοιξε πλήρως τα clips των DIMM slots.
                - Ευθυγράμμισε το notch της RAM με την εγκοπή του slot.
                - Πίεσε σταθερά μέχρι να ακουστεί το click από τα clips.
                - Για dual channel, ακολούθησε πάντα τις προτεινόμενες θέσεις του manual.
                """,
                "Προτεινόμενο βίντεο: εγκατάσταση RAM και σωστές θέσεις DIMM.",
                "https://www.youtube.com/results?search_query=how+to+install+ram+dual+channel+a2+b2"
        );

        addTutorial("3. Εγκατάσταση GPU", "Συναρμολόγηση hardware", """
                Βασική διαδικασία:
                1. Εντόπισε το κύριο PCIe x16 slot, συνήθως το επάνω πλήρους μήκους slot.
                2. Αφαίρεσε τα απαραίτητα PCIe covers από το πίσω μέρος του κουτιού.
                3. Τοποθέτησε την GPU ίσια μέσα στο slot μέχρι να ασφαλίσει το latch.
                4. Βίδωσε το bracket της GPU στο κουτί για στήριξη.
                5. Σύνδεσε τα PCIe power cables από το PSU. Σε high-end GPUs προτίμησε ξεχωριστά καλώδια.
                6. Έλεγξε ότι κανένα καλώδιο δεν ακουμπάει σε ανεμιστήρες.
                """,
                "Προτεινόμενο βίντεο: εγκατάσταση GPU και σύνδεση PCIe power καλωδίων.",
                "https://www.youtube.com/results?search_query=how+to+install+graphics+card+pcie+power+cables"
        );

        addTutorial("4. Εγκατάσταση αποθηκευτικού χώρου", "Storage", """
                Βασικές πληροφορίες SSD / HDD:
                - NVMe drives: τοποθετούνται σε M.2 slots και ασφαλίζουν με βίδα ή latch.
                - SATA SSD / HDD: τοποθετούνται σε drive bays ή brackets με βίδες.
                - Για SATA drives χρειάζεται SATA data cable στη μητρική και SATA power από το PSU.
                - Για system drive προτίμησε NVMe ή SATA SSD για γρήγορο boot και φόρτωση εφαρμογών.
                """,
                "Προτεινόμενο βίντεο: εγκατάσταση NVMe M.2 SSD και SATA SSD.",
                "https://www.youtube.com/results?search_query=how+to+install+nvme+m.2+ssd+sata+ssd+pc"
        );

        addTutorial("5. Cable management και airflow", "Κουτί και ψύξη", """
                Στόχοι:
                - Να κινείται ο αέρας ελεύθερα από μπροστά/κάτω προς πίσω/πάνω.
                - Να περνούν τα καλώδια πίσω από το motherboard tray όπου είναι δυνατόν.

                Πρακτικές συμβουλές:
                - Ομαδοποίησε καλώδια με Velcro straps ή zip ties.
                - Κράτησε τα καλώδια μακριά από φτερωτές ανεμιστήρων.
                - Μπροστά: 2 intake fans είναι καλή αρχή.
                - Πίσω/πάνω: 1–2 exhaust fans βοηθούν στην απομάκρυνση ζεστού αέρα.
                - Πριν κλείσεις το κουτί, έλεγξε ξανά ότι κανένα καλώδιο δεν ακουμπάει σε ανεμιστήρα.
                """,
                "Προτεινόμενο βίντεο: cable management και βασική ροή αέρα κουτιού.",
                "https://www.youtube.com/results?search_query=pc+cable+management+airflow+guide"
        );

        addTutorial("6. Πρώτη εκκίνηση και BIOS", "BIOS / πρώτη εκκίνηση", """
                Στην πρώτη εκκίνηση:
                - Σύνδεσε την οθόνη στην GPU και όχι στη μητρική, αν υπάρχει ξεχωριστή κάρτα γραφικών.
                - Μπες στο BIOS/UEFI με Del ή F2, ανάλογα με τη μητρική.
                - Έλεγξε ότι αναγνωρίζονται CPU, συνολική RAM και δίσκοι.
                - Ενεργοποίησε XMP/EXPO ώστε η RAM να τρέχει στην ονομαστική της ταχύτητα.
                - Ρύθμισε boot order, πρώτα USB για εγκατάσταση OS και μετά τον system drive.
                - Κάνε Save & Exit και συνέχισε με εγκατάσταση λειτουργικού.
                """,
                "Προτεινόμενο βίντεο: πρώτη εκκίνηση, BIOS/UEFI, XMP/EXPO και boot order.",
                "https://www.youtube.com/results?search_query=first+boot+pc+build+bios+xmp+expo+boot+order"
        );

        addTutorial("7. Ρύθμιση Windows για Gaming", "Βελτιστοποίηση Windows", """
                Μετά την εγκατάσταση Windows και drivers:
                - Εγκατέστησε chipset και GPU drivers από τις επίσημες ιστοσελίδες.
                - Ενεργοποίησε Game Mode από Ρυθμίσεις → Gaming → Game Mode.
                - Στις ρυθμίσεις γραφικών, όρισε τα βασικά παιχνίδια σε «Υψηλή απόδοση».
                - Απενεργοποίησε overlays και startup apps που δεν χρησιμοποιείς.
                - Ρύθμισε την οθόνη στο μέγιστο refresh rate από τις ρυθμίσεις οθόνης.
                """,
                "Προτεινόμενο βίντεο: βασικές ρυθμίσεις Windows για gaming απόδοση.",
                "https://www.youtube.com/results?search_query=windows+gaming+optimization+game+mode+gpu+driver+refresh+rate"
        );

        addTutorial("8. Ρύθμιση Windows για editing / 3D", "Βελτιστοποίηση Windows", """
                Για editing και content creation:
                - Εγκατέστησε πρώτα chipset, GPU και storage drivers.
                - Χρησιμοποίησε Balanced ή High performance power plan σε βαριές εργασίες.
                - Τοποθέτησε project files και cache/scratch στον πιο γρήγορο δίσκο, ιδανικά NVMe.
                - Κράτησε 15–20% ελεύθερο χώρο στους δίσκους για καλύτερη απόδοση.
                - Ρύθμισε την εφαρμογή editing ή 3D ώστε να χρησιμοποιεί GPU και αρκετή RAM.
                """,
                "Προτεινόμενο βίντεο: ρύθμιση Windows και scratch/cache για editing ή 3D.",
                "https://www.youtube.com/results?search_query=windows+optimization+video+editing+3d+rendering+scratch+cache+nvme"
        );

        addTutorial("9. Έλεγχος ασφαλείας πριν την εκκίνηση", "Ασφάλεια", """
                Σύντομο checklist:
                - Δεν υπάρχουν χαλαρές βίδες μέσα στο κουτί.
                - Όλα τα power cables έχουν κουμπώσει πλήρως, όπως 24-pin, CPU 8-pin, GPU PCIe και drives.
                - Η ψύκτρα CPU είναι σταθερά τοποθετημένη και ο ανεμιστήρας της είναι συνδεδεμένος.
                - Οι ανεμιστήρες κουτιού είναι συνδεδεμένοι σε headers ή fan hub.
                - Τα καλώδια δεν ακουμπούν σε κανέναν ανεμιστήρα.
                - Ο διακόπτης του PSU είναι στη θέση I πριν πατήσεις το power button του κουτιού.
                """,
                "Προτεινόμενο βίντεο: τελικός έλεγχος PC build πριν την πρώτη εκκίνηση.",
                "https://www.youtube.com/results?search_query=pc+build+final+checklist+before+first+boot"
        );
    }

    private void addTutorial(String key, String category, String body, String videoDescription, String videoUrl) {
        topics.add(key);
        tutorials.put(key, new TutorialInfo(key, category, body, videoDescription, videoUrl));
    }

    private void showTutorial(String key) {
        if (key == null) {
            topicTitleLabel.setText("Επίλεξε θέμα");
            topicCategoryLabel.setText("");
            topicBodyArea.setText("Επίλεξε ένα θέμα από τη λίστα για να δεις λεπτομέρειες.");
            videoPlaceholderLabel.setText("Βίντεο: δεν έχει επιλεγεί θέμα.");
            currentVideoUrl = null;
            if (openVideoButton != null) {
                openVideoButton.setDisable(true);
            }
            return;
        }

        TutorialInfo info = tutorials.get(key);
        if (info == null) {
            topicTitleLabel.setText(key);
            topicCategoryLabel.setText("");
            topicBodyArea.setText("Δεν υπάρχουν διαθέσιμες λεπτομέρειες για αυτό το θέμα.");
            videoPlaceholderLabel.setText("Βίντεο: δεν υπάρχουν δεδομένα.");
            currentVideoUrl = null;
            if (openVideoButton != null) {
                openVideoButton.setDisable(true);
            }
            return;
        }

        topicTitleLabel.setText(info.title());
        topicCategoryLabel.setText("Κατηγορία: " + info.category());
        topicBodyArea.setText(info.body());
        videoPlaceholderLabel.setText(info.videoDescription());
        currentVideoUrl = info.videoUrl();
        if (openVideoButton != null) {
            openVideoButton.setDisable(false);
        }
    }

    @FXML
    private void onOpenVideo() {
        if (currentVideoUrl == null || currentVideoUrl.isBlank()) {
            videoPlaceholderLabel.setText("Δεν υπάρχει διαθέσιμο link βίντεο για αυτό το θέμα.");
            return;
        }

        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                videoPlaceholderLabel.setText("Δεν ήταν δυνατό να ανοίξει ο browser. Link: " + currentVideoUrl);
                return;
            }
            Desktop.getDesktop().browse(new URI(currentVideoUrl));
        } catch (Exception ex) {
            videoPlaceholderLabel.setText("Αποτυχία ανοίγματος βίντεο. Link: " + currentVideoUrl);
        }
    }

    public void setProfile(String profileName) {
        profileLabel.setText("Προφίλ: " + profileName);
    }
}
