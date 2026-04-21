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
        categoryFilter.valueProperty().addListener((obs, oldV, newV) -> {
            filteredList.setPredicate(affectation -> {
                if (newV == null || newV.equals("Tous")) return true;
                return newV.equals(affectation.getCategory());
            });
        });

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

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }

    private void refreshData() {
        masterList.setAll(affectationService.getAllAffectations());
    }
}
