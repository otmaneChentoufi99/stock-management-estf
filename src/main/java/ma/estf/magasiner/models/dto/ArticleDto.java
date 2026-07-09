package ma.estf.magasiner.models.dto;

import lombok.*;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleDto {
    private Long id;
    private String reference;
    private String name;
    private String caracteristique;
    private Double prixUnit;
    private Integer quantityInStock;
    private Integer quantityDamaged;
    private Integer totalReceived;

    
    private String bonCommandeNumero;
    private String bonCommandeFournisseur;
    private String bonCommandeDate;
    private String bonCommandesSummary;
    private Integer quantiteCommandee;
    private Set<CategoryDto> categories;
    private java.util.List<String> availableInventoryNumbers;
}
