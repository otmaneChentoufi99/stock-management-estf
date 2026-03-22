package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.entity.LigneBonCommande;
import ma.estf.magasiner.models.dto.LigneBonCommandeDto;

public class LigneBonCommandeMapper {
    public static LigneBonCommandeDto toDto(LigneBonCommande entity) {
        if (entity == null) return null;
        return LigneBonCommandeDto.builder()
                .id(entity.getId())
                .article(ArticleMapper.toDto(entity.getArticle()))
                .quantiteCommandee(entity.getQuantiteCommandee())
                .quantiteLivree(entity.getQuantiteLivree())
                .build();
    }

    public static LigneBonCommande toEntity(LigneBonCommandeDto dto) {
        if (dto == null) return null;
        return LigneBonCommande.builder()
                .id(dto.getId())
                .article(ArticleMapper.toEntity(dto.getArticle()))
                .quantiteCommandee(dto.getQuantiteCommandee())
                .quantiteLivree(dto.getQuantiteLivree())
                .build();
    }
}
