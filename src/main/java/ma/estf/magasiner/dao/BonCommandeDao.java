package ma.estf.magasiner.dao;
import ma.estf.magasiner.models.entity.BonCommande;

public class BonCommandeDao extends GenericDaoImpl<BonCommande, Long> {
    public BonCommandeDao() {
        super(BonCommande.class);
    }
}
