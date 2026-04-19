package ma.estf.magasiner.models.dto;

import lombok.*;
import ma.estf.magasiner.models.entity.MovementType;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovementDto {
    private Long id;
    private MovementType type;
    private ArticleDto article;
    private int quantity;
    private String fromEntity;
    private String toEntity;
    private LocalDateTime date;
    private String reference;
}
