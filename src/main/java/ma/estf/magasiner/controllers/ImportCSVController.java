package ma.estf.magasiner.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import ma.estf.magasiner.models.dto.ParsedArticleItem;
import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.services.BonCommandeService;
import ma.estf.magasiner.services.CategoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.ComboBoxTableCell;

import java.io.File;
import java.util.List;

public class ImportCSVController {

    @FXML private TextField numeroBCField;
    @FXML private TextField serviceDemandeurField;
    @FXML private javafx.scene.control.ComboBox<String> typeComboBox;
    @FXML private Label selectedFileLabel;
    @FXML private Label statusLabel;
    
    @FXML private VBox tableContainer;
    @FXML private TableView<ParsedArticleItem> articlesTable;
    @FXML private TableColumn<ParsedArticleItem, String> colDesignation;
    @FXML private TableColumn<ParsedArticleItem, Integer> colQuantity;
    @FXML private TableColumn<ParsedArticleItem, Boolean> colNeedsInvNum;
    @FXML private TableColumn<ParsedArticleItem, CategoryDto> colCategory;
    @FXML private Button confirmImportBtn;

    private File selectedFile;
    private final BonCommandeService service = new BonCommandeService();
    private final CategoryService categoryService = new CategoryService();
    private List<ParsedArticleItem> currentItems;
    private ObservableList<CategoryDto> categories;

    @FXML
    public void initialize() {
        articlesTable.setEditable(true);
        colDesignation.setCellValueFactory(new PropertyValueFactory<>("designation"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        
        colNeedsInvNum.setCellValueFactory(cellData -> cellData.getValue().needsInventoryNumberProperty());
        colNeedsInvNum.setCellFactory(CheckBoxTableCell.forTableColumn(colNeedsInvNum));
        
        categories = FXCollections.observableArrayList(categoryService.findAll());
        colCategory.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
        colCategory.setCellFactory(ComboBoxTableCell.forTableColumn(categories));
        
        tableContainer.setVisible(false);
        tableContainer.setManaged(false);
    }

    @FXML
    public void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Bon de Commande Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        
        selectedFile = fileChooser.showOpenDialog(numeroBCField.getScene().getWindow());
        if (selectedFile != null) {
            selectedFileLabel.setText(selectedFile.getName());
            statusLabel.setText("");
            tableContainer.setVisible(false);
            tableContainer.setManaged(false);
        }
    }

    @FXML
    public void handleImport() {
        if (selectedFile == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a CSV/Excel file first.");
            return;
        }
        String numero = numeroBCField.getText();
        String serviceDemandeur = serviceDemandeurField.getText();
        String type = typeComboBox.getValue();
        
        if (numero == null || numero.trim().isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("N° Bon Commande is required.");
            return;
        }

        if (type == null || type.trim().isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Type de Bon Commande is required.");
            return;
        }

        try {
            currentItems = service.parseExcelBonCommande(selectedFile.getAbsolutePath(), type);
            articlesTable.getItems().setAll(currentItems);
            
            tableContainer.setVisible(true);
            tableContainer.setManaged(true);
            
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("File parsed successfully. Please review the items below.");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Parsing failed: " + e.getMessage());
        }
    }

    @FXML
    public void handleConfirmImport() {
        String numero = numeroBCField.getText();
        String serviceDemandeur = serviceDemandeurField.getText();
        String type = typeComboBox.getValue();

        try {
            service.saveBonCommande(numero, serviceDemandeur, type, currentItems);
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Import successful! Data added to database.");
            
            numeroBCField.clear();
            serviceDemandeurField.clear();
            typeComboBox.getSelectionModel().clearSelection();
            selectedFile = null;
            selectedFileLabel.setText("No file selected...");
            tableContainer.setVisible(false);
            tableContainer.setManaged(false);
            articlesTable.getItems().clear();
            currentItems = null;
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Import failed: " + e.getMessage());
        }
    }
}
