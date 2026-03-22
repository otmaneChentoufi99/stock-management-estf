package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ma.estf.magasiner.models.dto.BonCommandeDto;
import ma.estf.magasiner.models.dto.LigneBonCommandeDto;

public class BonCommandeDetailsController {

    @FXML private Label titleLabel;
    @FXML private TableView<LigneBonCommandeDto> lignesTable;
    @FXML private TableColumn<LigneBonCommandeDto, String> colRef;
    @FXML private TableColumn<LigneBonCommandeDto, String> colName;
    @FXML private TableColumn<LigneBonCommandeDto, Integer> colQty;

    @FXML
    public void initialize() {
        colRef.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getReference()));
        colName.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getName()));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantiteCommandee"));
    }

    public void initData(BonCommandeDto bc) {
        titleLabel.setText("Lignes de Commande: " + bc.getNumero());
        if (bc.getLignes() != null) {
            lignesTable.setItems(FXCollections.observableArrayList(bc.getLignes()));
        }
    }
}
