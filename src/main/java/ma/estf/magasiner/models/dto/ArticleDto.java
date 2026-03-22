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
    private Integer totalReceived;
    
    private String bonCommandeNumero;
    private String bonCommandeService;
    private String bonCommandeDate;
}
