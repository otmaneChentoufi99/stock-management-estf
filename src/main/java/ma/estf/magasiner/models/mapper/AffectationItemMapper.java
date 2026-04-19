package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.entity.AffectationItem;
import ma.estf.magasiner.models.dto.AffectationItemDto;

public class AffectationItemMapper {
    public static AffectationItemDto toDto(AffectationItem entity) {
        if (entity == null) return null;
        return AffectationItemDto.builder()
                .id(entity.getId())
                .article(ArticleMapper.toDto(entity.getArticle()))
                .quantity(entity.getQuantity())
                .inventoryNumber(entity.getInventoryNumber())
                .condition(entity.getCondition())
                .build();
    }

    public static AffectationItem toEntity(AffectationItemDto dto) {
        if (dto == null) return null;
        return AffectationItem.builder()
                .id(dto.getId())
                .article(ArticleMapper.toEntity(dto.getArticle()))
                .quantity(dto.getQuantity())
                .inventoryNumber(dto.getInventoryNumber())
                .condition(dto.getCondition())
                .build();
    }
}
