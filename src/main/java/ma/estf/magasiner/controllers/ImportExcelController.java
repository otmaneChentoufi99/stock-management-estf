package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.util.converter.DefaultStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.models.dto.ParsedArticleItem;
import ma.estf.magasiner.models.dto.ParsedBonCommande;
import ma.estf.magasiner.services.BonCommandeService;
import ma.estf.magasiner.services.CategoryService;

import java.io.File;

public class ImportExcelController {


    @FXML private Label selectedFileLabel;
    @FXML private Label statusLabel;

    // READ-ONLY display
    @FXML private Label numeroLabel;
    @FXML private Label fournisseurLabel;

    @FXML private VBox tableContainer;
    @FXML private TableView<ParsedArticleItem> articlesTable;
    @FXML private TableColumn<ParsedArticleItem, String> colDesignation;
    @FXML private TableColumn<ParsedArticleItem, Integer> colQuantity;
    @FXML private TableColumn<ParsedArticleItem, Boolean> colNeedsInvNum;
    @FXML private TableColumn<ParsedArticleItem, CategoryDto> colCategory;
    @FXML private TableColumn<ParsedArticleItem, CategoryDto> colFormat;
    @FXML private TableColumn<ParsedArticleItem, String> colCaracteristique;
    @FXML private TableColumn<ParsedArticleItem, Double> colPrixUnit;

    private File selectedFile;

    private final BonCommandeService service = new BonCommandeService();
    private final CategoryService categoryService = new CategoryService();

    private ParsedBonCommande parsedData;
    private ObservableList<CategoryDto> categoriesOnly;
    private ObservableList<CategoryDto> formatsOnly;

    @FXML
    public void initialize() {

        articlesTable.setEditable(true);

        colDesignation.setCellValueFactory(cellData -> cellData.getValue().designationProperty());
        
        // Custom Cell Factory for Designation to commit on focus loss
        colDesignation.setCellFactory(column -> new TableCell<>() {
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
                    if (event.getCode() == KeyCode.ESCAPE) {
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
                    getTableRow().getItem().setDesignation(newValue);
                }
            }
        });
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        colNeedsInvNum.setCellValueFactory(cellData ->
                cellData.getValue().needsInventoryNumberProperty());
        colNeedsInvNum.setCellFactory(CheckBoxTableCell.forTableColumn(colNeedsInvNum));

        categoriesOnly = FXCollections.observableArrayList(categoryService.findByType("CATEGORY"));
        formatsOnly = FXCollections.observableArrayList(categoryService.findByType("FORMAT"));

        colCategory.setCellValueFactory(cellData ->
                cellData.getValue().categoryProperty());
        colCategory.setCellFactory(param -> new TableCell<ParsedArticleItem, CategoryDto>() {
            private final ComboBox<CategoryDto> comboBox = new ComboBox<>(categoriesOnly);
            private boolean updating = false;

            {
                comboBox.setPromptText("Sélectionner...");
                comboBox.setMaxWidth(Double.MAX_VALUE);
                comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) return;
                    ParsedArticleItem item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setCategory(newVal);
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

        colFormat.setCellValueFactory(cellData ->
                cellData.getValue().formatProperty());
        colFormat.setCellFactory(param -> new TableCell<ParsedArticleItem, CategoryDto>() {
            private final ComboBox<CategoryDto> comboBox = new ComboBox<>(formatsOnly);
            private boolean updating = false;

            {
                comboBox.setPromptText("Sélectionner...");
                comboBox.setMaxWidth(Double.MAX_VALUE);
                comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (updating) return;
                    ParsedArticleItem item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setFormat(newVal);
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

        colCaracteristique.setCellValueFactory(cellData ->
                cellData.getValue().caracteristiqueProperty());
        colCaracteristique.setCellFactory(param -> new TableCell<ParsedArticleItem, String>() {
            private final TextArea textArea = new TextArea();
            private boolean updating = false;

            {
                textArea.setPromptText("Caractéristique...");
                textArea.setWrapText(true);
                textArea.textProperty().addListener((obs, oldVal, newVal) -> {
                    textArea.setPrefRowCount(calculateRowCount(newVal));
                    if (updating) return;
                    ParsedArticleItem item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        item.setCaracteristique(newVal != null ? newVal : "");
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

        colPrixUnit.setCellValueFactory(cellData ->
                cellData.getValue().prixUnitProperty().asObject());

        // Custom Cell Factory for Price to commit on focus loss
        colPrixUnit.setCellFactory(column -> new TableCell<>() {
            private TextField textField;
            private final DoubleStringConverter converter = new DoubleStringConverter();

            @Override
            public void startEdit() {
                if (!isEmpty()) {
                    super.startEdit();
                    createTextField();
                    setText(null);
                    setGraphic(textField);
                    textField.selectAll();
                    textField.requestFocus();
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() == null ? "0.0" : getItem().toString());
                setGraphic(null);
            }

            @Override
            public void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        if (textField != null) {
                            textField.setText(item == null ? "0.0" : item.toString());
                        }
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(item == null ? "0.0" : item.toString());
                        setGraphic(null);
                    }
                }
            }

            private void createTextField() {
                textField = new TextField(getItem() == null ? "0.0" : getItem().toString());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
                textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) {
                        try {
                            commitEdit(converter.fromString(textField.getText()));
                        } catch (Exception e) {
                            cancelEdit();
                        }
                    }
                });
                textField.setOnAction(event -> {
                    try {
                        commitEdit(converter.fromString(textField.getText()));
                    } catch (Exception e) {
                        cancelEdit();
                    }
                });
                textField.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                });
            }

            @Override
            public void commitEdit(Double newValue) {
                super.commitEdit(newValue);
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    getTableRow().getItem().setPrixUnit(newValue);
                }
            }
        });

        // Single click to edit (extended for price)
        articlesTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1 && !articlesTable.getSelectionModel().isEmpty()) {
                TablePosition<ParsedArticleItem, ?> pos = articlesTable.getFocusModel().getFocusedCell();
                if (pos != null) {
                    int colIndex = pos.getColumn();
                    if (colIndex == articlesTable.getColumns().indexOf(colDesignation)) {
                        articlesTable.edit(pos.getRow(), colDesignation);
                    } else if (colIndex == articlesTable.getColumns().indexOf(colPrixUnit)) {
                        articlesTable.edit(pos.getRow(), colPrixUnit);
                    }
                }
            }
        });

        tableContainer.setVisible(false);
        tableContainer.setManaged(false);
    }

    @FXML
    public void handleSelectFile() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Bon de Commande Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        selectedFile = fileChooser.showOpenDialog(selectedFileLabel.getScene().getWindow());

        if (selectedFile != null) {
            selectedFileLabel.setText(selectedFile.getName());
            statusLabel.setText("");

            tableContainer.setVisible(false);
            tableContainer.setManaged(false);

            parsedData = null;
        }
    }

    @FXML
    public void handleImport() {

        if (selectedFile == null) {
            setError("Please select an Excel file first.");
            return;
        }

        try {
            parsedData = service.parseExcelBonCommande(
                    selectedFile.getAbsolutePath()
            );

            // Load table
            articlesTable.getItems().setAll(parsedData.getItems());

            tableContainer.setVisible(true);
            tableContainer.setManaged(true);

            setSuccess("File parsed successfully. Review and confirm.");

        } catch (Exception e) {
            e.printStackTrace();
            setError("Parsing failed: " + e.getMessage());
        }
    }

    @FXML
    public void handleConfirmImport() {

        if (parsedData == null) {
            setError("Please import a file first.");
            return;
        }

        // Validation: Category and Format cannot be empty
        for (ParsedArticleItem item : articlesTable.getItems()) {
            if (item.getCategory() == null) {
                setError("Validation échouée: Catégorie manquante pour l'article : " + item.getDesignation());
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation");
                alert.setHeaderText("Catégorie manquante");
                alert.setContentText("Veuillez sélectionner une catégorie pour l'article : " + item.getDesignation());
                alert.showAndWait();
                return;
            }
            if (item.getFormat() == null) {
                setError("Validation échouée: Format manquant pour l'article : " + item.getDesignation());
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation");
                alert.setHeaderText("Format manquant");
                alert.setContentText("Veuillez sélectionner un format pour l'article : " + item.getDesignation());
                alert.showAndWait();
                return;
            }
        }

        try {
            service.saveBonCommande(parsedData);

            setSuccess("Import successful!");

            resetUI();

        } catch (Exception e) {
            e.printStackTrace();
            setError("Import failed: " + e.getMessage());
        }
    }

    private void resetUI() {
        selectedFile = null;
        parsedData = null;

        selectedFileLabel.setText("No file selected...");

        articlesTable.getItems().clear();

        tableContainer.setVisible(false);
        tableContainer.setManaged(false);


    }

    private void setError(String msg) {
        statusLabel.setStyle("-fx-text-fill: red;");
        statusLabel.setText(msg);
    }

    private void setSuccess(String msg) {
        statusLabel.setStyle("-fx-text-fill: green;");
        statusLabel.setText(msg);
    }

    private int calculateRowCount(String text) {
        if (text == null || text.isEmpty()) return 1;
        int explicitLines = text.split("\r\n|\r|\n", -1).length;
        int estimatedLines = (int) Math.ceil((double) text.length() / 45.0);
        int lines = Math.max(explicitLines, estimatedLines);
        return Math.min(4, Math.max(1, lines));
    }
}
