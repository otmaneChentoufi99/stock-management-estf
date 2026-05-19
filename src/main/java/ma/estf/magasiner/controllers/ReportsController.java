package ma.estf.magasiner.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ma.estf.magasiner.models.dto.ArticleDto;
import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.models.dto.MovementDto;
import ma.estf.magasiner.models.entity.MovementType;
import ma.estf.magasiner.services.ArticleService;
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

    private final ArticleService articleService = new ArticleService();
    private final MovementService movementService = new MovementService();
    private final ReportExportService exportService = new ReportExportService();

    private List<ArticleDto> allArticles = new ArrayList<>();
    private List<MovementDto> allMovements = new ArrayList<>();

    // Keep track of filtered data ready for export
    private List<ArticleDto> filteredArticlesForExport = new ArrayList<>();
    private List<MovementDto> filteredMovementsForExport = new ArrayList<>();
    private String currentReportType = "";

    @FXML
    public void initialize() {
        // 1. Initialize combos
        reportTypeCombo.setItems(FXCollections.observableArrayList("Inventaire Global", "Mouvements de Stock", "Alertes de Stock Bas"));
        reportTypeCombo.setValue("Inventaire Global");

        exportFormatCombo.setItems(FXCollections.observableArrayList("Excel (.xlsx)", "PDF (.pdf)", "CSV (.csv)"));
        exportFormatCombo.setValue("Excel (.xlsx)");

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
        try {
            allArticles = articleService.getAllArticles();
            allMovements = movementService.getAllMovements();

            // Populate KPIs
            double totalValue = allArticles.stream()
                    .mapToDouble(a -> (a.getQuantityInStock() != null ? a.getQuantityInStock() : 0) * (a.getPrixUnit() != null ? a.getPrixUnit() : 0.0))
                    .sum();
            lblTotalValue.setText(String.format("%,.2f DH", totalValue));

            int totalArticlesQty = allArticles.stream()
                    .mapToInt(a -> a.getQuantityInStock() != null ? a.getQuantityInStock() : 0)
                    .sum();
            lblTotalArticles.setText(String.valueOf(totalArticlesQty));

            long lowStockCount = allArticles.stream()
                    .filter(a -> (a.getQuantityInStock() != null ? a.getQuantityInStock() : 0) < 10)
                    .count();
            lblLowStock.setText(String.valueOf(lowStockCount));

            int totalDamaged = allArticles.stream()
                    .mapToInt(a -> a.getQuantityDamaged() != null ? a.getQuantityDamaged() : 0)
                    .sum();
            lblDamagedCount.setText(String.valueOf(totalDamaged));

            // Load Charts
            loadCategoryPieChart();
            loadStockBarChart();
            loadMovementLineChart();

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Erreur de chargement", "Impossible de charger les données statistiques : " + e.getMessage());
        }
    }

    private void loadCategoryPieChart() {
        categoryPieChart.getData().clear();

        Map<String, Integer> categoryCounts = new HashMap<>();
        for (ArticleDto art : allArticles) {
            int qty = art.getQuantityInStock() != null ? art.getQuantityInStock() : 0;
            if (qty <= 0) continue;

            if (art.getCategories() == null || art.getCategories().isEmpty()) {
                categoryCounts.put("Sans Catégorie", categoryCounts.getOrDefault("Sans Catégorie", 0) + qty);
            } else {
                for (CategoryDto cat : art.getCategories()) {
                    categoryCounts.put(cat.getName(), categoryCounts.getOrDefault(cat.getName(), 0) + qty);
                }
            }
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        categoryCounts.forEach((catName, qty) -> pieData.add(new PieChart.Data(catName + " (" + qty + ")", qty)));
        categoryPieChart.setData(pieData);
    }

    private void loadStockBarChart() {
        stockBarChart.getData().clear();

        // Sort by stock quantity descending and take top 8
        List<ArticleDto> topArticles = allArticles.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getQuantityInStock() != null ? b.getQuantityInStock() : 0,
                        a.getQuantityInStock() != null ? a.getQuantityInStock() : 0
                ))
                .limit(8)
                .collect(Collectors.toList());

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

    private void loadMovementLineChart() {
        movementLineChart.getData().clear();

        // Focus on movements in the last 30 days
        LocalDate limitDate = LocalDate.now().minusDays(30);
        List<MovementDto> recentMovements = allMovements.stream()
                .filter(m -> m.getDate() != null && m.getDate().toLocalDate().isAfter(limitDate.minusDays(1)))
                .collect(Collectors.toList());

        // Group by Date and type category (IN vs OUT)
        // IN categories: IN, RETURN, CORRECTION (if positive, but let's treat CORRECTION as IN for simplicity)
        // OUT categories: OUT, TRANSFER, LOSS, DAMAGE, MAINTENANCE
        Map<String, Integer> dailyIns = new TreeMap<>();
        Map<String, Integer> dailyOuts = new TreeMap<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd");

        // Prepopulate dates to avoid missing days in chart (last 15 days or last 30 days)
        for (int i = 29; i >= 0; i--) {
            String dateLabel = LocalDate.now().minusDays(i).format(dtf);
            dailyIns.put(dateLabel, 0);
            dailyOuts.put(dateLabel, 0);
        }

        for (MovementDto m : recentMovements) {
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
        previewTable.getColumns().clear();
        previewTable.getItems().clear();

        if ("Inventaire Global".equals(currentReportType)) {
            filteredArticlesForExport = articleService.getAllArticles();
            setupArticleTableColumns();
            previewTable.setItems(FXCollections.observableArrayList(filteredArticlesForExport));
        } else if ("Alertes de Stock Bas".equals(currentReportType)) {
            filteredArticlesForExport = articleService.getAllArticles().stream()
                    .filter(a -> (a.getQuantityInStock() != null ? a.getQuantityInStock() : 0) < 10)
                    .collect(Collectors.toList());
            setupArticleTableColumns();
            previewTable.setItems(FXCollections.observableArrayList(filteredArticlesForExport));
        } else if ("Mouvements de Stock".equals(currentReportType)) {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (start == null || end == null) {
                showWarningAlert("Champs requis", "Veuillez sélectionner les dates de début et de fin.");
                return;
            }
            if (start.isAfter(end)) {
                showWarningAlert("Dates invalides", "La date de début doit être antérieure à la date de fin.");
                return;
            }

            filteredMovementsForExport = movementService.getAllMovements().stream()
                    .filter(m -> m.getDate() != null &&
                            !m.getDate().toLocalDate().isBefore(start) &&
                            !m.getDate().toLocalDate().isAfter(end))
                    .sorted((m1, m2) -> m2.getDate().compareTo(m1.getDate())) // Newest first in preview
                    .collect(Collectors.toList());

            setupMovementTableColumns();
            previewTable.setItems(FXCollections.observableArrayList(filteredMovementsForExport));
        }
    }

    private void setupArticleTableColumns() {
        TableColumn<Object, String> colRef = new TableColumn<>("Référence");
        colRef.setCellValueFactory(cellData -> new SimpleStringProperty(((ArticleDto) cellData.getValue()).getReference()));
        colRef.setPrefWidth(100);

        TableColumn<Object, String> colName = new TableColumn<>("Designation");
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(((ArticleDto) cellData.getValue()).getName()));
        colName.setPrefWidth(220);

        TableColumn<Object, String> colCats = new TableColumn<>("Catégories");
        colCats.setCellValueFactory(cellData -> {
            Set<CategoryDto> cats = ((ArticleDto) cellData.getValue()).getCategories();
            String catsStr = (cats != null) ? cats.stream().map(CategoryDto::getName).collect(Collectors.joining(", ")) : "-";
            return new SimpleStringProperty(catsStr.isEmpty() ? "-" : catsStr);
        });
        colCats.setPrefWidth(150);

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

        previewTable.getColumns().addAll(Arrays.asList(colRef, colName, colCats, colPrice, colQty, colDamaged, colVal));
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

        previewTable.getColumns().addAll(Arrays.asList(colDate, colType, colArt, colQty, colFrom, colTo, colRef));
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
