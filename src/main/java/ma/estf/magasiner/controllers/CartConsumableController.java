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
import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.services.CategoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CartConsumableController {

    @FXML private TableView<ArticleDto> stockTable;
    @FXML private TableColumn<ArticleDto, String> colStockName;
    @FXML private TableColumn<ArticleDto, String> colStockService;
    @FXML private TableColumn<ArticleDto, String> colStockBC;
    @FXML private TableColumn<ArticleDto, String> colStockDate;
    @FXML private TableColumn<ArticleDto, Integer> colStockTotal;
    @FXML private TableColumn<ArticleDto, Integer> colStockQty;
    @FXML private TableColumn<ArticleDto, String> colStockCategory;
    
    @FXML private TextField searchFilterField;
    @FXML private ComboBox<CategoryDto> categoryFilterComboBox;

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
    private final CategoryService categoryService = new CategoryService();

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
        colStockCategory.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory() != null ? cellData.getValue().getCategory().getName() : ""));

        colCartName.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArticle().getName()));
        colCartQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        colCartActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnAdd = new Button("+");
            private final Button btnSub = new Button("-");
            private final HBox pane = new HBox(5, btnSub, btnAdd);
            {
                btnAdd.setStyle("-fx-base: #2ecc71; -fx-text-fill: white;");
                btnSub.setStyle("-fx-base: #e74c3c; -fx-text-fill: white;");
                
                btnAdd.setOnAction(event -> {
                    AffectationItemDto item = getTableView().getItems().get(getIndex());
                    if (item.getQuantity() < item.getArticle().getQuantityInStock()) {
                        item.setQuantity(item.getQuantity() + 1);
                        getTableView().refresh();
                    }
                });
                
                btnSub.setOnAction(event -> {
                    AffectationItemDto item = getTableView().getItems().get(getIndex());
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                        getTableView().refresh();
                    } else {
                        cartItems.remove(item);
                    }
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
            filterStock(newValue, categoryFilterComboBox.getValue());
        });

        categoryFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            filterStock(searchFilterField.getText(), newValue);
        });

        refreshData();
        if (!pendingSearchQuery.isEmpty()) {
            searchFilterField.setText(pendingSearchQuery);
            pendingSearchQuery = "";
        }
    }

    private void refreshData() {
        List<ArticleDto> allArticles = articleService.getAllArticles();
        // Keep ONLY CONSOMMABLE types
        masterStockList.setAll(allArticles.stream()
            .filter(a -> "CONSOMMABLE".equals(a.getType()))
            .collect(Collectors.toList()));

        List<CategoryDto> categories = categoryService.findAll();
        List<CategoryDto> comboItems = new ArrayList<>();
        comboItems.add(CategoryDto.builder().id(-1L).name("All Categories").build());
        comboItems.addAll(categories);
        categoryFilterComboBox.setItems(FXCollections.observableArrayList(comboItems));
        categoryFilterComboBox.getSelectionModel().selectFirst();

        filterStock(searchFilterField.getText(), categoryFilterComboBox.getValue());
        
        departments = deptService.getAllDepartments();
        assigneeDeptComboBox.setItems(FXCollections.observableArrayList(
            departments.stream().map(DepartmentDto::getName).toList()
        ));
    }

    private void filterStock(String filter, CategoryDto category) {
        boolean noFilter = (filter == null || filter.trim().isEmpty());
        boolean noCategory = (category == null || category.getId() == -1L);

        if (noFilter && noCategory) {
            stockTable.setItems(masterStockList);
            return;
        }

        String lowerCaseFilter = noFilter ? "" : filter.toLowerCase();
        
        List<ArticleDto> filtered = masterStockList.stream().filter(article -> {
            boolean matchText = noFilter;
            if (!noFilter) {
                if (article.getName() != null && article.getName().toLowerCase().contains(lowerCaseFilter)) matchText = true;
                else if (article.getBonCommandeFournisseur() != null && article.getBonCommandeFournisseur().toLowerCase().contains(lowerCaseFilter)) matchText = true;
                else if (article.getBonCommandeNumero() != null && article.getBonCommandeNumero().toLowerCase().contains(lowerCaseFilter)) matchText = true;
                else if (article.getBonCommandeDate() != null && article.getBonCommandeDate().toLowerCase().contains(lowerCaseFilter)) matchText = true;
            }

            boolean matchCategory = noCategory;
            if (!noCategory) {
                if (article.getCategory() != null && article.getCategory().getId().equals(category.getId())) matchCategory = true;
            }

            return matchText && matchCategory;
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
        
        for (AffectationItemDto item : cartItems) {
            if (item.getArticle().getId().equals(selected.getId())) {
                if (item.getQuantity() < selected.getQuantityInStock()) {
                    item.setQuantity(item.getQuantity() + 1);
                    cartTable.refresh();
                    statusLabel.setStyle("-fx-text-fill: green;");
                    statusLabel.setText("Incremented cart quantity.");
                } else {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Cannot exceed available stock.");
                }
                return;
            }
        }
        
        if (selected.getQuantityInStock() > 0) {
            AffectationItemDto item = AffectationItemDto.builder()
                .article(selected)
                .quantity(1)
                .build();
            cartItems.add(item);
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Added to cart.");
        } else {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Out of stock.");
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
            java.io.File invoicePdf = affectationService.checkoutCart(affectation, false); // false for consumable
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Checkout successful! Opening consumable invoice...");
            cartItems.clear();
            employeeNameField.clear();
            refreshData();
            
            if (java.awt.Desktop.isDesktopSupported()) {
                new Thread(() -> {
                    try {
                        java.awt.Desktop.getDesktop().open(invoicePdf);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Checkout failed: " + e.getMessage());
        }
    }
}
