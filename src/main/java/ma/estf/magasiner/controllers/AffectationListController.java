package ma.estf.magasiner.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ma.estf.magasiner.models.dto.AffectationDto;
import ma.estf.magasiner.services.AffectationService;

import java.time.format.DateTimeFormatter;

public class AffectationListController {

    @FXML private TableView<AffectationDto> affectationTable;
    @FXML private TableColumn<AffectationDto, Long> colId;
    @FXML private TableColumn<AffectationDto, String> colDate;
    @FXML private TableColumn<AffectationDto, String> colCategory;
    @FXML private TableColumn<AffectationDto, String> colEmployee;
    @FXML private TableColumn<AffectationDto, String> colDepartment;
    @FXML private TableColumn<AffectationDto, String> colStatus;
    @FXML private TableColumn<AffectationDto, Void> colAction;

    @FXML private ComboBox<String> categoryFilter;
    @FXML private TextField searchField;

    private final AffectationService affectationService = new AffectationService();
    private final javafx.collections.ObservableList<AffectationDto> masterList = FXCollections.observableArrayList();
    private final javafx.collections.transformation.FilteredList<AffectationDto> filteredList = new javafx.collections.transformation.FilteredList<>(masterList, p -> true);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDate().format(formatter)));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colEmployee.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colDepartment.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDepartment() != null ? 
                                       cellData.getValue().getDepartment().getName() : "-"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        categoryFilter.setItems(FXCollections.observableArrayList("Tous", "MATERIEL", "CONSOMMABLE"));
        categoryFilter.setValue("Tous");
        
        categoryFilter.valueProperty().addListener((obs, oldV, newV) -> updatePredicate());
        searchField.textProperty().addListener((obs, oldV, newV) -> updatePredicate());

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("Détails");
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(5, viewBtn);
            {
                viewBtn.setStyle("-fx-base: #3498db; -fx-text-fill: white;");
                
                viewBtn.setOnAction(event -> {
                    AffectationDto affectation = getTableView().getItems().get(getIndex());
                    RootController.instance.showAffectationManage(affectation);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || (getTableView().getItems().get(getIndex()) != null && "CLOSED".equals(getTableView().getItems().get(getIndex()).getStatus()))) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        affectationTable.setItems(filteredList);
        refreshData();
    }

    private void updatePredicate() {
        filteredList.setPredicate(affectation -> {
            // 1. Category Filter
            String category = categoryFilter.getValue();
            if (category != null && !category.equals("Tous")) {
                if (!category.equals(affectation.getCategory())) return false;
            }

            // 2. Search Text Filter
            String searchText = searchField.getText();
            if (searchText == null || searchText.trim().isEmpty()) return true;

            String lowerCaseFilter = searchText.toLowerCase().trim();

            // Check Affectation ID
            if (String.valueOf(affectation.getId()).contains(lowerCaseFilter)) return true;

            // Check Employee Name
            if (affectation.getEmployeeName() != null && 
                affectation.getEmployeeName().toLowerCase().contains(lowerCaseFilter)) return true;

            // Check Department
            if (affectation.getDepartment() != null && 
                affectation.getDepartment().getName().toLowerCase().contains(lowerCaseFilter)) return true;

            // Check Items (Inventory Number, Article Name, BC Number)
            if (affectation.getItems() != null) {
                for (ma.estf.magasiner.models.dto.AffectationItemDto item : affectation.getItems()) {
                    // Inventory Number
                    if (item.getInventoryNumber() != null && 
                        item.getInventoryNumber().toLowerCase().contains(lowerCaseFilter)) return true;
                    
                    if (item.getArticle() != null) {
                        // Article Name
                        if (item.getArticle().getName() != null && 
                            item.getArticle().getName().toLowerCase().contains(lowerCaseFilter)) return true;
                        
                        // BC Number
                        if (item.getArticle().getBonCommandeNumero() != null && 
                            item.getArticle().getBonCommandeNumero().toLowerCase().contains(lowerCaseFilter)) return true;
                    }
                }
            }

            return false;
        });
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }

    private void refreshData() {
        masterList.setAll(affectationService.getAllAffectations());
    }
}
