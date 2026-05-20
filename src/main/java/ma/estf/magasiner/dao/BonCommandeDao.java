package ma.estf.magasiner.dao;
import ma.estf.magasiner.models.entity.BonCommande;
import org.hibernate.Session;

public class BonCommandeDao extends GenericDaoImpl<BonCommande, Long> {
    public BonCommandeDao() {
        super(BonCommande.class);
    }

    public boolean existsByNumero(String numero) {
        if (numero == null) return false;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createQuery("select count(b) from BonCommande b where b.numero = :numero", Long.class)
                    .setParameter("numero", numero)
                    .uniqueResult();
            return count != null && count > 0;
        }
    }

    public BonCommande findByNumero(String numero) {
        if (numero == null) return null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from BonCommande b where b.numero = :numero", BonCommande.class)
                    .setParameter("numero", numero)
                    .uniqueResult();
        }
    }
}
