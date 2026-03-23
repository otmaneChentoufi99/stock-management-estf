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
    @FXML private TableColumn<AffectationDto, String> colEmployee;
    @FXML private TableColumn<AffectationDto, String> colDepartment;
    @FXML private TableColumn<AffectationDto, Void> colAction;

    private final AffectationService affectationService = new AffectationService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDate().format(formatter)));
        colEmployee.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colDepartment.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDepartment() != null ? 
                                       cellData.getValue().getDepartment().getName() : "-"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("Voir Détails");
            {
                viewBtn.setStyle("-fx-base: #3498db; -fx-text-fill: white;");
                viewBtn.setOnAction(event -> {
                    AffectationDto affectation = getTableView().getItems().get(getIndex());
                    // For now, we just print or we could show a dialog
                    System.out.println("Viewing details for affectation: " + affectation.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(viewBtn);
                }
            }
        });

        refreshData();
    }

    private void refreshData() {
        affectationTable.setItems(FXCollections.observableArrayList(affectationService.getAllAffectations()));
    }
}
