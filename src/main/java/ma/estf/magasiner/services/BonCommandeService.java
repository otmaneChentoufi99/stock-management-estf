package ma.estf.magasiner.services;

import ma.estf.magasiner.models.dto.BonCommandeDto;
import java.util.List;
import ma.estf.magasiner.dao.ArticleDao;
import ma.estf.magasiner.dao.BonCommandeDao;
import ma.estf.magasiner.dao.SequenceDao;
import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.entity.BonCommande;
import ma.estf.magasiner.models.entity.LigneBonCommande;

import java.time.LocalDate;
import java.util.ArrayList;
import ma.estf.magasiner.models.dto.ParsedArticleItem;

public class BonCommandeService {
    private final BonCommandeDao bonCommandeDao = new BonCommandeDao();
    private final ArticleDao articleDao = new ArticleDao();

    public List<BonCommandeDto> getAllBonCommandes() {
        return bonCommandeDao.findAll().stream().map(ma.estf.magasiner.models.mapper.BonCommandeMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ParsedArticleItem> parseExcelBonCommande(String filePath, String type) throws Exception {
        List<ParsedArticleItem> items = new ArrayList<>();

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
                        boolean needsInventoryNumber = "MATERIEL".equals(type);
                        items.add(new ParsedArticleItem(designation, quantity, needsInventoryNumber));
                    }
                }
            }
        }

        if (items.isEmpty()) {
            throw new Exception(
                    "L'importation a échoué : Impossible de trouver les colonnes QTE et DESIGNATION, ou les lignes sont vides.");
        }

        return items;
    }

    public void saveBonCommande(String numeroBC, String serviceDemandeur, String type, List<ParsedArticleItem> items) throws Exception {
        BonCommande bc = BonCommande.builder()
                .numero(numeroBC)
                .dateBC(LocalDate.now().toString())
                .serviceDemandeur(serviceDemandeur)
                .statut("Reçu")
                .lignes(new ArrayList<>())
                .build();

        for (ParsedArticleItem item : items) {
            String ref = "REF-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);

            List<String> invNumbers = new ArrayList<>();
            if (item.isNeedsInventoryNumber() && item.getQuantity() > 0) {
                SequenceDao sequenceDao = new SequenceDao();
                for (int i = 0; i < item.getQuantity(); i++) {
                    invNumbers.add(sequenceDao.getNextInventoryNumber());
                }
                
                // No labels in importation phase anymore
                // new JasperReportService().generateLabelsForImportAsync(invNumbers, item.getDesignation());
            }

            Article article = Article.builder()
                    .reference(ref)
                    .name(item.getDesignation())
                    .quantityInStock(0) // Start with 0
                    .quantityDamaged(0)
                    .totalReceived(item.getQuantity())
                    .type(type)
                    .availableInventoryNumbers(invNumbers)
                    .build();

            articleDao.save(article);
            
            // Record IN movement
            new MovementService().recordMovement(
                ma.estf.magasiner.models.entity.MovementType.IN, 
                article.getId(), 
                item.getQuantity(), 
                "FOURNISSEUR", 
                "STOCK", 
                numeroBC
            );

            LigneBonCommande ligne = LigneBonCommande.builder()
                    .bonCommande(bc)
                    .article(article)
                    .quantiteCommandee(item.getQuantity())
                    .quantiteLivree(item.getQuantity())
                    .build();

            bc.getLignes().add(ligne);
        }

        bonCommandeDao.save(bc);
    }
}
