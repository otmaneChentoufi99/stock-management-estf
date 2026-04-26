package ma.estf.magasiner.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedBonCommande {
    private String numero;
    private String fournisseur;
    private String exercice;
    private String serviceDemandeur;
    private List<ParsedArticleItem> items;
}
