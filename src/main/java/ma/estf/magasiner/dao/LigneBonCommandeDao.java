package ma.estf.magasiner.dao;
import ma.estf.magasiner.models.entity.LigneBonCommande;

public class LigneBonCommandeDao extends GenericDaoImpl<LigneBonCommande, Long> {
    public LigneBonCommandeDao() {
        super(LigneBonCommande.class);
    }
}
