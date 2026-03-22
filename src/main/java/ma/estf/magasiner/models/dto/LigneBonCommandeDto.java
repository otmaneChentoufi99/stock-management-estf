package ma.estf.magasiner.models.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneBonCommandeDto {
    private Long id;
    private ArticleDto article;
    private int quantiteCommandee;
    private int quantiteLivree;
}
