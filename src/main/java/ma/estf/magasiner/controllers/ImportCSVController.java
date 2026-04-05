package ma.estf.magasiner.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import ma.estf.magasiner.services.BonCommandeService;

import java.io.File;

public class ImportCSVController {

    @FXML private TextField numeroBCField;
    @FXML private TextField serviceDemandeurField;
    @FXML private javafx.scene.control.ComboBox<String> typeComboBox;
    @FXML private Label selectedFileLabel;
    @FXML private Label statusLabel;

    private File selectedFile;
    private final BonCommandeService service = new BonCommandeService();

    @FXML
    public void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Bon de Commande Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        
        selectedFile = fileChooser.showOpenDialog(numeroBCField.getScene().getWindow());
        if (selectedFile != null) {
            selectedFileLabel.setText(selectedFile.getName());
            statusLabel.setText("");
        }
    }

    @FXML
    public void handleImport() {
        if (selectedFile == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a CSV file first.");
            return;
        }
        String numero = numeroBCField.getText();
        String serviceDemandeur = serviceDemandeurField.getText();
        String type = typeComboBox.getValue();
        
        if (numero == null || numero.trim().isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("N° Bon Commande is required.");
            return;
        }

        if (type == null || type.trim().isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Type de Bon Commande is required.");
            return;
        }

        try {
            service.importExcelAsBonCommande(selectedFile.getAbsolutePath(), numero, serviceDemandeur, type);
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Import successful! Data added to database.");
            numeroBCField.clear();
            serviceDemandeurField.clear();
            typeComboBox.getSelectionModel().clearSelection();
            selectedFile = null;
            selectedFileLabel.setText("No file selected...");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Import failed: " + e.getMessage());
        }
    }
}
