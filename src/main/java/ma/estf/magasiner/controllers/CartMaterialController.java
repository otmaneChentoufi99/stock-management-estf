package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import ma.estf.magasiner.models.dto.AffectationDto;
import ma.estf.magasiner.models.dto.AffectationItemDto;
import ma.estf.magasiner.models.dto.ArticleDto;
import ma.estf.magasiner.models.dto.DepartmentDto;
import ma.estf.magasiner.services.AffectationService;
import ma.estf.magasiner.services.ArticleService;
import ma.estf.magasiner.services.DepartmentService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CartMaterialController {

    @FXML private TableView<ArticleDto> stockTable;
    @FXML private TableColumn<ArticleDto, String> colStockName;
    @FXML private TableColumn<ArticleDto, String> colStockService;
    @FXML private TableColumn<ArticleDto, String> colStockBC;
    @FXML private TableColumn<ArticleDto, String> colStockDate;
    @FXML private TableColumn<ArticleDto, Integer> colStockTotal;
    @FXML private TableColumn<ArticleDto, Integer> colStockQty;
    @FXML private TableColumn<ArticleDto, String> colStockInv;
    
    @FXML private TextField searchFilterField;

    @FXML private TableView<AffectationItemDto> cartTable;
    @FXML private TableColumn<AffectationItemDto, String> colCartName;
    @FXML private TableColumn<AffectationItemDto, Integer> colCartQty;
    @FXML private TableColumn<AffectationItemDto, Void> colCartActions;

    @FXML private ComboBox<String> assigneeDeptComboBox;
    @FXML private TextField employeeNameField;
    @FXML private Label statusLabel;

    private final ArticleService articleService = new ArticleService();
    private final DepartmentService deptService = new DepartmentService();
    private final AffectationService affectationService = new AffectationService();

    private ObservableList<AffectationItemDto> cartItems = FXCollections.observableArrayList();
    private ObservableList<ArticleDto> masterStockList = FXCollections.observableArrayList();
    private List<DepartmentDto> departments;
    public static String pendingSearchQuery = "";

    @FXML
    public void initialize() {
        colStockName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStockService.setCellValueFactory(new PropertyValueFactory<>("bonCommandeService"));
        colStockBC.setCellValueFactory(new PropertyValueFactory<>("bonCommandeNumero"));
        colStockDate.setCellValueFactory(new PropertyValueFactory<>("bonCommandeDate"));
        colStockTotal.setCellValueFactory(new PropertyValueFactory<>("totalReceived"));
        colStockQty.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        colStockInv.setCellValueFactory(cellData -> {
            java.util.List<String> invs = cellData.getValue().getAvailableInventoryNumbers();
            return new javafx.beans.property.SimpleStringProperty(invs != null ? String.join(", ", invs) : "");
        });

        colCartName.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getName()));
        colCartQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        colCartActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnSub = new Button("Supprimer");
            private final HBox pane = new HBox(5, btnSub);
            {
                btnSub.setStyle("-fx-base: #e74c3c; -fx-text-fill: white;");
                
                btnSub.setOnAction(event -> {
                    AffectationItemDto item = getTableView().getItems().get(getIndex());
                    cartItems.remove(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        cartTable.setItems(cartItems);
        
        searchFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterStock(newValue);
        });

        refreshData();
        if (!pendingSearchQuery.isEmpty()) {
            searchFilterField.setText(pendingSearchQuery);
            pendingSearchQuery = "";
        }
    }

    private void refreshData() {
        List<ArticleDto> allArticles = articleService.getAllArticles();
        // Keep ONLY MATERIEL types
        masterStockList.setAll(allArticles.stream()
            .filter(a -> "MATERIEL".equals(a.getType()))
            .collect(Collectors.toList()));

        filterStock(searchFilterField.getText());
        
        departments = deptService.getAllDepartments();
        assigneeDeptComboBox.setItems(FXCollections.observableArrayList(
            departments.stream().map(DepartmentDto::getName).toList()
        ));
    }

    private void filterStock(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            stockTable.setItems(masterStockList);
            return;
        }
        String lowerCaseFilter = filter.toLowerCase();
        List<ArticleDto> filtered = masterStockList.stream().filter(article -> {
            if (article.getName() != null && article.getName().toLowerCase().contains(lowerCaseFilter)) return true;
            if (article.getBonCommandeService() != null && article.getBonCommandeService().toLowerCase().contains(lowerCaseFilter)) return true;
            if (article.getBonCommandeNumero() != null && article.getBonCommandeNumero().toLowerCase().contains(lowerCaseFilter)) return true;
            if (article.getBonCommandeDate() != null && article.getBonCommandeDate().toLowerCase().contains(lowerCaseFilter)) return true;
            return false;
        }).toList();
        stockTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void handleAddToCart() {
        ArticleDto selected = stockTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select an article.");
            return;
        }
        
        long countInCart = cartItems.stream()
            .filter(item -> item.getArticle().getId().equals(selected.getId()))
            .mapToLong(AffectationItemDto::getQuantity)
            .sum();
            
        if (countInCart < selected.getQuantityInStock()) {
            AffectationItemDto existingItem = cartItems.stream()
                .filter(item -> item.getArticle().getId().equals(selected.getId()))
                .findFirst()
                .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + 1);
                cartTable.refresh();
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Incremented quantity in cart.");
            } else {
                AffectationItemDto item = AffectationItemDto.builder()
                    .article(selected)
                    .quantity(1)
                    .build();
                cartItems.add(item);
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Added new line to cart.");
            }
        } else {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Cannot exceed available stock.");
        }
    }

    @FXML
    public void handleCheckout() {
        if (cartItems.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Cart is empty.");
            return;
        }
        int sIdx = assigneeDeptComboBox.getSelectionModel().getSelectedIndex();
        if (sIdx < 0) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please select a Department.");
            return;
        }
        
        String empName = employeeNameField.getText();
        if (empName == null || empName.trim().isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please enter an employee name.");
            return;
        }

        DepartmentDto selectedDept = departments.get(sIdx);
        
        AffectationDto affectation = AffectationDto.builder()
            .employeeName(empName)
            .department(selectedDept)
            .items(new ArrayList<>(cartItems))
            .build();
            
        try {
            affectationService.checkoutCart(affectation, true); // true for material
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Checkout successful! Generating documents in background...");
            cartItems.clear();
            employeeNameField.clear();
            refreshData();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Checkout failed: " + e.getMessage());
        }
    }
}
