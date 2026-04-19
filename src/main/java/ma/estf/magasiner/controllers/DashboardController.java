package ma.estf.magasiner.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import ma.estf.magasiner.dao.ArticleDao;
import ma.estf.magasiner.models.entity.Article;
import java.util.List;

public class DashboardController {
    @FXML private Label totalArticlesLabel;
    @FXML private Label totalDamagedLabel;
    
    @FXML private TableView<Article> inventoryTable;
    @FXML private TableColumn<Article, String> colRef;
    @FXML private TableColumn<Article, String> colName;
    @FXML private TableColumn<Article, String> colType;
    @FXML private TableColumn<Article, Integer> colAvailable;
    @FXML private TableColumn<Article, Integer> colDamaged;

    private final ArticleDao articleDao = new ArticleDao();

    @FXML
    public void initialize() {
        colRef.setCellValueFactory(new PropertyValueFactory<>("reference"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colAvailable.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        colDamaged.setCellValueFactory(new PropertyValueFactory<>("quantityDamaged"));

        refreshData();
    }

    private void refreshData() {
        try {
            List<Article> articles = articleDao.findAll();
            inventoryTable.setItems(FXCollections.observableArrayList(articles));
            
            long totalAvailable = articles.stream().mapToLong(Article::getQuantityInStock).sum();
            long totalDamaged = articles.stream().mapToLong(Article::getQuantityDamaged).sum();
            
            totalArticlesLabel.setText(String.valueOf(totalAvailable));
            totalDamagedLabel.setText(String.valueOf(totalDamaged));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
