package ma.estf.magasiner.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import ma.estf.magasiner.MainApplication;

public class RootController {
    @FXML private StackPane contentArea;
    public static RootController instance;

    @FXML
    public void initialize() {
        instance = this;
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("/ma/estf/magasiner/views/" + fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    @FXML public void showDashboard() { loadView("Dashboard.fxml"); }
    @FXML public void showImportCSV() { loadView("ImportCSV.fxml"); }
    @FXML public void showBonCommandeList() { loadView("BonCommandeList.fxml"); }
    @FXML public void showCartMaterial() { loadView("CartMaterial.fxml"); }
    @FXML public void showCartConsumable() { loadView("CartConsumable.fxml"); }
    @FXML public void showAffectationList() { loadView("AffectationList.fxml"); }
    @FXML public void showMovements() { loadView("MovementHistory.fxml"); }
    @FXML public void showHR() { loadView("HR.fxml"); }
    @FXML public void showCategories() { loadView("Category.fxml"); }

    public void showAffectationManage(ma.estf.magasiner.models.dto.AffectationDto aff) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("/ma/estf/magasiner/views/AffectationManage.fxml"));
            Node node = loader.load();
            AffectationManageController controller = loader.getController();
            controller.setAffectation(aff);
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
