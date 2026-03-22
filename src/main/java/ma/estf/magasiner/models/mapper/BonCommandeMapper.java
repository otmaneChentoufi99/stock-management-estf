package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.entity.BonCommande;
import ma.estf.magasiner.models.dto.BonCommandeDto;
import java.util.stream.Collectors;

public class BonCommandeMapper {
    public static BonCommandeDto toDto(BonCommande entity) {
        if (entity == null) return null;
        return BonCommandeDto.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .dateBC(entity.getDateBC())
                .serviceDemandeur(entity.getServiceDemandeur())
                .statut(entity.getStatut())
                .lignes(entity.getLignes() != null ? 
                        entity.getLignes().stream().map(LigneBonCommandeMapper::toDto).collect(Collectors.toList()) : null)
                .build();
    }

    public static BonCommande toEntity(BonCommandeDto dto) {
        if (dto == null) return null;
        return BonCommande.builder()
                .id(dto.getId())
                .numero(dto.getNumero())
                .dateBC(dto.getDateBC())
                .serviceDemandeur(dto.getServiceDemandeur())
                .statut(dto.getStatut())
                .lignes(dto.getLignes() != null ? 
                        dto.getLignes().stream().map(LigneBonCommandeMapper::toEntity).collect(Collectors.toList()) : null)
                .build();
    }
}
