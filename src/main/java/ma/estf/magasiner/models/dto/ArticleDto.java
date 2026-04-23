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
    private Integer quantityInStock;
    private Integer quantityDamaged;
    private Integer totalReceived;
    private String type;
    
    private String bonCommandeNumero;
    private String bonCommandeService;
    private String bonCommandeDate;
    private java.util.List<String> availableInventoryNumbers;
}
