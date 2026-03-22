package ma.estf.magasiner.services;

import ma.estf.magasiner.models.dto.BonCommandeDto;
import java.util.List;
import ma.estf.magasiner.dao.ArticleDao;
import ma.estf.magasiner.dao.BonCommandeDao;
import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.entity.BonCommande;
import ma.estf.magasiner.models.entity.LigneBonCommande;

import java.time.LocalDate;
import java.util.ArrayList;

public class BonCommandeService {
    private final BonCommandeDao bonCommandeDao = new BonCommandeDao();
    private final ArticleDao articleDao = new ArticleDao();

    public List<BonCommandeDto> getAllBonCommandes() {
        return bonCommandeDao.findAll().stream().map(ma.estf.magasiner.models.mapper.BonCommandeMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public void importExcelAsBonCommande(String filePath, String numeroBC, String serviceDemandeur) throws Exception {
        BonCommande bc = BonCommande.builder()
                .numero(numeroBC)
                .dateBC(LocalDate.now().toString())
                .serviceDemandeur(serviceDemandeur)
                .statut("Reçu")
                .lignes(new ArrayList<>())
                .build();

        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath);
                org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(fis)) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            boolean inTable = false;
            int qteColIndex = -1;
            int designationColIndex = -1;

            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                // Search for the header row
                if (!inTable) {
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                            String val = cell.getStringCellValue().trim().toUpperCase();
                            if (val.contains("DESIGNATION") || val.contains("DÉSIGNATION")) {
                                inTable = true;
                                designationColIndex = cell.getColumnIndex();
                            }
                            if (val.contains("QTE") || val.contains("QTÉ")) {
                                qteColIndex = cell.getColumnIndex();
                            }
                        }
                    }
                    continue;
                }

                if (inTable && designationColIndex != -1 && qteColIndex != -1) {
                    org.apache.poi.ss.usermodel.Cell desigCell = row.getCell(designationColIndex);
                    org.apache.poi.ss.usermodel.Cell qteCell = row.getCell(qteColIndex);

                    String designation = "";
                    if (desigCell != null) {
                        if (desigCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                            designation = desigCell.getStringCellValue().trim();
                        }
                    }

                    if (designation.toUpperCase().contains("TOTAL")
                            || designation.toUpperCase().contains("ARRETE LE PRESENT")) {
                        break;
                    }
                    if (designation.isEmpty()) {
                        continue;
                    }

                    int quantity = 0;
                    if (qteCell != null) {
                        if (qteCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                            quantity = (int) qteCell.getNumericCellValue();
                        } else if (qteCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                            try {
                                quantity = Integer.parseInt(qteCell.getStringCellValue().trim());
                            } catch (Exception e) {
                            }
                        }
                    }

                    if (quantity > 0) {
                        String ref = "REF-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);

                        Article article = Article.builder()
                                .reference(ref)
                                .name(designation)
                                .quantityInStock(quantity)
                                .totalReceived(quantity)
                                .build();

                        articleDao.save(article);

                        LigneBonCommande ligne = LigneBonCommande.builder()
                                .bonCommande(bc)
                                .article(article)
                                .quantiteCommandee(quantity)
                                .quantiteLivree(quantity)
                                .build();

                        bc.getLignes().add(ligne);
                    }
                }
            }
        }

        if (bc.getLignes().isEmpty()) {
            throw new Exception(
                    "L'importation a échoué : Impossible de trouver les colonnes QTE et DESIGNATION, ou les lignes sont vides.");
        }

        bonCommandeDao.save(bc);
    }
}
