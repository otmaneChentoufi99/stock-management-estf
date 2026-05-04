package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.services.CategoryService;

import java.util.List;

public class CategoryController {
    @FXML private TextField categoryNameField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ListView<CategoryDto> categoryListView;
    
    private final CategoryService categoryService = new CategoryService();
    private List<CategoryDto> categories;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList("CATEGORY", "FORMAT"));
        typeComboBox.getSelectionModel().selectFirst();
        refreshData();
    }

    private void refreshData() {
        categories = categoryService.findAll();
        categoryListView.setItems(FXCollections.observableArrayList(categories));
    }

    @FXML
    public void handleAddCategory() {
        String name = categoryNameField.getText();
        String type = typeComboBox.getValue();
        if (name != null && !name.trim().isEmpty() && type != null) {
            categoryService.save(CategoryDto.builder()
                    .name(name)
                    .type(type)
                    .build());
            categoryNameField.clear();
            refreshData();
        }
    }

    @FXML
    public void handleDeleteCategory() {
        CategoryDto selected = categoryListView.getSelectionModel().getSelectedItem();
//        if (selected != null) {
//            categoryService.delete(selected);
//            refreshData();
//        }
    }
}
