package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.dto.ArticleDto;

public class ArticleMapper {
    public static ArticleDto toDto(Article entity) {
        if (entity == null) return null;
        
        String bcNum = "";
        String bcFournisseur = "";
        String bcDate = "";
        
        if (entity.getLignesBonCommande() != null && !entity.getLignesBonCommande().isEmpty()) {
            ma.estf.magasiner.models.entity.BonCommande bc = entity.getLignesBonCommande().get(0).getBonCommande();
            if (bc != null) {
                bcNum = bc.getNumero();
                bcFournisseur = bc.getFournisseur();
                bcDate = bc.getDateBC();
            }
        }
        
        return ArticleDto.builder()
                .id(entity.getId())
                .reference(entity.getReference())
                .name(entity.getName())
                .quantityInStock(entity.getQuantityInStock())
                .quantityDamaged(entity.getQuantityDamaged())
                .totalReceived(entity.getTotalReceived() == null ? 0 : entity.getTotalReceived())
                .type(entity.getType())
                .bonCommandeNumero(bcNum)
                .bonCommandeFournisseur(bcFournisseur)
                .bonCommandeDate(bcDate)
                .category(CategoryMapper.toDto(entity.getCategory()))
                .availableInventoryNumbers(entity.getAvailableInventoryNumbers() != null ? new java.util.ArrayList<>(entity.getAvailableInventoryNumbers()) : new java.util.ArrayList<>())
                .build();
    }

    public static Article toEntity(ArticleDto dto) {
        if (dto == null) return null;
        return Article.builder()
                .id(dto.getId())
                .reference(dto.getReference())
                .name(dto.getName())
                .quantityInStock(dto.getQuantityInStock())
                .quantityDamaged(dto.getQuantityDamaged())
                .totalReceived(dto.getTotalReceived() == null ? 0 : dto.getTotalReceived())
                .type(dto.getType())
                .category(CategoryMapper.toEntity(dto.getCategory()))
                .availableInventoryNumbers(dto.getAvailableInventoryNumbers() != null ? new java.util.ArrayList<>(dto.getAvailableInventoryNumbers()) : new java.util.ArrayList<>())
                .build();
    }
}
