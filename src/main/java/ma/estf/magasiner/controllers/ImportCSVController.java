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

public class ImportCSVController {

    @FXML private ComboBox<String> typeComboBox;
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
    @FXML private TableColumn<ParsedArticleItem, String> colCaracteristique;
    @FXML private TableColumn<ParsedArticleItem, Double> colPrixUnit;

    private File selectedFile;

    private final BonCommandeService service = new BonCommandeService();
    private final CategoryService categoryService = new CategoryService();

    private ParsedBonCommande parsedData;
    private ObservableList<CategoryDto> categories;

    @FXML
    public void initialize() {

        articlesTable.setEditable(true);

        colDesignation.setCellValueFactory(new PropertyValueFactory<>("designation"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        colNeedsInvNum.setCellValueFactory(cellData ->
                cellData.getValue().needsInventoryNumberProperty());
        colNeedsInvNum.setCellFactory(CheckBoxTableCell.forTableColumn(colNeedsInvNum));

        categories = FXCollections.observableArrayList(categoryService.findAll());

        colCategory.setCellValueFactory(cellData ->
                cellData.getValue().categoryProperty());

        colCategory.setCellFactory(ComboBoxTableCell.forTableColumn(categories));

        colCaracteristique.setCellValueFactory(cellData ->
                cellData.getValue().caracteristiqueProperty());

        // Robust Custom Cell Factory to commit on focus loss without requiring ENTER
        colCaracteristique.setCellFactory(column -> new TableCell<>() {
            private TextField textField;

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
                        if (textField != null) {
                            textField.setText(getString());
                        }
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(getString());
                        setGraphic(null);
                    }
                }
            }

            private void createTextField() {
                textField = new TextField(getString());
                textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
                textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) {
                        commitEdit(textField.getText());
                    }
                });
                textField.setOnAction(event -> commitEdit(textField.getText()));
                textField.setOnKeyPressed(event -> {
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
                    getTableRow().getItem().setCaracteristique(newValue);
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
                    if (colIndex == articlesTable.getColumns().indexOf(colCaracteristique)) {
                        articlesTable.edit(pos.getRow(), colCaracteristique);
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

        selectedFile = fileChooser.showOpenDialog(typeComboBox.getScene().getWindow());

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

        String type = typeComboBox.getValue();

        if (type == null || type.isEmpty()) {
            setError("Type de Bon Commande is required.");
            return;
        }

        try {
            parsedData = service.parseExcelBonCommande(
                    selectedFile.getAbsolutePath(),
                    type
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

        String type = typeComboBox.getValue();

        if (type == null || type.isEmpty()) {
            setError("Type is required.");
            return;
        }

        try {
            service.saveBonCommande(parsedData, type);

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

        typeComboBox.getSelectionModel().clearSelection();
    }

    private void setError(String msg) {
        statusLabel.setStyle("-fx-text-fill: red;");
        statusLabel.setText(msg);
    }

    private void setSuccess(String msg) {
        statusLabel.setStyle("-fx-text-fill: green;");
        statusLabel.setText(msg);
    }
}