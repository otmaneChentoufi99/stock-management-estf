package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import ma.estf.magasiner.models.dto.BonCommandeDto;
import ma.estf.magasiner.services.BonCommandeService;

public class BonCommandeListController {

    @FXML
    private TableView<BonCommandeDto> bcTable;
    @FXML
    private TableColumn<BonCommandeDto, String> colNumero;
    @FXML
    private TableColumn<BonCommandeDto, String> colDate;
    @FXML
    private TableColumn<BonCommandeDto, String> colService;
    @FXML
    private TableColumn<BonCommandeDto, String> colStatut;
    @FXML
    private TableColumn<BonCommandeDto, Void> colAction;

    private final BonCommandeService service = new BonCommandeService();

    @FXML
    public void initialize() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateBC"));
        colService.setCellValueFactory(new PropertyValueFactory<>("serviceDemandeur"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("Voir Articles");
            {
                viewBtn.setStyle("-fx-base: #3498db; -fx-text-fill: white;");
                viewBtn.setOnAction(event -> {
                    BonCommandeDto bc = getTableView().getItems().get(getIndex());
                    openDetailsDialog(bc);
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
        bcTable.setItems(FXCollections.observableArrayList(service.getAllBonCommandes()));
    }

    private void openDetailsDialog(BonCommandeDto bc) {
        CartController.pendingSearchQuery = bc.getNumero();
        if (RootController.instance != null) {
            RootController.instance.showCart();
        }
    }
}
