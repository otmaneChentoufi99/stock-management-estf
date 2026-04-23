package ma.estf.magasiner.dao;

import ma.estf.magasiner.models.entity.AppSequence;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class SequenceDao {

    public String getNextInventoryNumber() {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            AppSequence seq = session.get(AppSequence.class, "MATERIAL_INV");
            if (seq == null) {
                seq = AppSequence.builder().id("MATERIAL_INV").nextValue(140500L).build();
                session.persist(seq);
            }
            long val = seq.getNextValue();
            seq.setNextValue(val + 1);
            session.merge(seq);
            tx.commit();
            return String.valueOf(val);
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
