package ma.estf.magasiner.dao;

import ma.estf.magasiner.models.entity.Article;
import org.hibernate.Session;
import java.util.List;

public class ArticleDao extends GenericDaoImpl<Article, Long> {
    public ArticleDao() {
        super(Article.class);
    }
    
    public List<Article> findAvailableArticles() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Article a where a.quantityInStock > 0", Article.class).list();
        }
    }
}
