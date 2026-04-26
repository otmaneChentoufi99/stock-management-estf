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
    private TableColumn<BonCommandeDto, String> fournissuer;
    @FXML
    private TableColumn<BonCommandeDto, String> colDate;
    @FXML
    private TableColumn<BonCommandeDto, String> colStatut;
    @FXML
    private TableColumn<BonCommandeDto, Void> colAction;

    private final BonCommandeService service = new BonCommandeService();

    @FXML
    public void initialize() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        fournissuer.setCellValueFactory(new PropertyValueFactory<>("fournisseur"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateBC"));
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
        CartMaterialController.pendingSearchQuery = bc.getNumero();
        CartConsumableController.pendingSearchQuery = bc.getNumero();

        // Check the type of the first article to decide which cart to show
        String type = "MATERIEL"; // default
        if (bc.getLignes() != null && !bc.getLignes().isEmpty()) {
            if (bc.getLignes().get(0).getArticle() != null) {
                type = bc.getLignes().get(0).getArticle().getType();
            }
        }

        if (RootController.instance != null) {
            if ("CONSOMMABLE".equals(type)) {
                RootController.instance.showCartConsumable();
            } else {
                RootController.instance.showCartMaterial();
            }
        }
    }
}
