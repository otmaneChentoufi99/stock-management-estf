package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ma.estf.magasiner.models.dto.AffectationDto;
import ma.estf.magasiner.models.dto.AffectationItemDto;
import ma.estf.magasiner.services.AffectationService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AffectationConsumableManageController {

    @FXML private Label titleLabel;
    @FXML private Label beneficiaryLabel;
    @FXML private Label dateLabel;

    @FXML private TableView<AffectationItemDto> itemsTable;
    @FXML private TableColumn<AffectationItemDto, String> colName;
    @FXML private TableColumn<AffectationItemDto, Integer> colQuantity;
    @FXML private TableColumn<AffectationItemDto, String> colInventory;
    @FXML private TableColumn<AffectationItemDto, String> colCondition;

    private final AffectationService affectationService = new AffectationService();
    private AffectationDto currentAffectation;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getName()));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colInventory.setCellValueFactory(new PropertyValueFactory<>("inventoryNumber"));
        colCondition.setCellValueFactory(new PropertyValueFactory<>("condition"));
    }

    public void setAffectation(AffectationDto aff) {
        this.currentAffectation = aff;
        titleLabel.setText("Détails de l'Affectation #" + aff.getId() + " (Consommable)");
        beneficiaryLabel.setText("Bénéficiaire: " + aff.getEmployeeName() + 
            (aff.getDepartment() != null ? " (" + aff.getDepartment().getName() + ")" : ""));
        dateLabel.setText("Date: " + aff.getDate().format(formatter));
        
        refreshItems();
    }

    private void refreshItems() {
        List<AffectationDto> all = affectationService.getAllAffectations();
        currentAffectation = all.stream()
                .filter(a -> a.getId().equals(currentAffectation.getId()))
                .findFirst()
                .orElse(currentAffectation);
        
        itemsTable.setItems(FXCollections.observableArrayList(currentAffectation.getItems()));
    }
}
