package ma.estf.magasiner.models.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BonCommandeDto {
    private Long id;
    private String numero;
    private String dateBC;
    private String serviceDemandeur;
    private String statut;
    private List<LigneBonCommandeDto> lignes;
}
