package ma.estf.magasiner.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ma.estf.magasiner.models.dto.ArticleDto;
import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.models.dto.MovementDto;
import ma.estf.magasiner.models.entity.MovementType;
import ma.estf.magasiner.services.ArticleService;
import ma.estf.magasiner.services.CategoryService;
import ma.estf.magasiner.services.MovementService;
import ma.estf.magasiner.services.ReportExportService;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportsController {

    // KPI Labels
    @FXML private Label lblTotalValue;
    @FXML private Label lblTotalArticles;
    @FXML private Label lblLowStock;
    @FXML private Label lblDamagedCount;

    // Charts
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> stockBarChart;
    @FXML private CategoryAxis barXAxis;
    @FXML private NumberAxis barYAxis;

    @FXML private LineChart<String, Number> movementLineChart;
    @FXML private CategoryAxis lineXAxis;
    @FXML private NumberAxis lineYAxis;

    // Report Export Controls
    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private VBox dateRangeContainer;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> exportFormatCombo;
    @FXML private TableView<Object> previewTable;
    @FXML private Button btnPreview;
    @FXML private Button btnExport;
    @FXML private ComboBox<String> filterCategoryCombo;
    @FXML private ComboBox<String> filterFormatCombo;

    // Loading indicator overlay
    @FXML private StackPane loadingOverlay;
    @FXML private TabPane mainTabPane;

    private final ArticleService articleService = new ArticleService();
    private final MovementService movementService = new MovementService();
    private final CategoryService categoryService = new CategoryService();
    private final ReportExportService exportService = new ReportExportService();

    // Keep track of filtered data ready for export
    private List<ArticleDto> filteredArticlesForExport = new ArrayList<>();
    private List<MovementDto> filteredMovementsForExport = new ArrayList<>();
    private String currentReportType = "";

    private static class DashboardData {
        final Object[] kpiResults;
        final List<Object[]> categoryDistribution;
        final List<ArticleDto> topArticles;
        final List<MovementDto> recentMovements;

        DashboardData(Object[] kpiResults, List<Object[]> categoryDistribution, List<ArticleDto> topArticles, List<MovementDto> recentMovements) {
            this.kpiResults = kpiResults;
            this.categoryDistribution = categoryDistribution;
            this.topArticles = topArticles;
            this.recentMovements = recentMovements;
        }
    }

    @FXML
    public void initialize() {
        // 1. Initialize combos
        reportTypeCombo.setItems(FXCollections.observableArrayList("Inventaire Global", "Mouvements de Stock", "Alertes de Stock Bas"));
        reportTypeCombo.setValue("Inventaire Global");

        exportFormatCombo.setItems(FXCollections.observableArrayList("Excel (.xlsx)", "PDF (.pdf)", "CSV (.csv)"));
        exportFormatCombo.setValue("Excel (.xlsx)");

        // Initialize Category Filter
        List<CategoryDto> categories = categoryService.findByType("CATEGORY");
        ObservableList<String> catNames = FXCollections.observableArrayList("Toutes");
        if (categories != null) {
            catNames.addAll(categories.stream().map(CategoryDto::getName).collect(Collectors.toList()));
        }
        filterCategoryCombo.setItems(catNames);
        filterCategoryCombo.setValue("Toutes");

        // Initialize Format Filter
        List<CategoryDto> formats = categoryService.findByType("FORMAT");
        ObservableList<String> formatNames = FXCollections.observableArrayList("Tous");
        if (formats != null) {
            formatNames.addAll(formats.stream().map(CategoryDto::getName).collect(Collectors.toList()));
        }
        filterFormatCombo.setItems(formatNames);
        filterFormatCombo.setValue("Tous");

        // Date Pickers defaults to last 30 days
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());

        // Toggle date picker container based on report type
        dateRangeContainer.setDisable(true);
        reportTypeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if ("Mouvements de Stock".equals(newV)) {
                dateRangeContainer.setDisable(false);
            } else {
                dateRangeContainer.setDisable(true);
            }
        });

        // 2. Load Stats and Charts
        refreshDashboardData();
    }

    private void refreshDashboardData() {
        loadingOverlay.setVisible(true);

        Task<DashboardData> loadTask = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                Object[] kpiResults = articleService.getInventoryKpis();
                List<Object[]> categoryDistribution = articleService.getCategoryStockDistribution();
                List<ArticleDto> topArticles = articleService.getTopArticlesByStock(8);

                LocalDateTime limitDate = LocalDateTime.now().minusDays(30).withHour(0).withMinute(0).withSecond(0);
                List<MovementDto> recentMovements = movementService.getRecentMovements(limitDate);

                return new DashboardData(kpiResults, categoryDistribution, topArticles, recentMovements);
            }
        };

        loadTask.setOnSucceeded(event -> {
            DashboardData data = loadTask.getValue();

            // Populate KPIs
            if (data.kpiResults != null) {
                Double totalValue = (Double) data.kpiResults[0];
                lblTotalValue.setText(String.format("%,.2f DH", totalValue != null ? totalValue : 0.0));

                Number totalArticlesQty = (Number) data.kpiResults[1];
                lblTotalArticles.setText(String.valueOf(totalArticlesQty != null ? totalArticlesQty.intValue() : 0));

                Number lowStockCount = (Number) data.kpiResults[2];
                lblLowStock.setText(String.valueOf(lowStockCount != null ? lowStockCount.intValue() : 0));

                Number totalDamaged = (Number) data.kpiResults[3];
                lblDamagedCount.setText(String.valueOf(totalDamaged != null ? totalDamaged.intValue() : 0));
            }

            // Load Charts
            bindCategoryPieChart(data.categoryDistribution);
            bindStockBarChart(data.topArticles);
            bindMovementLineChart(data.recentMovements);

            loadingOverlay.setVisible(false);
        });

        loadTask.setOnFailed(event -> {
            loadingOverlay.setVisible(false);
            Throwable e = loadTask.getException();
            if (e != null) {
                e.printStackTrace();
                showErrorAlert("Erreur de chargement", "Impossible de charger les données statistiques : " + e.getMessage());
            }
        });

        new Thread(loadTask).start();
    }

    private void bindCategoryPieChart(List<Object[]> distribution) {
        categoryPieChart.getData().clear();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (distribution != null) {
            for (Object[] row : distribution) {
                String catName = (String) row[0];
                Number qty = (Number) row[1];
                int val = qty != null ? qty.intValue() : 0;
                pieData.add(new PieChart.Data(catName + " (" + val + ")", val));
            }
        }
        categoryPieChart.setData(pieData);
    }

    private void bindStockBarChart(List<ArticleDto> topArticles) {
        stockBarChart.getData().clear();
        if (topArticles == null) return;

        XYChart.Series<String, Number> seriesAvailable = new XYChart.Series<>();
        seriesAvailable.setName("Disponible");

        XYChart.Series<String, Number> seriesDamaged = new XYChart.Series<>();
        seriesDamaged.setName("Endommagé");

        for (ArticleDto art : topArticles) {
            String nameLabel = art.getName().length() > 15 ? art.getName().substring(0, 12) + "..." : art.getName();
            seriesAvailable.getData().add(new XYChart.Data<>(nameLabel, art.getQuantityInStock() != null ? art.getQuantityInStock() : 0));
            seriesDamaged.getData().add(new XYChart.Data<>(nameLabel, art.getQuantityDamaged() != null ? art.getQuantityDamaged() : 0));
        }

        stockBarChart.getData().addAll(Arrays.asList(seriesAvailable, seriesDamaged));
    }

    private void bindMovementLineChart(List<MovementDto> recentMovements) {
        movementLineChart.getData().clear();
        if (recentMovements == null) return;

        Map<String, Integer> dailyIns = new TreeMap<>();
        Map<String, Integer> dailyOuts = new TreeMap<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd");

        // Prepopulate dates to avoid missing days in chart (last 30 days)
        for (int i = 29; i >= 0; i--) {
            String dateLabel = LocalDate.now().minusDays(i).format(dtf);
            dailyIns.put(dateLabel, 0);
            dailyOuts.put(dateLabel, 0);
        }

        for (MovementDto m : recentMovements) {
            if (m.getDate() == null) continue;
            String dateLabel = m.getDate().format(dtf);
            if (!dailyIns.containsKey(dateLabel)) continue; // outside the 30-day window

            MovementType type = m.getType();
            if (type == MovementType.IN || type == MovementType.RETURN || type == MovementType.CORRECTION) {
                dailyIns.put(dateLabel, dailyIns.get(dateLabel) + m.getQuantity());
            } else {
                dailyOuts.put(dateLabel, dailyOuts.get(dateLabel) + m.getQuantity());
            }
        }

        XYChart.Series<String, Number> seriesIns = new XYChart.Series<>();
        seriesIns.setName("Entrées (IN / Retours)");

        XYChart.Series<String, Number> seriesOuts = new XYChart.Series<>();
        seriesOuts.setName("Sorties (OUT / Pertes / Dommages)");

        dailyIns.forEach((date, qty) -> seriesIns.getData().add(new XYChart.Data<>(date, qty)));
        dailyOuts.forEach((date, qty) -> seriesOuts.getData().add(new XYChart.Data<>(date, qty)));

        movementLineChart.getData().addAll(Arrays.asList(seriesIns, seriesOuts));
    }

    // ==========================================
    // PREVIEW AND EXPORT LOGIC
    // ==========================================

    @FXML
    public void handleFilterPreview() {
        currentReportType = reportTypeCombo.getValue();
        String selectedCategory = filterCategoryCombo.getValue();
        String selectedFormat = filterFormatCombo.getValue();
        previewTable.getColumns().clear();
        previewTable.getItems().clear();

        loadingOverlay.setVisible(true);

        Task<List<?>> filterTask = new Task<>() {
            @Override
            protected List<?> call() throws Exception {
                if ("Inventaire Global".equals(currentReportType)) {
                    filteredArticlesForExport = articleService.getAllArticles();
                    if (selectedCategory != null && !"Toutes".equals(selectedCategory)) {
                        filteredArticlesForExport = filteredArticlesForExport.stream()
                                .filter(a -> a.getCategories() != null && a.getCategories().stream().anyMatch(c -> "CATEGORY".equals(c.getType()) && c.getName().equals(selectedCategory)))
                                .collect(Collectors.toList());
                    }
                    if (selectedFormat != null && !"Tous".equals(selectedFormat)) {
                        filteredArticlesForExport = filteredArticlesForExport.stream()
                                .filter(a -> a.getCategories() != null && a.getCategories().stream().anyMatch(c -> "FORMAT".equals(c.getType()) && c.getName().equals(selectedFormat)))
                                .collect(Collectors.toList());
                    }
                    return filteredArticlesForExport;
                } else if ("Alertes de Stock Bas".equals(currentReportType)) {
                    filteredArticlesForExport = articleService.getAllArticles().stream()
                            .filter(a -> (a.getQuantityInStock() != null ? a.getQuantityInStock() : 0) < 10)
                            .collect(Collectors.toList());
                    if (selectedCategory != null && !"Toutes".equals(selectedCategory)) {
                        filteredArticlesForExport = filteredArticlesForExport.stream()
                                .filter(a -> a.getCategories() != null && a.getCategories().stream().anyMatch(c -> "CATEGORY".equals(c.getType()) && c.getName().equals(selectedCategory)))
                                .collect(Collectors.toList());
                    }
                    if (selectedFormat != null && !"Tous".equals(selectedFormat)) {
                        filteredArticlesForExport = filteredArticlesForExport.stream()
                                .filter(a -> a.getCategories() != null && a.getCategories().stream().anyMatch(c -> "FORMAT".equals(c.getType()) && c.getName().equals(selectedFormat)))
                                .collect(Collectors.toList());
                    }
                    return filteredArticlesForExport;
                } else if ("Mouvements de Stock".equals(currentReportType)) {
                    LocalDate start = startDatePicker.getValue();
                    LocalDate end = endDatePicker.getValue();

                    if (start == null || end == null) {
                        throw new IllegalArgumentException("Veuillez sélectionner les dates de début et de fin.");
                    }
                    if (start.isAfter(end)) {
                        throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin.");
                    }

                    filteredMovementsForExport = movementService.getAllMovements().stream()
                            .filter(m -> m.getDate() != null &&
                                    !m.getDate().toLocalDate().isBefore(start) &&
                                    !m.getDate().toLocalDate().isAfter(end))
                            .sorted((m1, m2) -> m2.getDate().compareTo(m1.getDate())) // Newest first in preview
                            .collect(Collectors.toList());
                    if (selectedCategory != null && !"Toutes".equals(selectedCategory)) {
                        filteredMovementsForExport = filteredMovementsForExport.stream()
                                .filter(m -> m.getArticle() != null && m.getArticle().getCategories() != null &&
                                        m.getArticle().getCategories().stream().anyMatch(c -> "CATEGORY".equals(c.getType()) && c.getName().equals(selectedCategory)))
                                .collect(Collectors.toList());
                    }
                    if (selectedFormat != null && !"Tous".equals(selectedFormat)) {
                        filteredMovementsForExport = filteredMovementsForExport.stream()
                                .filter(m -> m.getArticle() != null && m.getArticle().getCategories() != null &&
                                        m.getArticle().getCategories().stream().anyMatch(c -> "FORMAT".equals(c.getType()) && c.getName().equals(selectedFormat)))
                                .collect(Collectors.toList());
                    }
                    return filteredMovementsForExport;
                }
                return Collections.emptyList();
            }
        };

        filterTask.setOnSucceeded(event -> {
            loadingOverlay.setVisible(false);
            if ("Mouvements de Stock".equals(currentReportType)) {
                setupMovementTableColumns();
                previewTable.setItems(FXCollections.observableArrayList(filteredMovementsForExport));
            } else {
                setupArticleTableColumns();
                previewTable.setItems(FXCollections.observableArrayList(filteredArticlesForExport));
            }
        });

        filterTask.setOnFailed(event -> {
            loadingOverlay.setVisible(false);
            Throwable e = filterTask.getException();
            if (e != null) {
                if (e instanceof IllegalArgumentException) {
                    showWarningAlert("Champs requis", e.getMessage());
                } else {
                    e.printStackTrace();
                    showErrorAlert("Erreur de chargement", "Impossible de filtrer les données : " + e.getMessage());
                }
            }
        });

        new Thread(filterTask).start();
    }

    private void setupArticleTableColumns() {
        TableColumn<Object, String> colRef = new TableColumn<>("Référence");
        colRef.setCellValueFactory(cellData -> new SimpleStringProperty(((ArticleDto) cellData.getValue()).getReference()));
        colRef.setPrefWidth(100);

        TableColumn<Object, String> colName = new TableColumn<>("Designation");
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(((ArticleDto) cellData.getValue()).getName()));
        colName.setPrefWidth(220);

        TableColumn<Object, String> colCats = new TableColumn<>("Catégorie");
        colCats.setCellValueFactory(cellData -> {
            Set<CategoryDto> cats = ((ArticleDto) cellData.getValue()).getCategories();
            String catsStr = (cats != null) ? cats.stream()
                    .filter(c -> "CATEGORY".equals(c.getType()))
                    .map(CategoryDto::getName)
                    .collect(Collectors.joining(", ")) : "-";
            return new SimpleStringProperty(catsStr.isEmpty() ? "-" : catsStr);
        });
        colCats.setPrefWidth(130);

        TableColumn<Object, String> colFormats = new TableColumn<>("Format");
        colFormats.setCellValueFactory(cellData -> {
            Set<CategoryDto> cats = ((ArticleDto) cellData.getValue()).getCategories();
            String formatsStr = (cats != null) ? cats.stream()
                    .filter(c -> "FORMAT".equals(c.getType()))
                    .map(CategoryDto::getName)
                    .collect(Collectors.joining(", ")) : "-";
            return new SimpleStringProperty(formatsStr.isEmpty() ? "-" : formatsStr);
        });
        colFormats.setPrefWidth(120);

        TableColumn<Object, String> colPrice = new TableColumn<>("Prix Unit.");
        colPrice.setCellValueFactory(cellData -> {
            Double p = ((ArticleDto) cellData.getValue()).getPrixUnit();
            return new SimpleStringProperty(p != null ? String.format("%.2f DH", p) : "-");
        });
        colPrice.setPrefWidth(100);

        TableColumn<Object, String> colQty = new TableColumn<>("Qté Stock");
        colQty.setCellValueFactory(cellData -> {
            Integer q = ((ArticleDto) cellData.getValue()).getQuantityInStock();
            return new SimpleStringProperty(q != null ? String.valueOf(q) : "0");
        });
        colQty.setPrefWidth(100);

        TableColumn<Object, String> colDamaged = new TableColumn<>("Qté Endom.");
        colDamaged.setCellValueFactory(cellData -> {
            Integer d = ((ArticleDto) cellData.getValue()).getQuantityDamaged();
            return new SimpleStringProperty(d != null ? String.valueOf(d) : "0");
        });
        colDamaged.setPrefWidth(100);

        TableColumn<Object, String> colVal = new TableColumn<>("Valeur Totale");
        colVal.setCellValueFactory(cellData -> {
            ArticleDto art = (ArticleDto) cellData.getValue();
            double val = (art.getQuantityInStock() != null ? art.getQuantityInStock() : 0) * (art.getPrixUnit() != null ? art.getPrixUnit() : 0.0);
            return new SimpleStringProperty(String.format("%.2f DH", val));
        });
        colVal.setPrefWidth(120);

        TableColumn<Object, String> colBcs = new TableColumn<>("Bons de Commande");
        colBcs.setCellValueFactory(cellData -> {
            String bcs = ((ArticleDto) cellData.getValue()).getBonCommandesSummary();
            return new SimpleStringProperty(bcs != null ? bcs : "-");
        });
        colBcs.setPrefWidth(250);

        previewTable.getColumns().addAll(Arrays.asList(colRef, colName, colCats, colFormats, colBcs, colPrice, colQty, colDamaged, colVal));
    }

    private void setupMovementTableColumns() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        TableColumn<Object, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime d = ((MovementDto) cellData.getValue()).getDate();
            return new SimpleStringProperty(d != null ? d.format(dtf) : "-");
        });
        colDate.setPrefWidth(130);

        TableColumn<Object, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(((MovementDto) cellData.getValue()).getType().name()));
        colType.setPrefWidth(90);

        TableColumn<Object, String> colArt = new TableColumn<>("Article");
        colArt.setCellValueFactory(cellData -> {
            ArticleDto a = ((MovementDto) cellData.getValue()).getArticle();
            return new SimpleStringProperty(a != null ? a.getName() : "-");
        });
        colArt.setPrefWidth(200);

        TableColumn<Object, String> colQty = new TableColumn<>("Quantité");
        colQty.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(((MovementDto) cellData.getValue()).getQuantity())));
        colQty.setPrefWidth(90);

        TableColumn<Object, String> colFrom = new TableColumn<>("De");
        colFrom.setCellValueFactory(cellData -> new SimpleStringProperty(((MovementDto) cellData.getValue()).getFromEntity()));
        colFrom.setPrefWidth(120);

        TableColumn<Object, String> colTo = new TableColumn<>("Vers");
        colTo.setCellValueFactory(cellData -> new SimpleStringProperty(((MovementDto) cellData.getValue()).getToEntity()));
        colTo.setPrefWidth(120);

        TableColumn<Object, String> colRef = new TableColumn<>("Réf Document");
        colRef.setCellValueFactory(cellData -> new SimpleStringProperty(((MovementDto) cellData.getValue()).getReference()));
        colRef.setPrefWidth(120);

        TableColumn<Object, String> colEntryDate = new TableColumn<>("Date Entrée");
        colEntryDate.setCellValueFactory(cellData -> {
            ArticleDto a = ((MovementDto) cellData.getValue()).getArticle();
            return new SimpleStringProperty(a != null && a.getBonCommandeDate() != null && !a.getBonCommandeDate().isEmpty() ? a.getBonCommandeDate() : "-");
        });
        colEntryDate.setPrefWidth(120);

        TableColumn<Object, String> colOrderQty = new TableColumn<>("Qté Comm.");
        colOrderQty.setCellValueFactory(cellData -> {
            ArticleDto a = ((MovementDto) cellData.getValue()).getArticle();
            return new SimpleStringProperty(a != null && a.getQuantiteCommandee() != null ? String.valueOf(a.getQuantiteCommandee()) : "0");
        });
        colOrderQty.setPrefWidth(90);

        TableColumn<Object, String> colStockRemaining = new TableColumn<>("Stock Restant");
        colStockRemaining.setCellValueFactory(cellData -> {
            ArticleDto a = ((MovementDto) cellData.getValue()).getArticle();
            return new SimpleStringProperty(a != null && a.getQuantityInStock() != null ? String.valueOf(a.getQuantityInStock()) : "0");
        });
        colStockRemaining.setPrefWidth(100);

        previewTable.getColumns().addAll(Arrays.asList(colDate, colType, colArt, colQty, colFrom, colTo, colRef, colEntryDate, colOrderQty, colStockRemaining));
    }

    @FXML
    public void handleExport() {
        if (currentReportType.isEmpty()) {
            showWarningAlert("Action requise", "Veuillez filtrer et prévisualiser les données avant d'exporter.");
            return;
        }

        boolean isMovement = "Mouvements de Stock".equals(currentReportType);
        if (isMovement && filteredMovementsForExport.isEmpty()) {
            showWarningAlert("Aucune donnée", "Il n'y a pas de données à exporter.");
            return;
        }
        if (!isMovement && filteredArticlesForExport.isEmpty()) {
            showWarningAlert("Aucune donnée", "Il n'y a pas de données à exporter.");
            return;
        }

        String format = exportFormatCombo.getValue();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport");

        // Format extension mapping
        String defaultFileName = currentReportType.toLowerCase().replaceAll(" ", "_") + "_" + System.currentTimeMillis();
        if (format.contains("Excel")) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel (*.xlsx)", "*.xlsx"));
            fileChooser.setInitialFileName(defaultFileName + ".xlsx");
        } else if (format.contains("PDF")) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));
            fileChooser.setInitialFileName(defaultFileName + ".pdf");
        } else {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV (*.csv)", "*.csv"));
            fileChooser.setInitialFileName(defaultFileName + ".csv");
        }

        Stage stage = (Stage) btnExport.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                if (isMovement) {
                    if (format.contains("Excel")) {
                        exportService.exportMovementsToExcel(file, filteredMovementsForExport);
                    } else if (format.contains("PDF")) {
                        exportService.exportMovementsToPDF(file, filteredMovementsForExport);
                    } else {
                        exportService.exportMovementsToCSV(file, filteredMovementsForExport);
                    }
                } else {
                    String title = "Rapport d'Inventaire : " + currentReportType;
                    if (format.contains("Excel")) {
                        exportService.exportArticlesToExcel(file, filteredArticlesForExport, title);
                    } else if (format.contains("PDF")) {
                        exportService.exportArticlesToPDF(file, filteredArticlesForExport, title);
                    } else {
                        exportService.exportArticlesToCSV(file, filteredArticlesForExport);
                    }
                }

                showInfoAlert("Exportation Réussie", "Le rapport a été exporté avec succès dans :\n" + file.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Erreur d'exportation", "Impossible d'exporter le fichier : " + e.getMessage());
            }
        }
    }

    // ==========================================
    // DIALOG ALERTS
    // ==========================================

    private void showWarningAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
