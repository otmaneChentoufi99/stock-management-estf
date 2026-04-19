package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import ma.estf.magasiner.models.dto.AffectationDto;
import ma.estf.magasiner.models.dto.AffectationItemDto;
import ma.estf.magasiner.services.AffectationService;

import java.time.format.DateTimeFormatter;

public class AffectationManageController {

    @FXML private Label titleLabel;
    @FXML private Label beneficiaryLabel;
    @FXML private Label dateLabel;

    @FXML private TableView<AffectationItemDto> itemsTable;
    @FXML private TableColumn<AffectationItemDto, String> colRef;
    @FXML private TableColumn<AffectationItemDto, String> colName;
    @FXML private TableColumn<AffectationItemDto, Integer> colQuantity;
    @FXML private TableColumn<AffectationItemDto, String> colInventory;
    @FXML private TableColumn<AffectationItemDto, String> colCondition;

    @FXML private VBox actionPanel;
    @FXML private Label selectedItemLabel;
    @FXML private TextField actionQuantityField;
    @FXML private TextField targetField;
    @FXML private ComboBox<String> conditionCombo;

    private final AffectationService affectationService = new AffectationService();
    private AffectationDto currentAffectation;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colRef.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getReference()));
        colName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getName()));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colInventory.setCellValueFactory(new PropertyValueFactory<>("inventoryNumber"));
        colCondition.setCellValueFactory(new PropertyValueFactory<>("condition"));

        conditionCombo.setItems(FXCollections.observableArrayList("GOOD", "DAMAGED", "BROKEN"));
        conditionCombo.setValue("GOOD");

        itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getQuantity() > 0) {
                showActionPanel(newVal);
            } else {
                hideActionPanel();
            }
        });
    }

    public void setAffectation(AffectationDto aff) {
        this.currentAffectation = aff;
        titleLabel.setText("Gestion de l'Affectation #" + aff.getId());
        beneficiaryLabel.setText("Bénéficiaire: " + aff.getEmployeeName() + 
            (aff.getDepartment() != null ? " (" + aff.getDepartment().getName() + ")" : ""));
        dateLabel.setText("Date: " + aff.getDate().format(formatter));
        
        refreshItems();
    }

    private void refreshItems() {
        // We need to re-fetch to get updated quantities or items
        java.util.List<AffectationDto> all = affectationService.getAllAffectations();
        currentAffectation = all.stream()
                .filter(a -> a.getId().equals(currentAffectation.getId()))
                .findFirst()
                .orElse(currentAffectation);
        
        itemsTable.setItems(FXCollections.observableArrayList(currentAffectation.getItems()));
    }

    private void showActionPanel(AffectationItemDto item) {
        actionPanel.setVisible(true);
        actionPanel.setManaged(true);
        selectedItemLabel.setText("Action sur: " + item.getArticle().getName());
        actionQuantityField.setText(String.valueOf(item.getQuantity()));
    }

    private void hideActionPanel() {
        actionPanel.setVisible(false);
        actionPanel.setManaged(false);
    }

    @FXML
    private void handleTransferSelected() {
        AffectationItemDto selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            int qty = Integer.parseInt(actionQuantityField.getText());
            String target = targetField.getText();
            
            if (target == null || target.trim().isEmpty()) {
                showError("Veuillez indiquer un bénéficiaire ou département cible.");
                return;
            }

            affectationService.transferItems(currentAffectation.getId(), selected.getArticle().getId(), qty, target, null);
            refreshItems();
            showAlert("Succès", "Transfert effectué avec succès.");
        } catch (NumberFormatException e) {
            showError("Quantité invalide.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleReturnSelected() {
        AffectationItemDto selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            int qty = Integer.parseInt(actionQuantityField.getText());
            String condition = conditionCombo.getValue();

            affectationService.returnToInventory(currentAffectation.getId(), selected.getArticle().getId(), qty, condition);
            refreshItems();
            showAlert("Succès", "Article retourné au stock.");
        } catch (NumberFormatException e) {
            showError("Quantité invalide.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleTransferAll() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Transférer Tout");
        dialog.setHeaderText("Transférer tous les articles à une nouvelle destination");
        dialog.setContentText("Bénéficiaire / Service:");

        dialog.showAndWait().ifPresent(target -> {
            try {
                affectationService.transferAllItems(currentAffectation.getId(), target, null);
                refreshItems();
                showAlert("Succès", "Tous les articles ont été transférés.");
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
    }

    @FXML
    private void handleReturnAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Retourner Tout");
        confirm.setHeaderText("Retourner tous les articles au stock?");
        confirm.setContentText("Les articles seront marqués comme étant en bon état (GOOD).");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    affectationService.returnAllItems(currentAffectation.getId(), "GOOD");
                    refreshItems();
                    showAlert("Succès", "Tous les articles on été retournés au stock.");
                } catch (Exception e) {
                    showError(e.getMessage());
                }
            }
        });
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
