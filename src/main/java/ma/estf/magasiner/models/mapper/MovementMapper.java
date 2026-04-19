package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.entity.Movement;
import ma.estf.magasiner.models.dto.MovementDto;

public class MovementMapper {
    public static MovementDto toDto(Movement entity) {
        if (entity == null) return null;
        return MovementDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .article(ArticleMapper.toDto(entity.getArticle()))
                .quantity(entity.getQuantity())
                .fromEntity(entity.getFromEntity())
                .toEntity(entity.getToEntity())
                .date(entity.getDate())
                .reference(entity.getReference())
                .build();
    }

    public static Movement toEntity(MovementDto dto) {
        if (dto == null) return null;
        return Movement.builder()
                .id(dto.getId())
                .type(dto.getType())
                .article(ArticleMapper.toEntity(dto.getArticle()))
                .quantity(dto.getQuantity())
                .fromEntity(dto.getFromEntity())
                .toEntity(dto.getToEntity())
                .date(dto.getDate())
                .reference(dto.getReference())
                .build();
    }
}
