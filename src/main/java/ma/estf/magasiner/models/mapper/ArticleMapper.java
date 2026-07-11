package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.dto.ArticleDto;
import java.util.stream.Collectors;
import java.util.HashSet;

public class ArticleMapper {
    public static ArticleDto toDto(Article entity) {
        if (entity == null) return null;
        
        String bcNum = "";
        String bcFournisseur = "";
        String bcDate = "";
        String bcSummary = "";
        int qtyCommandee = 0;
        
        if (entity.getLignesBonCommande() != null && !entity.getLignesBonCommande().isEmpty()) {
            bcSummary = entity.getLignesBonCommande().stream()
                .map(ma.estf.magasiner.models.entity.LigneBonCommande::getBonCommande)
                .filter(java.util.Objects::nonNull)
                .map(bc -> bc.getNumero() + " (" + bc.getFournisseur() + ")")
                .distinct()
                .collect(Collectors.joining(", "));

            qtyCommandee = entity.getLignesBonCommande().stream()
                .mapToInt(ma.estf.magasiner.models.entity.LigneBonCommande::getQuantiteCommandee)
                .sum();

            ma.estf.magasiner.models.entity.BonCommande bc = entity.getLignesBonCommande().get(0).getBonCommande();
            if (bc != null) {
                bcNum = bc.getNumero();
                bcFournisseur = bc.getFournisseur();
                bcDate = bc.getDateBC();
            }
        }
        
        return ArticleDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .caracteristique(entity.getCaracteristique())
                .prixUnit(entity.getPrixUnit())
                .quantityInStock(entity.getQuantityInStock())
                .quantityDamaged(entity.getQuantityDamaged())
                .totalReceived(entity.getTotalReceived() == null ? 0 : entity.getTotalReceived())

                .bonCommandeNumero(bcNum)
                .bonCommandeFournisseur(bcFournisseur)
                .bonCommandeDate(bcDate)
                .bonCommandesSummary(bcSummary.isEmpty() ? "-" : bcSummary)
                .quantiteCommandee(qtyCommandee)
                .categories(entity.getCategories() != null ? entity.getCategories().stream().map(CategoryMapper::toDto).collect(Collectors.toSet()) : new HashSet<>())
                .availableInventoryNumbers(entity.getAvailableInventoryNumbers() != null ? new java.util.ArrayList<>(entity.getAvailableInventoryNumbers()) : new java.util.ArrayList<>())
                .build();
    }

    public static Article toEntity(ArticleDto dto) {
        if (dto == null) return null;
        return Article.builder()
                .id(dto.getId())
                .name(dto.getName())
                .caracteristique(dto.getCaracteristique())
                .prixUnit(dto.getPrixUnit())
                .quantityInStock(dto.getQuantityInStock())
                .quantityDamaged(dto.getQuantityDamaged())
                .totalReceived(dto.getTotalReceived() == null ? 0 : dto.getTotalReceived())

                .categories(dto.getCategories() != null ? dto.getCategories().stream().map(CategoryMapper::toEntity).collect(Collectors.toSet()) : new HashSet<>())
                .availableInventoryNumbers(dto.getAvailableInventoryNumbers() != null ? new java.util.ArrayList<>(dto.getAvailableInventoryNumbers()) : new java.util.ArrayList<>())
                .build();
    }
}
