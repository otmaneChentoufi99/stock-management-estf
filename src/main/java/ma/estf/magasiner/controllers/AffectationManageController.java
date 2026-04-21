package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ma.estf.magasiner.models.dto.AffectationDto;
import ma.estf.magasiner.models.dto.AffectationItemDto;
import ma.estf.magasiner.services.AffectationService;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
    @FXML private TableColumn<AffectationItemDto, Void> colSelect;

    @FXML private TableView<AffectationItemDto> selectionTable;
    @FXML private TableColumn<AffectationItemDto, String> colSelName;
    @FXML private TableColumn<AffectationItemDto, Integer> colSelQty;
    @FXML private TableColumn<AffectationItemDto, String> colSelCond;
    @FXML private TableColumn<AffectationItemDto, Void> colSelRemove;

    @FXML private TextField targetField;

    private final AffectationService affectationService = new AffectationService();
    private AffectationDto currentAffectation;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ObservableList<AffectationItemDto> selectionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colRef.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getReference()));
        colName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getName()));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colInventory.setCellValueFactory(new PropertyValueFactory<>("inventoryNumber"));
        colCondition.setCellValueFactory(new PropertyValueFactory<>("condition"));

        colSelect.setCellFactory(param -> new TableCell<>() {
            private final Button addBtn = new Button("+");
            {
                addBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
                addBtn.setOnAction(event -> {
                    AffectationItemDto item = getTableView().getItems().get(getIndex());
                    handleAddToSelection(item);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()).getQuantity() <= 0) {
                    setGraphic(null);
                } else {
                    setGraphic(addBtn);
                }
            }
        });

        // Selection Table
        colSelName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getName()));
        colSelQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        
        colSelQty.setCellFactory(param -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>();
            private AffectationItemDto currentDto;
            private final javafx.beans.value.ChangeListener<Integer> listener = (obs, oldV, newV) -> {
                if (currentDto != null) currentDto.setQuantity(newV);
            };

            {
                spinner.setEditable(true);
                spinner.setPrefWidth(80);
                spinner.valueProperty().addListener(listener);
            }

            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setGraphic(null);
                    currentDto = null;
                } else {
                    currentDto = getTableView().getItems().get(getIndex());
                    int max = currentAffectation.getItems().stream()
                        .filter(i -> i.getId().equals(currentDto.getId()))
                        .findFirst().map(AffectationItemDto::getQuantity).orElse(currentDto.getQuantity());
                    
                    spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, max), currentDto.getQuantity()));
                    setGraphic(spinner);
                }
            }
        });

        colSelCond.setCellFactory(param -> new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList("GOOD", "DAMAGED", "BROKEN"));
            private AffectationItemDto currentDto;
            private final javafx.beans.value.ChangeListener<String> listener = (obs, oldV, newV) -> {
                if (currentDto != null) currentDto.setCondition(newV);
            };

            {
                combo.valueProperty().addListener(listener);
                combo.setPrefWidth(100);
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setGraphic(null);
                    currentDto = null;
                } else {
                    currentDto = getTableView().getItems().get(getIndex());
                    combo.setValue(currentDto.getCondition());
                    setGraphic(combo);
                }
            }
        });

        colSelRemove.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btn.setOnAction(event -> selectionList.remove(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        selectionTable.setItems(selectionList);
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
        java.util.List<AffectationDto> all = affectationService.getAllAffectations();
        currentAffectation = all.stream()
                .filter(a -> a.getId().equals(currentAffectation.getId()))
                .findFirst()
                .orElse(currentAffectation);
        
        itemsTable.setItems(FXCollections.observableArrayList(currentAffectation.getItems()));
        selectionList.clear();
    }

    private void handleAddToSelection(AffectationItemDto item) {
        // Only add if not already in selection
        boolean exists = selectionList.stream().anyMatch(i -> i.getId().equals(item.getId()));
        if (!exists) {
            // Clone the item DTO for selection so quantity doesn't bind to main table yet
            AffectationItemDto clone = AffectationItemDto.builder()
                    .id(item.getId())
                    .article(item.getArticle())
                    .quantity(1) // Default to 1
                    .inventoryNumber(item.getInventoryNumber())
                    .condition("GOOD")
                    .build();
            selectionList.add(clone);
        }
    }

    @FXML
    private void handleBulkTransfer() {
        if (selectionList.isEmpty()) return;

        String target = targetField.getText();
        if (target == null || target.trim().isEmpty()) {
            showError("Veuillez indiquer un bénéficiaire ou département cible.");
            return;
        }

        try {
            Map<Long, Integer> itemsMap = selectionList.stream()
                    .collect(Collectors.toMap(AffectationItemDto::getId, AffectationItemDto::getQuantity));
            
            java.io.File pdf = affectationService.transferItems(currentAffectation.getId(), itemsMap, target, null);
            refreshItems();
            openPdf(pdf);
            showAlert("Succès", "Transfert groupé effectué. Facture générée.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleBulkReturn() {
        if (selectionList.isEmpty()) return;

        try {
            java.io.File pdf = affectationService.returnToInventory(currentAffectation.getId(), new java.util.ArrayList<>(selectionList));
            refreshItems();
            openPdf(pdf);
            showAlert("Succès", "Retour au stock effectué. Bon de retour généré.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void openPdf(java.io.File file) {
        if (java.awt.Desktop.isDesktopSupported()) {
            new Thread(() -> {
                try {
                    java.awt.Desktop.getDesktop().open(file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
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
                // We can reuse bulk logic technically
                Map<Long, Integer> itemsMap = currentAffectation.getItems().stream()
                        .filter(i -> i.getQuantity() > 0)
                        .collect(Collectors.toMap(AffectationItemDto::getId, AffectationItemDto::getQuantity));
                
                java.io.File pdf = affectationService.transferItems(currentAffectation.getId(), itemsMap, target, null);
                refreshItems();
                openPdf(pdf);
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
                    java.util.List<AffectationItemDto> items = currentAffectation.getItems().stream()
                            .filter(i -> i.getQuantity() > 0)
                            .peek(i -> i.setCondition("GOOD"))
                            .collect(Collectors.toList());
                    
                    java.io.File pdf = affectationService.returnToInventory(currentAffectation.getId(), items);
                    refreshItems();
                    openPdf(pdf);
                    showAlert("Succès", "Tous les articles ont été retournés.");
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
