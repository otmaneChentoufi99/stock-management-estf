package ma.estf.magasiner.models.dto;

import lombok.*;

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
    private String type;
    
    private String bonCommandeNumero;
    private String bonCommandeFournisseur;
    private String bonCommandeDate;
    private CategoryDto category;
    private java.util.List<String> availableInventoryNumbers;
}
