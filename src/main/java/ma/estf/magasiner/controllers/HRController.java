package ma.estf.magasiner.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ma.estf.magasiner.models.dto.DepartmentDto;
import ma.estf.magasiner.services.DepartmentService;

import java.util.List;

public class HRController {
    @FXML private TextField deptNameField;
    @FXML private ListView<String> deptListView;
    
    private final DepartmentService deptService = new DepartmentService();
    private List<DepartmentDto> departments;

    @FXML
    public void initialize() {
        refreshData();
    }

    private void refreshData() {
        departments = deptService.getAllDepartments();
        deptListView.setItems(FXCollections.observableArrayList(
            departments.stream().map(DepartmentDto::getName).toList()
        ));
    }

    @FXML
    public void handleAddDepartment() {
        String name = deptNameField.getText();
        if (name != null && !name.trim().isEmpty()) {
            deptService.createDepartment(DepartmentDto.builder().name(name).build());
            deptNameField.clear();
            refreshData();
        }
    }
}
