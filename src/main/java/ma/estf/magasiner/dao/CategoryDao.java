package ma.estf.magasiner.dao;

import ma.estf.magasiner.models.entity.Category;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Optional;

public class CategoryDao extends GenericDaoImpl<Category, Long> {
    public CategoryDao() {
        super(Category.class);
    }

    public Optional<Category> findByName(String name) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Category> query = session.createQuery("FROM Category WHERE name = :name", Category.class);
            query.setParameter("name", name);
            return query.uniqueResultOptional();
        }
    }
}
