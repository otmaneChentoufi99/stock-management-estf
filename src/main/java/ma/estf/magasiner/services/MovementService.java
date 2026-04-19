package ma.estf.magasiner.services;

import ma.estf.magasiner.dao.MovementDao;
import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.entity.Movement;
import ma.estf.magasiner.models.entity.MovementType;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ma.estf.magasiner.dao.HibernateUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import ma.estf.magasiner.models.dto.MovementDto;
import ma.estf.magasiner.models.mapper.MovementMapper;

public class MovementService {
    private final MovementDao movementDao = new MovementDao();

    public void recordMovement(MovementType type, Long articleId, int quantity, String from, String to, String reference) throws Exception {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                recordMovement(session, type, articleId, quantity, from, to, reference);
                tx.commit();
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                throw e;
            }
        }
    }

    public void recordMovement(Session session, MovementType type, Long articleId, int quantity, String from, String to, String reference) throws Exception {
            Article article = session.get(Article.class, articleId);
            if (article == null) throw new Exception("Article not found.");

            // Update stock based on type
            switch (type) {
                case IN:
                case CORRECTION:
                    article.setQuantityInStock(article.getQuantityInStock() + quantity);
                    break;
                case OUT:
                case LOSS:
                    if (article.getQuantityInStock() < quantity) throw new Exception("Insufficient stock.");
                    article.setQuantityInStock(article.getQuantityInStock() - quantity);
                    break;
                case TRANSFER:
                    break;
                case RETURN:
                    article.setQuantityInStock(article.getQuantityInStock() + quantity);
                    break;
                case DAMAGE:
                    if (article.getQuantityInStock() < quantity) throw new Exception("Insufficient usable stock.");
                    article.setQuantityInStock(article.getQuantityInStock() - quantity);
                    article.setQuantityDamaged(article.getQuantityDamaged() + quantity);
                    break;
                case MAINTENANCE:
                    if (article.getQuantityInStock() < quantity) throw new Exception("Insufficient usable stock.");
                    article.setQuantityInStock(article.getQuantityInStock() - quantity);
                    break;
            }

            Movement movement = Movement.builder()
                    .type(type)
                    .article(article)
                    .quantity(quantity)
                    .fromEntity(from)
                    .toEntity(to)
                    .date(LocalDateTime.now())
                    .reference(reference)
                    .build();

            session.merge(article);
            session.persist(movement);
    }

    public List<MovementDto> getAllMovements() {
        return movementDao.findAll().stream()
                .map(MovementMapper::toDto)
                .collect(Collectors.toList());
    }
}
