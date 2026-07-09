package ma.estf.magasiner.dao;

import ma.estf.magasiner.models.entity.AppSequence;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import java.util.ArrayList;

public class SequenceDao {

    public String getNextInventoryNumber() {
        List<String> numbers = getNextInventoryNumbers(1);
        return numbers.isEmpty() ? null : numbers.get(0);
    }

    public List<String> getNextInventoryNumbers(Session session, int count) {
        if (count <= 0) return new ArrayList<>();
        AppSequence seq = session.get(AppSequence.class, "MATERIAL_INV");
        if (seq == null) {
            seq = AppSequence.builder().id("MATERIAL_INV").nextValue(140500L).build();
            session.persist(seq);
        }
        long val = seq.getNextValue();
        seq.setNextValue(val + count);
        session.merge(seq);

        List<String> numbers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            numbers.add(String.valueOf(val + i));
        }
        return numbers;
    }

    public List<String> getNextInventoryNumbers(int count) {
        if (count <= 0) return new ArrayList<>();
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            List<String> numbers = getNextInventoryNumbers(session, count);
            tx.commit();
            return numbers;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
