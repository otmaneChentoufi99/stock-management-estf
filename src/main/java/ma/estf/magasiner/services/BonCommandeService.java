package ma.estf.magasiner.services;

import ma.estf.magasiner.models.dto.BonCommandeDto;
import java.util.List;
import ma.estf.magasiner.dao.ArticleDao;
import ma.estf.magasiner.dao.BonCommandeDao;
import ma.estf.magasiner.dao.SequenceDao;
import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.entity.BonCommande;
import ma.estf.magasiner.models.entity.LigneBonCommande;

import java.io.FileInputStream;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ma.estf.magasiner.models.dto.ParsedArticleItem;
import ma.estf.magasiner.models.dto.ParsedBonCommande;

import java.time.LocalDate;


public class BonCommandeService {
    private final BonCommandeDao bonCommandeDao = new BonCommandeDao();
    private final ArticleDao articleDao = new ArticleDao();

    public List<BonCommandeDto> getAllBonCommandes() {
        return bonCommandeDao.findAll().stream().map(ma.estf.magasiner.models.mapper.BonCommandeMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public ParsedBonCommande parseExcelBonCommande(String filePath, String type) throws Exception {

        List<ParsedArticleItem> items = new ArrayList<>();

        String numero = null;
        String fournisseur = null;

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            boolean inTable = false;
            int qteColIndex = -1;
            int designationColIndex = -1;
            int priceColIndex = -1;

            for (Row row : sheet) {

                // 🔍 Scan all cells in row (metadata + header detection)
                for (Cell cell : row) {

                    if (cell.getCellType() == CellType.STRING) {

                        String raw = cell.getStringCellValue();
                        String val = raw.trim().toUpperCase();

                        // ========================
                        // 🔍 Extract NUMERO BC
                        // ========================
                        if (numero == null && (val.contains("BON DE COMMANDE") )) {
                            numero = extractNumeroBC(row);
                        }

                        // ========================
                        // 🔍 Extract FOURNISSEUR
                        // ========================
                        if (fournisseur == null && val.contains("FOURNISSEUR")) {
                            fournisseur = extractFournisseur(row);
                        }

                        // ========================
                        // 📊 Detect TABLE HEADER
                        // ========================
                        if (!inTable) {
                            if (val.contains("DESIGNATION") || val.contains("DÉSIGNATION")) {
                                designationColIndex = cell.getColumnIndex();
                            }
                            if (val.contains("QTE") || val.contains("QTÉ")) {
                                qteColIndex = cell.getColumnIndex();
                            }
                            // Check for price column using multiple possible patterns
                            if (val.contains("PRIX.U HT") ) {
                                    priceColIndex = cell.getColumnIndex();
                            }
                        }
                    }
                }

                if (!inTable && designationColIndex != -1 && qteColIndex != -1) {
                    inTable = true;
                    continue; // Skip the header row itself
                }

                // ⛔ Skip rows until table starts (including the header row itself)
                if (!inTable || (designationColIndex != -1 && row.getRowNum() == sheet.getRow(row.getRowNum()).getRowNum() && isHeaderRow(row, designationColIndex))) continue;

                // ========================
                // 📦 Parse ITEMS
                // ========================
                Cell desigCell = row.getCell(designationColIndex);
                Cell qteCell = row.getCell(qteColIndex);
                Cell priceCell = priceColIndex != -1 ? row.getCell(priceColIndex) : null;

                String designation = getStringCellValue(desigCell);

                if (designation == null || designation.isEmpty()) continue;

                String upper = designation.toUpperCase();

                // Stop conditions
                if (upper.contains("TOTAL") || upper.contains("ARRETE LE PRESENT")) break;

                int quantity = getNumericCellValue(qteCell);

                if (quantity > 0) {
                    boolean needsInventoryNumber = "MATERIEL".equalsIgnoreCase(type);
                    ParsedArticleItem item = new ParsedArticleItem(designation, quantity, needsInventoryNumber);
                    if (priceCell != null) {
                        item.setPrixUnit(getDoubleCellValue(priceCell));
                    }
                    items.add(item);
                }
            }
        }

        if (items.isEmpty()) {
            throw new Exception("Aucune ligne valide trouvée.");
        }

        return ParsedBonCommande.builder()
                .numero(numero)
                .fournisseur(fournisseur)
                .items(items)
                .build();
    }
    private String extractNumeroBC(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.STRING) {
                String text = cell.getStringCellValue().trim().toUpperCase();

                if (text.contains("BON DE COMMANDE") || text.contains("BC")) {

                    // Case 1: "BON DE COMMANDE N°2"
                    String digits = text.replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        return digits;
                    }

                    // Case 2: value in next cells
                    String next = getNextNonEmptyCell(row, cell.getColumnIndex());
                    if (next != null) return next;
                }
            }
        }
        return null;
    }

    private String extractFournisseur(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.STRING) {

                String raw = cell.getStringCellValue().trim();
                String upper = raw.toUpperCase();

                if (upper.contains("FOURNISSEUR")) {

                    // Case 1: "Fournisseur : SMART LEVEL"
                    if (raw.contains(":")) {
                        String[] parts = raw.split(":");
                        if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                            return parts[1].trim();
                        }
                    }

                    // Case 2: value in next cells
                    String next = getNextNonEmptyCell(row, cell.getColumnIndex());
                    if (next != null) return next;
                }
            }
        }
        return null;
    }
    private String getNextNonEmptyCell(Row row, int startIndex) {
        for (int i = startIndex + 1; i < startIndex + 6; i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                String val = cell.toString().trim();
                if (!val.isEmpty()) {
                    return val;
                }
            }
        }
        return null;
    }
    private boolean isHeaderRow(Row row, int designationColIndex) {
        Cell cell = row.getCell(designationColIndex);
        if (cell == null) return false;
        String val = cell.toString().toUpperCase();
        return val.contains("DESIGNATION") || val.contains("DÉSIGNATION");
    }

    private String getNextCellValue(Row row, int index) {
        for (int i = index + 1; i <= index + 3; i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                String val = cell.toString().trim();
                if (!val.isEmpty()) return val;
            }
        }
        return null;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> null;
        };
    }


    private double getDoubleCellValue(Cell cell) {
        if (cell == null) return 0.0;
        
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        return switch (type) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    // Remove common thousands separators (space, comma in English, apostrophe)
                    // and convert French decimal comma to dot
                    String val = cell.getStringCellValue().trim()
                        .replace(" ", "")
                        .replace("'", "")
                        .replace(",", ".");
                    
                    // Keep only numbers and the last dot
                    // If multiple dots, only keep the last one as decimal
                    int lastDot = val.lastIndexOf('.');
                    if (lastDot != -1) {
                        String whole = val.substring(0, lastDot).replaceAll("[^0-9]", "");
                        String decimal = val.substring(lastDot + 1).replaceAll("[^0-9]", "");
                        val = whole + "." + decimal;
                    } else {
                        val = val.replaceAll("[^0-9]", "");
                    }
                    
                    yield val.isEmpty() ? 0.0 : Double.parseDouble(val);
                } catch (Exception e) {
                    yield 0.0;
                }
            }
            default -> 0.0;
        };
    }

    private int getNumericCellValue(Cell cell) {
        if (cell == null) return 0;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (Exception e) {
                    return 0;
                }
            default:
                return 0;
        }
    }
    public void saveBonCommande(ParsedBonCommande data, String type) throws Exception {

        if (data == null || data.getItems() == null || data.getItems().isEmpty()) {
            throw new Exception("Aucune donnée à enregistrer.");
        }

        String numeroBC = data.getNumero();
        String serviceDemandeur = data.getServiceDemandeur();
        String fournisseur = data.getFournisseur();
        List<ParsedArticleItem> items = data.getItems();

        BonCommande bc = BonCommande.builder()
                .numero(numeroBC)
                .dateBC(LocalDate.now().toString())
                .serviceDemandeur(serviceDemandeur)
                .fournisseur(fournisseur)
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
            }

            Article article = Article.builder()
                    .reference(ref)
                    .name(item.getDesignation())
                    .caracteristique(item.getCaracteristique())
                    .prixUnit(item.getPrixUnit())
                    .quantityInStock(0)
                    .quantityDamaged(0)
                    .totalReceived(item.getQuantity())
                    .type(type)
                    .category(ma.estf.magasiner.models.mapper.CategoryMapper.toEntity(item.getCategory()))
                    .availableInventoryNumbers(invNumbers)
                    .build();

            articleDao.save(article);

            // 🔹 Movement (use fournisseur if available)
            new MovementService().recordMovement(
                    ma.estf.magasiner.models.entity.MovementType.IN,
                    article.getId(),
                    item.getQuantity(),
                    fournisseur != null ? fournisseur : "FOURNISSEUR",
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
