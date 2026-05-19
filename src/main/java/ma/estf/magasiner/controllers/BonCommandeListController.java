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
    private final List<String> sessionAllocatedNumbers = new java.util.ArrayList<>();

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
        colItemDesignation.setCellFactory(column -> new TableCell<>() {
            private TextArea textArea;

            @Override
            public void startEdit() {
                if (!isEmpty()) {
                    super.startEdit();
                    createTextArea();
                    setText(null);
                    setGraphic(textArea);
                    textArea.selectAll();
                    textArea.requestFocus();
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textArea != null) {
                            textArea.setText(getString());
                        }
                        setText(null);
                        setGraphic(textArea);
                    } else {
                        setText(getString());
                        setGraphic(null);
                        setWrapText(true);
                    }
                }
            }

            private void createTextArea() {
                textArea = new TextArea(getString());
                textArea.setWrapText(true);
                textArea.setPrefRowCount(calculateRowCount(getString()));
                textArea.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
                textArea.textProperty().addListener((obs, oldVal, newVal) -> {
                    textArea.setPrefRowCount(calculateRowCount(newVal));
                });
                textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) {
                        commitEdit(textArea.getText());
                    }
                });
                textArea.setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });
            }

            private String getString() {
                return getItem() == null ? "" : getItem();
            }

            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    getTableRow().getItem().getArticle().setName(newValue);
                }
            }
        });

        colItemCaracteristique.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getArticle().getCaracteristique()));
        colItemCaracteristique.setCellFactory(param -> new TableCell<LigneBonCommandeDto, String>() {
            private final TextArea textArea = new TextArea();
            private boolean updating = false;

            {
                textArea.setPromptText("Caractéristique...");
                textArea.setWrapText(true);
                textArea.textProperty().addListener((obs, oldVal, newVal) -> {
                    textArea.setPrefRowCount(calculateRowCount(newVal));
                    if (updating) return;
                    LigneBonCommandeDto item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null && item.getArticle() != null) {
                        item.getArticle().setCaracteristique(newVal != null ? newVal : "");
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    updating = true;
                    textArea.setText(item == null ? "" : item);
                    textArea.setPrefRowCount(calculateRowCount(item));
                    updating = false;
                    setGraphic(textArea);
                }
            }
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
                
                // Re-sync sessionAllocatedNumbers from the current state of the table
                sessionAllocatedNumbers.clear();
                for (LigneBonCommandeDto ligne : detailsTable.getItems()) {
                    if (ligne.getArticle() != null && ligne.getArticle().getAvailableInventoryNumbers() != null) {
                        for (String inv : ligne.getArticle().getAvailableInventoryNumbers()) {
                            if (inv != null && !inv.trim().isEmpty() && !sessionAllocatedNumbers.contains(inv)) {
                                sessionAllocatedNumbers.add(inv);
                            }
                        }
                    }
                }
                sessionAllocatedNumbers.sort((a, b) -> {
                    try {
                        return Long.compare(Long.parseLong(a), Long.parseLong(b));
                    } catch (NumberFormatException e) {
                        return a.compareTo(b);
                    }
                });
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
        colItemCategory.setCellFactory(param -> new TableCell<LigneBonCommandeDto, CategoryDto>() {
            private final ComboBox<CategoryDto> comboBox = new ComboBox<>(categories);
            private boolean updating = false;

            {
                comboBox.setPromptText("Sélectionner...");
                comboBox.setMaxWidth(Double.MAX_VALUE);
                comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) return;
                    LigneBonCommandeDto rowData = getTableRow() != null ? getTableRow().getItem() : null;
                    if (rowData != null && rowData.getArticle() != null) {
                        Set<CategoryDto> cats = rowData.getArticle().getCategories();
                        if (cats == null) {
                            cats = new HashSet<>();
                            rowData.getArticle().setCategories(cats);
                        }
                        cats.removeIf(c -> "CATEGORY".equals(c.getType()));
                        if (newVal != null) cats.add(newVal);
                    }
                });
            }

            @Override
            protected void updateItem(CategoryDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    updating = true;
                    comboBox.setValue(item);
                    updating = false;
                    setGraphic(comboBox);
                }
            }
        });

        colItemFormat.setCellValueFactory(cellData -> {
            Set<CategoryDto> cats = cellData.getValue().getArticle().getCategories();
            CategoryDto found = cats.stream().filter(c -> "FORMAT".equals(c.getType())).findFirst().orElse(null);
            return new SimpleObjectProperty<>(found);
        });
        colItemFormat.setCellFactory(param -> new TableCell<LigneBonCommandeDto, CategoryDto>() {
            private final ComboBox<CategoryDto> comboBox = new ComboBox<>(formats);
            private boolean updating = false;

            {
                comboBox.setPromptText("Sélectionner...");
                comboBox.setMaxWidth(Double.MAX_VALUE);
                comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) return;
                    LigneBonCommandeDto rowData = getTableRow() != null ? getTableRow().getItem() : null;
                    if (rowData != null && rowData.getArticle() != null) {
                        Set<CategoryDto> cats = rowData.getArticle().getCategories();
                        if (cats == null) {
                            cats = new HashSet<>();
                            rowData.getArticle().setCategories(cats);
                        }
                        cats.removeIf(c -> "FORMAT".equals(c.getType()));
                        if (newVal != null) cats.add(newVal);
                    }
                });
            }

            @Override
            protected void updateItem(CategoryDto item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    updating = true;
                    comboBox.setValue(item);
                    updating = false;
                    setGraphic(comboBox);
                }
            }
        });
    }

    private void toggleInventory(LigneBonCommandeDto targetLigne, boolean needsInv) {
        if (needsInv) {
            if (targetLigne.getArticle().getAvailableInventoryNumbers() == null) {
                targetLigne.getArticle().setAvailableInventoryNumbers(new ArrayList<>());
            }
            if (targetLigne.getArticle().getAvailableInventoryNumbers().isEmpty()) {
                // Temporary dummy element so it is recognized as checked for our redistribution
                targetLigne.getArticle().getAvailableInventoryNumbers().add("TEMP");
            }
        } else {
            if (targetLigne.getArticle().getAvailableInventoryNumbers() != null) {
                targetLigne.getArticle().getAvailableInventoryNumbers().clear();
            }
        }

        // Recalculate and redistribute inventory numbers for all checked articles
        List<LigneBonCommandeDto> checkedLines = new ArrayList<>();
        int totalQtyNeeded = 0;
        for (LigneBonCommandeDto line : detailsTable.getItems()) {
            if (line.getArticle().getAvailableInventoryNumbers() != null && !line.getArticle().getAvailableInventoryNumbers().isEmpty()) {
                checkedLines.add(line);
                totalQtyNeeded += line.getQuantiteCommandee();
            }
        }

        // If we need more sequence values than currently allocated, fetch them
        int diff = totalQtyNeeded - sessionAllocatedNumbers.size();
        for (int i = 0; i < diff; i++) {
            sessionAllocatedNumbers.add(sequenceDao.getNextInventoryNumber());
        }

        // Sort the numbers to make sure they are in order
        sessionAllocatedNumbers.sort((a, b) -> {
            try {
                return Long.compare(Long.parseLong(a), Long.parseLong(b));
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });

        // Distribute the numbers sequentially to all checked lines
        int index = 0;
        for (LigneBonCommandeDto line : checkedLines) {
            int qty = line.getQuantiteCommandee();
            List<String> subList = new ArrayList<>();
            for (int i = 0; i < qty; i++) {
                if (index < sessionAllocatedNumbers.size()) {
                    subList.add(sessionAllocatedNumbers.get(index++));
                }
            }
            line.getArticle().setAvailableInventoryNumbers(subList);
        }

        // Clear any remaining unchecked lines
        for (LigneBonCommandeDto line : detailsTable.getItems()) {
            if (!checkedLines.contains(line)) {
                if (line.getArticle().getAvailableInventoryNumbers() != null) {
                    line.getArticle().getAvailableInventoryNumbers().clear();
                }
            }
        }

        detailsTable.refresh();
    }

    private void loadDetails(BonCommandeDto bc) {
        if (bc.getLignes() != null) {
            sessionAllocatedNumbers.clear();
            for (LigneBonCommandeDto ligne : bc.getLignes()) {
                if (ligne.getArticle() != null && ligne.getArticle().getAvailableInventoryNumbers() != null) {
                    for (String inv : ligne.getArticle().getAvailableInventoryNumbers()) {
                        if (inv != null && !inv.trim().isEmpty() && !sessionAllocatedNumbers.contains(inv)) {
                            sessionAllocatedNumbers.add(inv);
                        }
                    }
                }
            }
            sessionAllocatedNumbers.sort((a, b) -> {
                try {
                    return Long.compare(Long.parseLong(a), Long.parseLong(b));
                } catch (NumberFormatException e) {
                    return a.compareTo(b);
                }
            });
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

    private int calculateRowCount(String text) {
        if (text == null || text.isEmpty()) return 1;
        int explicitLines = text.split("\r\n|\r|\n", -1).length;
        int estimatedLines = (int) Math.ceil((double) text.length() / 45.0);
        int lines = Math.max(explicitLines, estimatedLines);
        return Math.min(4, Math.max(1, lines));
    }
}
