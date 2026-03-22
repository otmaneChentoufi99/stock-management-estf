package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.dto.ArticleDto;

public class ArticleMapper {
    public static ArticleDto toDto(Article entity) {
        if (entity == null) return null;
        
        String bcNum = "";
        String bcServ = "";
        String bcDate = "";
        
        if (entity.getLignesBonCommande() != null && !entity.getLignesBonCommande().isEmpty()) {
            ma.estf.magasiner.models.entity.BonCommande bc = entity.getLignesBonCommande().get(0).getBonCommande();
            if (bc != null) {
                bcNum = bc.getNumero();
                bcServ = bc.getServiceDemandeur();
                bcDate = bc.getDateBC();
            }
        }
        
        return ArticleDto.builder()
                .id(entity.getId())
                .reference(entity.getReference())
                .name(entity.getName())
                .quantityInStock(entity.getQuantityInStock())
                .totalReceived(entity.getTotalReceived() == null ? 0 : entity.getTotalReceived())
                .bonCommandeNumero(bcNum)
                .bonCommandeService(bcServ)
                .bonCommandeDate(bcDate)
                .build();
    }

    public static Article toEntity(ArticleDto dto) {
        if (dto == null) return null;
        return Article.builder()
                .id(dto.getId())
                .reference(dto.getReference())
                .name(dto.getName())
                .quantityInStock(dto.getQuantityInStock())
                .totalReceived(dto.getTotalReceived() == null ? 0 : dto.getTotalReceived())
                .build();
    }
}
