package ma.estf.magasiner.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ma.estf.magasiner.dao.ArticleDao;
import ma.estf.magasiner.dao.BonCommandeDao;

public class DashboardController {
    @FXML private Label totalArticlesLabel;
    @FXML private Label totalBCLabel;

    private final ArticleDao articleDao = new ArticleDao();
    private final BonCommandeDao bcDao = new BonCommandeDao();

    @FXML
    public void initialize() {
        try {
            int articleCount = articleDao.findAll().size();
            int bcCount = bcDao.findAll().size();
            
            totalArticlesLabel.setText(String.valueOf(articleCount));
            totalBCLabel.setText(String.valueOf(bcCount));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
