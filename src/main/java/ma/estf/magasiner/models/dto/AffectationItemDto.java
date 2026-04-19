package ma.estf.magasiner.models.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationItemDto {
    private Long id;
    private ArticleDto article;
    private int quantity;
    private String inventoryNumber;
    private String condition;
}
