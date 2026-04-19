package ma.estf.magasiner.dao;

import ma.estf.magasiner.models.entity.Movement;
import org.hibernate.Session;
import java.util.List;

public class MovementDao extends GenericDaoImpl<Movement, Long> {
    public MovementDao() {
        super(Movement.class);
    }

    public List<Movement> findByArticle(Long articleId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Movement m where m.article.id = :articleId order by m.date desc", Movement.class)
                    .setParameter("articleId", articleId)
                    .list();
        }
    }
}
