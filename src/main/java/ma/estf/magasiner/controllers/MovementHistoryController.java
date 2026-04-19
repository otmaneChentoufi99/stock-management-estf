package ma.estf.magasiner.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ma.estf.magasiner.models.dto.MovementDto;
import ma.estf.magasiner.services.MovementService;

import java.time.format.DateTimeFormatter;

public class MovementHistoryController {

    @FXML private TableView<MovementDto> movementTable;
    @FXML private TableColumn<MovementDto, String> colDate;
    @FXML private TableColumn<MovementDto, String> colType;
    @FXML private TableColumn<MovementDto, String> colArticle;
    @FXML private TableColumn<MovementDto, Integer> colQty;
    @FXML private TableColumn<MovementDto, String> colFrom;
    @FXML private TableColumn<MovementDto, String> colTo;
    @FXML private TableColumn<MovementDto, String> colRef;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;

    private final MovementService movementService = new MovementService();
    private final ObservableList<MovementDto> masterList = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDate().format(formatter)));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colArticle.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getArticle() != null ? 
                                       cellData.getValue().getArticle().getName() : "Unknown"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colFrom.setCellValueFactory(new PropertyValueFactory<>("fromEntity"));
        colTo.setCellValueFactory(new PropertyValueFactory<>("toEntity"));
        colRef.setCellValueFactory(new PropertyValueFactory<>("reference"));

        typeFilter.setItems(FXCollections.observableArrayList("Tous", "IN", "OUT", "TRANSFER", "RETURN", "LOSS", "DAMAGE", "MAINTENANCE"));
        typeFilter.setValue("Tous");

        FilteredList<MovementDto> filteredData = new FilteredList<>(masterList, p -> true);
        
        searchField.textProperty().addListener((obs, oldV, newV) -> updateFilter(filteredData));
        typeFilter.valueProperty().addListener((obs, oldV, newV) -> updateFilter(filteredData));

        movementTable.setItems(filteredData);
        refreshData();
    }

    private void updateFilter(FilteredList<MovementDto> filteredData) {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String typeText = typeFilter.getValue();

        filteredData.setPredicate(m -> {
            boolean matchesSearch = true;
            if (!searchText.isEmpty()) {
                matchesSearch = (m.getArticle() != null && m.getArticle().getName().toLowerCase().contains(searchText)) ||
                                (m.getFromEntity() != null && m.getFromEntity().toLowerCase().contains(searchText)) ||
                                (m.getToEntity() != null && m.getToEntity().toLowerCase().contains(searchText)) ||
                                (m.getReference() != null && m.getReference().toLowerCase().contains(searchText));
            }

            boolean matchesType = true;
            if (typeText != null && !typeText.equals("Tous")) {
                matchesType = m.getType().name().equals(typeText);
            }

            return matchesSearch && matchesType;
        });
    }

    private void refreshData() {
        masterList.setAll(movementService.getAllMovements());
    }
}
