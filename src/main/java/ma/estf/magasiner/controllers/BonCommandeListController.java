package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;

import ma.estf.magasiner.models.dto.BonCommandeDto;
import ma.estf.magasiner.models.dto.LigneBonCommandeDto;
import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.services.BonCommandeService;
import ma.estf.magasiner.services.CategoryService;
import ma.estf.magasiner.services.ArticleService;
import ma.estf.magasiner.dao.SequenceDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

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

    // Details Table
    @FXML private VBox detailsContainer;
    @FXML private TableView<LigneBonCommandeDto> detailsTable;
    @FXML private TableColumn<LigneBonCommandeDto, String> colItemDesignation;
    @FXML private TableColumn<LigneBonCommandeDto, String> colItemCaracteristique;
    @FXML private TableColumn<LigneBonCommandeDto, Integer> colItemQty;
    @FXML private TableColumn<LigneBonCommandeDto, Double> colItemPrice;
    @FXML private TableColumn<LigneBonCommandeDto, Boolean> colItemInvCheck;
    @FXML private TableColumn<LigneBonCommandeDto, String> colItemInvNumbers;
    @FXML private TableColumn<LigneBonCommandeDto, CategoryDto> colItemCategory;
    @FXML private TableColumn<LigneBonCommandeDto, CategoryDto> colItemFormat;

    private final BonCommandeService service = new BonCommandeService();
    private final CategoryService categoryService = new CategoryService();
    private final ArticleService articleService = new ArticleService();
    private final SequenceDao sequenceDao = new SequenceDao();

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

        bcTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadDetails(newVal);
            } else {
                detailsContainer.setVisible(false);
                detailsContainer.setManaged(false);
            }
        });

        setupDetailsTable();

        refreshData();
    }

    private void setupDetailsTable() {
        colItemDesignation.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getArticle().getName()));
        colItemDesignation.setCellFactory(TextFieldTableCell.forTableColumn());
        colItemDesignation.setOnEditCommit(event -> {
            event.getRowValue().getArticle().setName(event.getNewValue());
        });

        colItemCaracteristique.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getArticle().getCaracteristique()));
        colItemCaracteristique.setCellFactory(TextFieldTableCell.forTableColumn());
        colItemCaracteristique.setOnEditCommit(event -> {
            event.getRowValue().getArticle().setCaracteristique(event.getNewValue());
        });

        colItemQty.setCellValueFactory(new PropertyValueFactory<>("quantiteCommandee"));
        
        colItemPrice.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getArticle().getPrixUnit()));

        // Inventory
        colItemInvCheck.setCellValueFactory(cellData -> {
            boolean hasInvs = cellData.getValue().getArticle().getAvailableInventoryNumbers() != null 
                              && !cellData.getValue().getArticle().getAvailableInventoryNumbers().isEmpty();
            return new SimpleBooleanProperty(hasInvs);
        });
        colItemInvCheck.setCellFactory(param -> new CheckBoxTableCell<LigneBonCommandeDto, Boolean>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    CheckBox cb = (CheckBox) getGraphic();
                    if (cb != null) {
                        cb.setOnAction(e -> {
                            LigneBonCommandeDto rowData = getTableView().getItems().get(getIndex());
                            toggleInventory(rowData, cb.isSelected());
                        });
                    }
                }
            }
        });

        colItemInvNumbers.setCellValueFactory(cellData -> {
            List<String> invs = cellData.getValue().getArticle().getAvailableInventoryNumbers();
            return new SimpleStringProperty(invs != null ? String.join(", ", invs) : "");
        });
        colItemInvNumbers.setCellFactory(TextFieldTableCell.forTableColumn());
        colItemInvNumbers.setOnEditCommit(event -> {
            String val = event.getNewValue();
            if (val != null) {
                List<String> list = Arrays.asList(val.split(",\\s*"));
                event.getRowValue().getArticle().setAvailableInventoryNumbers(new ArrayList<>(list));
            }
        });

        // Categories
        ObservableList<CategoryDto> categories = FXCollections.observableArrayList(categoryService.findByType("CATEGORY"));
        ObservableList<CategoryDto> formats = FXCollections.observableArrayList(categoryService.findByType("FORMAT"));

        colItemCategory.setCellValueFactory(cellData -> {
            Set<CategoryDto> cats = cellData.getValue().getArticle().getCategories();
            CategoryDto found = cats.stream().filter(c -> "CATEGORY".equals(c.getType())).findFirst().orElse(null);
            return new SimpleObjectProperty<>(found);
        });
        colItemCategory.setCellFactory(ComboBoxTableCell.forTableColumn(categories));
        colItemCategory.setOnEditCommit(event -> {
            Set<CategoryDto> cats = event.getRowValue().getArticle().getCategories();
            if (cats == null) {
                cats = new HashSet<>();
                event.getRowValue().getArticle().setCategories(cats);
            }
            cats.removeIf(c -> "CATEGORY".equals(c.getType()));
            if (event.getNewValue() != null) cats.add(event.getNewValue());
        });

        colItemFormat.setCellValueFactory(cellData -> {
            Set<CategoryDto> cats = cellData.getValue().getArticle().getCategories();
            CategoryDto found = cats.stream().filter(c -> "FORMAT".equals(c.getType())).findFirst().orElse(null);
            return new SimpleObjectProperty<>(found);
        });
        colItemFormat.setCellFactory(ComboBoxTableCell.forTableColumn(formats));
        colItemFormat.setOnEditCommit(event -> {
            Set<CategoryDto> cats = event.getRowValue().getArticle().getCategories();
            if (cats == null) {
                cats = new HashSet<>();
                event.getRowValue().getArticle().setCategories(cats);
            }
            cats.removeIf(c -> "FORMAT".equals(c.getType()));
            if (event.getNewValue() != null) cats.add(event.getNewValue());
        });
    }

    private void toggleInventory(LigneBonCommandeDto ligne, boolean needsInv) {
        if (needsInv) {
            if (ligne.getArticle().getAvailableInventoryNumbers() == null || ligne.getArticle().getAvailableInventoryNumbers().isEmpty()) {
                int qty = ligne.getQuantiteCommandee();
                List<String> newInvs = new ArrayList<>();
                for (int i = 0; i < qty; i++) {
                    newInvs.add(sequenceDao.getNextInventoryNumber());
                }
                ligne.getArticle().setAvailableInventoryNumbers(newInvs);
            }
        } else {
            if (ligne.getArticle().getAvailableInventoryNumbers() != null) {
                ligne.getArticle().getAvailableInventoryNumbers().clear();
            }
        }
        detailsTable.refresh();
    }

    private void loadDetails(BonCommandeDto bc) {
        if (bc.getLignes() != null) {
            detailsTable.setItems(FXCollections.observableArrayList(bc.getLignes()));
            detailsContainer.setVisible(true);
            detailsContainer.setManaged(true);
        }
    }

    @FXML
    public void handleSaveDetails() {
        BonCommandeDto selectedBc = bcTable.getSelectionModel().getSelectedItem();
        if (selectedBc == null) return;

        try {
            for (LigneBonCommandeDto ligne : selectedBc.getLignes()) {
                articleService.updateArticle(ligne.getArticle());
            }
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Modifications enregistrées avec succès !");
            alert.showAndWait();
            
            refreshData();
            detailsContainer.setVisible(false);
            detailsContainer.setManaged(false);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Échec de l'enregistrement");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
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
            ma.estf.magasiner.models.dto.ArticleDto firstArticle = bc.getLignes().get(0).getArticle();
            if (firstArticle != null) {
                java.util.List<String> invs = firstArticle.getAvailableInventoryNumbers();
                if (invs != null && !invs.isEmpty()) {
                    type = "MATERIEL";
                } else {
                    type = "CONSOMMABLE";
                }
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
