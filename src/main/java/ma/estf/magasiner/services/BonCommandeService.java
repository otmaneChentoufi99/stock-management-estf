package ma.estf.magasiner.services;

import ma.estf.magasiner.models.dto.BonCommandeDto;
import java.util.List;
import ma.estf.magasiner.dao.ArticleDao;
import ma.estf.magasiner.dao.BonCommandeDao;
import ma.estf.magasiner.dao.SequenceDao;
import ma.estf.magasiner.dao.HibernateUtil;
import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.entity.BonCommande;
import ma.estf.magasiner.models.entity.LigneBonCommande;
import ma.estf.magasiner.models.entity.Category;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import org.hibernate.Session;
import org.hibernate.Transaction;

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

    public ParsedBonCommande parseExcelBonCommande(String filePath) throws Exception {

        List<ParsedArticleItem> items = new ArrayList<>();

        String numero = null;
        String fournisseur = null;
        String exercice = null;

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = null;
            // Search for sheet named "B.R" or similar, fallback to sheet index 0
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                String name = workbook.getSheetName(i).toUpperCase().trim();
                if (name.equals("B.R") || name.contains("RECEPTION") || name.equals("BR")) {
                    sheet = workbook.getSheetAt(i);
                    break;
                }
            }
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

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
                        // 🔍 Extract NUMERO BC / REFERENCE
                        // ========================
                        if (numero == null && (val.contains("BON DE COMMANDE") || val.contains("BC") || 
                            val.contains("REFERENCE") || val.contains("RÉFERENCE") || 
                            val.contains("RÉFÉRENCE") || val.contains("RÉF"))) {
                            numero = extractNumeroBC(row);
                        }

                        // ========================
                        // 🔍 Extract FOURNISSEUR
                        // ========================
                        if (fournisseur == null && (val.contains("FOURNISSEUR") || 
                            val.contains("DÉNOMINATION") || val.contains("DENOMINATION"))) {
                            fournisseur = extractFournisseur(row);
                        }

                        // ========================
                        // 🔍 Extract EXERCICE
                        // ========================
                        if (exercice == null && (val.contains("EXERCICE") || val.contains("EXRCICE"))) {
                            exercice = extractExercice(row);
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
                            if (val.contains("PRIX.U HT") || val.contains("P.U H.T") || 
                                val.contains("P.U. HT") || val.contains("P.U")) {
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
                    boolean needsInventoryNumber = false;
                    ParsedArticleItem item = new ParsedArticleItem(designation, quantity, needsInventoryNumber);
                    if (priceCell != null) {
                        item.setPrixUnit(getDoubleCellValue(priceCell));
                    }
                    items.add(item);
                }
            }

            // Fallback for exercice extraction if not explicitly found in standard cells
            if (exercice == null) {
                exercice = extractExerciceFallback(sheet, numero);
            }
        }

        if (items.isEmpty()) {
            throw new Exception("Aucune ligne valide trouvée.");
        }

        return ParsedBonCommande.builder()
                .numero(numero)
                .fournisseur(fournisseur)
                .exercice(exercice)
                .items(items)
                .build();
    }

    private String extractNumeroBC(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.STRING) {
                String text = cell.getStringCellValue().trim();
                String upper = text.toUpperCase();

                if (upper.contains("BON DE COMMANDE") || upper.contains("BC") || 
                    upper.contains("RÉFERENCE") || upper.contains("REFERENCE") || 
                    upper.contains("RÉFÉRENCE") || upper.contains("RÉF")) {

                    // Try to find a pattern like "N°", "N:", "N ", "Réference", etc.
                    int index = upper.indexOf("N°");
                    if (index == -1) index = upper.indexOf("N :");
                    if (index == -1) index = upper.indexOf("N:");
                    if (index == -1) index = upper.indexOf("RÉFERENCE");
                    if (index == -1) index = upper.indexOf("REFERENCE");
                    if (index == -1) index = upper.indexOf("RÉFÉRENCE");
                    if (index == -1) index = upper.indexOf("RÉF");

                    if (index != -1) {
                        int colonIndex = text.indexOf(":", index);
                        int startOffset = (colonIndex != -1) ? colonIndex + 1 : index + 2;
                        
                        if (startOffset < text.length()) {
                            String num = text.substring(startOffset).trim();
                            // remove leading separators
                            while (!num.isEmpty() && (num.startsWith(":") || num.startsWith(".") || 
                                   num.startsWith("-") || num.startsWith("°") || num.startsWith(" "))) {
                                num = num.substring(1).trim();
                            }
                            if (!num.isEmpty()) {
                                return num;
                            }
                        }
                    }

                    // Case 1: digits extraction
                    String digits = upper.replaceAll("[^0-9]", "");
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

                if (upper.contains("FOURNISSEUR") || upper.contains("DÉNOMINATION OU IDENTITÉ") || 
                    upper.contains("DENOMINATION OU IDENTITE")) {

                    int colonIndex = raw.indexOf(":");
                    if (colonIndex != -1) {
                        String supplierPart = raw.substring(colonIndex + 1).trim();
                        // remove trailing details like "- Tél : ..." if present
                        int dashIndex = supplierPart.indexOf("-");
                        if (dashIndex != -1) {
                            supplierPart = supplierPart.substring(0, dashIndex).trim();
                        }
                        if (!supplierPart.isEmpty()) {
                            return supplierPart;
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

    private String extractExercice(Row row) {
        for (Cell cell : row) {
            String val = "";
            if (cell.getCellType() == CellType.STRING) {
                val = cell.getStringCellValue().trim();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                val = String.valueOf((int) cell.getNumericCellValue()).trim();
            } else {
                continue;
            }

            String upper = val.toUpperCase();
            if (upper.contains("EXERCICE") || upper.contains("EXRCICE")) {
                // Case 1: "Exercice : 2025" or "Exrcice: 2025"
                if (val.contains(":")) {
                    String[] parts = val.split(":");
                    if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                        String res = parts[1].trim();
                        if (res.endsWith(".0")) {
                            res = res.substring(0, res.length() - 2);
                        }
                        return res;
                    }
                }

                // Case 2: value in next cells
                String next = getNextNonEmptyCell(row, cell.getColumnIndex());
                if (next != null) {
                    if (next.endsWith(".0")) {
                        next = next.substring(0, next.length() - 2);
                    }
                    return next;
                }
            }
        }
        return null;
    }

    private String extractExerciceFallback(Sheet sheet, String numero) {
        if (numero != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20[2-3][0-9])").matcher(numero);
            if (m.find()) {
                return m.group(1);
            }
        }
        for (int r = 0; r < Math.min(25, sheet.getPhysicalNumberOfRows()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell cell : row) {
                String val = cell.toString();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20[2-3][0-9])").matcher(val);
                if (m.find()) {
                    return m.group(1);
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

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        return switch (type) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    yield String.valueOf((long) val);
                } else {
                    yield String.valueOf(val);
                }
            }
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

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        switch (type) {
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
    public void saveBonCommande(ParsedBonCommande data) throws Exception {
        if (data == null || data.getItems() == null || data.getItems().isEmpty()) {
            throw new Exception("Aucune donnée à enregistrer.");
        }

        String numeroBC = data.getNumero();
        if (numeroBC == null || numeroBC.trim().isEmpty()) {
            throw new Exception("Le numéro du bon de commande est introuvable ou vide.");
        }
        if (bonCommandeDao.existsByNumero(numeroBC.trim())) {
            throw new Exception("Le bon de commande N° " + numeroBC + " existe déjà.");
        }
        String serviceDemandeur = data.getServiceDemandeur();
        String fournisseur = data.getFournisseur();
        String exercice = data.getExercice();
        List<ParsedArticleItem> items = data.getItems();

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            BonCommande bc = BonCommande.builder()
                    .numero(numeroBC)
                    .dateBC(LocalDate.now().toString())
                    .serviceDemandeur(serviceDemandeur)
                    .fournisseur(fournisseur)
                    .exercice(exercice)
                    .statut("Reçu")
                    .lignes(new ArrayList<>())
                    .build();

            SequenceDao sequenceDao = new SequenceDao();

            for (ParsedArticleItem item : items) {
                List<String> invNumbers = new ArrayList<>();

                if (item.isNeedsInventoryNumber() && item.getQuantity() > 0) {
                    invNumbers.addAll(sequenceDao.getNextInventoryNumbers(session, item.getQuantity()));
                }

                Article article = Article.builder()
                        .name(item.getDesignation())
                        .caracteristique(item.getCaracteristique())
                        .prixUnit(item.getPrixUnit())
                        .quantityInStock(0)
                        .quantityDamaged(0)
                        .totalReceived(item.getQuantity())
                        .categories(new HashSet<>())
                        .availableInventoryNumbers(invNumbers)
                        .build();

                // Merge categories to session to avoid detached entity warnings
                Set<Category> mappedCategories = new HashSet<>();
                for (Category cat : item.getAllSelectedCategories().stream()
                        .map(ma.estf.magasiner.models.mapper.CategoryMapper::toEntity)
                        .collect(java.util.stream.Collectors.toSet())) {
                    mappedCategories.add(session.merge(cat));
                }
                article.setCategories(mappedCategories);

                session.persist(article);

                // 🔹 Movement (use the overload that accepts session)
                new MovementService().recordMovement(
                        session,
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

            session.persist(bc);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
