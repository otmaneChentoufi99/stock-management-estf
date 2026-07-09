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

    public Object[] getInventoryKpis() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return (Object[]) session.createQuery(
                "select " +
                "sum(coalesce(a.quantityInStock, 0) * coalesce(a.prixUnit, 0.0)), " +
                "sum(coalesce(a.quantityInStock, 0)), " +
                "sum(case when coalesce(a.quantityInStock, 0) < 10 then 1L else 0L end), " +
                "sum(coalesce(a.quantityDamaged, 0)) " +
                "from Article a"
            ).uniqueResult();
        }
    }

    public List<Object[]> getCategoryStockDistribution() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> list = session.createQuery(
                "select c.name, sum(a.quantityInStock) " +
                "from Article a join a.categories c " +
                "where a.quantityInStock > 0 " +
                "group by c.name", Object[].class
            ).list();
            
            Long noCatSum = session.createQuery(
                "select sum(a.quantityInStock) " +
                "from Article a " +
                "where a.quantityInStock > 0 and a.categories is empty", Long.class
            ).uniqueResult();
            
            if (noCatSum != null && noCatSum > 0) {
                List<Object[]> mutableList = new java.util.ArrayList<>(list);
                mutableList.add(new Object[]{"Sans Catégorie", noCatSum.intValue()});
                return mutableList;
            }
            return list;
        }
    }

    public List<Article> findTopArticlesByStock(int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Article a order by coalesce(a.quantityInStock, 0) desc", Article.class)
                    .setMaxResults(limit)
                    .list();
        }
    }
}
