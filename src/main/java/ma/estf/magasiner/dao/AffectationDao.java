package ma.estf.magasiner.dao;
import ma.estf.magasiner.models.entity.Affectation;

public class AffectationDao extends GenericDaoImpl<Affectation, Long> {
    public AffectationDao() {
        super(Affectation.class);
    }
}
