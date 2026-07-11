package ma.estf.magasiner.services;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;

import com.opencsv.CSVWriter;

import ma.estf.magasiner.models.dto.ArticleDto;
import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.models.dto.MovementDto;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReportExportService {

    private final DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ==========================================
    // EXCEL EXPORTS
    // ==========================================

    public void exportArticlesToExcel(File file, List<ArticleDto> articles, String title) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Inventaire");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle borderStyle = createBorderedStyle(workbook);
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.cloneStyleFrom(borderStyle);
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00\" DH\""));

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Metadata Row
            Row metaRow = sheet.createRow(1);
            metaRow.createCell(0).setCellValue("Généré le : " + LocalDateTime.now().format(fileDateFormatter));

            // Headers
            String[] headers = {"Designation", "Caractéristiques", "Catégorie", "Format", "Bons de Commande", "Prix Unit.", "Quantité en Stock", "Quantité Endommagée", "Valeur Totale"};
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 4;
            double totalValue = 0.0;
            int totalStockQty = 0;
            int totalDamagedQty = 0;

            for (ArticleDto art : articles) {
                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0); c0.setCellValue(art.getName()); c0.setCellStyle(borderStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(art.getCaracteristique() != null ? art.getCaracteristique() : "-"); c1.setCellStyle(borderStyle);
                
                String cats = art.getCategories() != null ? art.getCategories().stream().filter(c -> "CATEGORY".equals(c.getType())).map(CategoryDto::getName).collect(Collectors.joining(", ")) : "-";
                Cell c2 = row.createCell(2); c2.setCellValue(cats.isEmpty() ? "-" : cats); c2.setCellStyle(borderStyle);

                String formats = art.getCategories() != null ? art.getCategories().stream().filter(c -> "FORMAT".equals(c.getType())).map(CategoryDto::getName).collect(Collectors.joining(", ")) : "-";
                Cell c3 = row.createCell(3); c3.setCellValue(formats.isEmpty() ? "-" : formats); c3.setCellStyle(borderStyle);

                Cell c4 = row.createCell(4); c4.setCellValue(art.getBonCommandesSummary() != null ? art.getBonCommandesSummary() : "-"); c4.setCellStyle(borderStyle);

                double price = art.getPrixUnit() != null ? art.getPrixUnit() : 0.0;
                Cell c5 = row.createCell(5); c5.setCellValue(price); c5.setCellStyle(currencyStyle);

                int qty = art.getQuantityInStock() != null ? art.getQuantityInStock() : 0;
                Cell c6 = row.createCell(6); c6.setCellValue(qty); c6.setCellStyle(borderStyle);

                int damaged = art.getQuantityDamaged() != null ? art.getQuantityDamaged() : 0;
                Cell c7 = row.createCell(7); c7.setCellValue(damaged); c7.setCellStyle(borderStyle);

                double val = price * qty;
                Cell c8 = row.createCell(8); c8.setCellValue(val); c8.setCellStyle(currencyStyle);

                totalValue += val;
                totalStockQty += qty;
                totalDamagedQty += damaged;
            }

            // Totals Row
            Row totalRow = sheet.createRow(rowIdx);
            CellStyle boldBorder = workbook.createCellStyle();
            boldBorder.cloneStyleFrom(borderStyle);
            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldBorder.setFont(boldFont);

            CellStyle boldCurrency = workbook.createCellStyle();
            boldCurrency.cloneStyleFrom(currencyStyle);
            boldCurrency.setFont(boldFont);

            Cell labelCell = totalRow.createCell(0);
            labelCell.setCellValue("TOTAL");
            labelCell.setCellStyle(boldBorder);
            for (int i = 1; i <= 5; i++) {
                totalRow.createCell(i).setCellStyle(boldBorder); // empty styled cells
            }

            Cell sumQtyCell = totalRow.createCell(6);
            sumQtyCell.setCellValue(totalStockQty);
            sumQtyCell.setCellStyle(boldBorder);

            Cell sumDamagedCell = totalRow.createCell(7);
            sumDamagedCell.setCellValue(totalDamagedQty);
            sumDamagedCell.setCellStyle(boldBorder);

            Cell sumValCell = totalRow.createCell(8);
            sumValCell.setCellValue(totalValue);
            sumValCell.setCellStyle(boldCurrency);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    public void exportMovementsToExcel(File file, List<MovementDto> movements) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Mouvements");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle borderStyle = createBorderedStyle(workbook);

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Rapport Historique des Mouvements");
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Metadata Row
            Row metaRow = sheet.createRow(1);
            metaRow.createCell(0).setCellValue("Généré le : " + LocalDateTime.now().format(fileDateFormatter));

            // Headers
            String[] headers = {"Date", "Type", "Article Designation", "Quantité", "De", "Vers", "Référence Doc", "Date Entrée Système", "Quantité Commandée", "Stock Restant"};
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 4;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (MovementDto m : movements) {
                Row row = sheet.createRow(rowIdx++);
                Cell c0 = row.createCell(0); c0.setCellValue(m.getDate() != null ? m.getDate().format(dtf) : "-"); c0.setCellStyle(borderStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(m.getType() != null ? m.getType().name() : "-"); c1.setCellStyle(borderStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(m.getArticle() != null ? m.getArticle().getName() : "-"); c2.setCellStyle(borderStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(m.getQuantity()); c3.setCellStyle(borderStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(m.getFromEntity() != null ? m.getFromEntity() : "-"); c4.setCellStyle(borderStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(m.getToEntity() != null ? m.getToEntity() : "-"); c5.setCellStyle(borderStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(m.getReference() != null ? m.getReference() : "-"); c6.setCellStyle(borderStyle);
                
                Cell c7 = row.createCell(7); c7.setCellValue(m.getArticle() != null && m.getArticle().getBonCommandeDate() != null && !m.getArticle().getBonCommandeDate().isEmpty() ? m.getArticle().getBonCommandeDate() : "-"); c7.setCellStyle(borderStyle);
                Cell c8 = row.createCell(8); c8.setCellValue(m.getArticle() != null && m.getArticle().getQuantiteCommandee() != null ? m.getArticle().getQuantiteCommandee() : 0); c8.setCellStyle(borderStyle);
                Cell c9 = row.createCell(9); c9.setCellValue(m.getArticle() != null && m.getArticle().getQuantityInStock() != null ? m.getArticle().getQuantityInStock() : 0); c9.setCellStyle(borderStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createBorderedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    // ==========================================
    // CSV EXPORTS
    // ==========================================

    public void exportArticlesToCSV(File file, List<ArticleDto> articles) throws Exception {
        try (Writer writer = new FileWriter(file);
             CSVWriter csvWriter = new CSVWriter(writer)) {
            String[] headers = {"Designation", "Caracteristique", "Categorie", "Format", "Bons de Commande", "Prix Unit", "Quantite Stock", "Quantite Endommage", "Valeur Totale"};
            csvWriter.writeNext(headers);

            for (ArticleDto art : articles) {
                double price = art.getPrixUnit() != null ? art.getPrixUnit() : 0.0;
                int qty = art.getQuantityInStock() != null ? art.getQuantityInStock() : 0;
                String cats = art.getCategories() != null ? art.getCategories().stream().filter(c -> "CATEGORY".equals(c.getType())).map(CategoryDto::getName).collect(Collectors.joining(", ")) : "-";
                String formats = art.getCategories() != null ? art.getCategories().stream().filter(c -> "FORMAT".equals(c.getType())).map(CategoryDto::getName).collect(Collectors.joining(", ")) : "-";
                
                String[] row = {
                        art.getName(),
                        art.getCaracteristique() != null ? art.getCaracteristique() : "-",
                        cats.isEmpty() ? "-" : cats,
                        formats.isEmpty() ? "-" : formats,
                        art.getBonCommandesSummary() != null ? art.getBonCommandesSummary() : "-",
                        String.valueOf(price),
                        String.valueOf(qty),
                        String.valueOf(art.getQuantityDamaged() != null ? art.getQuantityDamaged() : 0),
                        String.valueOf(price * qty)
                };
                csvWriter.writeNext(row);
            }
        }
    }

    public void exportMovementsToCSV(File file, List<MovementDto> movements) throws Exception {
        try (Writer writer = new FileWriter(file);
             CSVWriter csvWriter = new CSVWriter(writer)) {
            String[] headers = {"Date", "Type", "Article Designation", "Quantite", "De", "Vers", "Reference Document", "Date Entree Systeme", "Quantite Commandee", "Stock Restant"};
            csvWriter.writeNext(headers);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (MovementDto m : movements) {
                String[] row = {
                        m.getDate() != null ? m.getDate().format(dtf) : "-",
                        m.getType() != null ? m.getType().name() : "-",
                        m.getArticle() != null ? m.getArticle().getName() : "-",
                        String.valueOf(m.getQuantity()),
                        m.getFromEntity() != null ? m.getFromEntity() : "-",
                        m.getToEntity() != null ? m.getToEntity() : "-",
                        m.getReference() != null ? m.getReference() : "-",
                        m.getArticle() != null && m.getArticle().getBonCommandeDate() != null && !m.getArticle().getBonCommandeDate().isEmpty() ? m.getArticle().getBonCommandeDate() : "-",
                        m.getArticle() != null && m.getArticle().getQuantiteCommandee() != null ? String.valueOf(m.getArticle().getQuantiteCommandee()) : "0",
                        m.getArticle() != null && m.getArticle().getQuantityInStock() != null ? String.valueOf(m.getArticle().getQuantityInStock()) : "0"
                };
                csvWriter.writeNext(row);
            }
        }
    }

    // ==========================================
    // PDF EXPORTS (using OpenPDF)
    // ==========================================

    public void exportArticlesToPDF(File file, List<ArticleDto> articles, String reportTitle) throws Exception {
        Document document = new Document(PageSize.A4.rotate()); // Landscape is better for wide tables
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // 1. Add Header
        addPDFHeader(document, reportTitle);

        // 2. Add Table
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);
        table.setWidths(new float[]{1.8f, 1.6f, 1.3f, 1.3f, 2.0f, 0.8f, 0.8f, 0.8f, 1.0f});

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        Font boldCellFont = new Font(Font.HELVETICA, 9, Font.BOLD);

        // Header cells
        String[] headers = {"Désignation", "Caractéristiques", "Catégorie", "Format", "Bons de Commande", "P. Unit.", "Qté Stock", "Qté Endom.", "Val. Totale"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new java.awt.Color(44, 62, 80)); // Deep Blue
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        double totalValue = 0.0;
        int totalStockQty = 0;
        int totalDamagedQty = 0;
        boolean alt = false;

        for (ArticleDto art : articles) {
            java.awt.Color bg = alt ? new java.awt.Color(245, 247, 248) : java.awt.Color.WHITE;
            alt = !alt;

            double price = art.getPrixUnit() != null ? art.getPrixUnit() : 0.0;
            int qty = art.getQuantityInStock() != null ? art.getQuantityInStock() : 0;
            int damaged = art.getQuantityDamaged() != null ? art.getQuantityDamaged() : 0;
            double val = price * qty;

            totalValue += val;
            totalStockQty += qty;
            totalDamagedQty += damaged;

            table.addCell(createPDFCell(art.getName(), cellFont, bg, Element.ALIGN_LEFT));
            table.addCell(createPDFCell(art.getCaracteristique() != null ? art.getCaracteristique() : "-", cellFont, bg, Element.ALIGN_LEFT));

            String cats = art.getCategories() != null ? art.getCategories().stream().filter(c -> "CATEGORY".equals(c.getType())).map(CategoryDto::getName).collect(Collectors.joining(", ")) : "-";
            table.addCell(createPDFCell(cats.isEmpty() ? "-" : cats, cellFont, bg, Element.ALIGN_LEFT));

            String formats = art.getCategories() != null ? art.getCategories().stream().filter(c -> "FORMAT".equals(c.getType())).map(CategoryDto::getName).collect(Collectors.joining(", ")) : "-";
            table.addCell(createPDFCell(formats.isEmpty() ? "-" : formats, cellFont, bg, Element.ALIGN_LEFT));

            table.addCell(createPDFCell(art.getBonCommandesSummary() != null ? art.getBonCommandesSummary() : "-", cellFont, bg, Element.ALIGN_LEFT));
            table.addCell(createPDFCell(String.format("%.2f DH", price), cellFont, bg, Element.ALIGN_RIGHT));
            table.addCell(createPDFCell(String.valueOf(qty), cellFont, bg, Element.ALIGN_CENTER));
            table.addCell(createPDFCell(String.valueOf(damaged), cellFont, bg, Element.ALIGN_CENTER));
            table.addCell(createPDFCell(String.format("%.2f DH", val), cellFont, bg, Element.ALIGN_RIGHT));
        }

        // Add Totals row
        java.awt.Color totalBg = new java.awt.Color(230, 233, 237);
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL", boldCellFont));
        totalLabel.setColspan(5);
        totalLabel.setBackgroundColor(totalBg);
        totalLabel.setPadding(6);
        table.addCell(totalLabel);

        table.addCell(createPDFCell("", boldCellFont, totalBg, Element.ALIGN_RIGHT));
        table.addCell(createPDFCell(String.valueOf(totalStockQty), boldCellFont, totalBg, Element.ALIGN_CENTER));
        table.addCell(createPDFCell(String.valueOf(totalDamagedQty), boldCellFont, totalBg, Element.ALIGN_CENTER));
        table.addCell(createPDFCell(String.format("%.2f DH", totalValue), boldCellFont, totalBg, Element.ALIGN_RIGHT));

        document.add(table);

        // 3. Add Signature Areas
        addPDFSignatures(document);

        document.close();
    }

    public void exportMovementsToPDF(File file, List<MovementDto> movements) throws Exception {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // 1. Add Header
        addPDFHeader(document, "Rapport Historique des Mouvements");

        // 2. Add Table
        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);
        table.setWidths(new float[]{1.1f, 0.7f, 1.6f, 0.6f, 1.0f, 1.0f, 0.9f, 1.0f, 0.7f, 0.7f});

        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.WHITE);
        Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        String[] headers = {"Date", "Type", "Désignation", "Qté", "Origine", "Destination", "Doc Réf", "Date Entrée", "Qté Comm.", "Stock Rest."};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new java.awt.Color(44, 62, 80));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        boolean alt = false;
        for (MovementDto m : movements) {
            java.awt.Color bg = alt ? new java.awt.Color(245, 247, 248) : java.awt.Color.WHITE;
            alt = !alt;

            table.addCell(createPDFCell(m.getDate() != null ? m.getDate().format(dtf) : "-", cellFont, bg, Element.ALIGN_CENTER));
            table.addCell(createPDFCell(m.getType() != null ? m.getType().name() : "-", cellFont, bg, Element.ALIGN_CENTER));
            table.addCell(createPDFCell(m.getArticle() != null ? m.getArticle().getName() : "-", cellFont, bg, Element.ALIGN_LEFT));
            table.addCell(createPDFCell(String.valueOf(m.getQuantity()), cellFont, bg, Element.ALIGN_CENTER));
            table.addCell(createPDFCell(m.getFromEntity() != null ? m.getFromEntity() : "-", cellFont, bg, Element.ALIGN_LEFT));
            table.addCell(createPDFCell(m.getToEntity() != null ? m.getToEntity() : "-", cellFont, bg, Element.ALIGN_LEFT));
            table.addCell(createPDFCell(m.getReference() != null ? m.getReference() : "-", cellFont, bg, Element.ALIGN_LEFT));
            table.addCell(createPDFCell(m.getArticle() != null && m.getArticle().getBonCommandeDate() != null && !m.getArticle().getBonCommandeDate().isEmpty() ? m.getArticle().getBonCommandeDate() : "-", cellFont, bg, Element.ALIGN_CENTER));
            table.addCell(createPDFCell(m.getArticle() != null && m.getArticle().getQuantiteCommandee() != null ? String.valueOf(m.getArticle().getQuantiteCommandee()) : "0", cellFont, bg, Element.ALIGN_CENTER));
            table.addCell(createPDFCell(m.getArticle() != null && m.getArticle().getQuantityInStock() != null ? String.valueOf(m.getArticle().getQuantityInStock()) : "0", cellFont, bg, Element.ALIGN_CENTER));
        }

        document.add(table);

        // 3. Add Signature Areas
        addPDFSignatures(document);

        document.close();
    }

    private void addPDFHeader(Document document, String titleStr) throws Exception {
        Font universityFont = new Font(Font.HELVETICA, 10, Font.BOLD, new java.awt.Color(44, 62, 80));
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new java.awt.Color(44, 62, 80));
        Font metaFont = new Font(Font.HELVETICA, 9, Font.ITALIC, java.awt.Color.GRAY);

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Left Header: Logo and text
        Paragraph leftInfo = new Paragraph();
        leftInfo.add(new Phrase("UNIVERSITÉ SIDI MOHAMED BEN ABDELLAH\n", universityFont));
        leftInfo.add(new Phrase("ÉCOLE SUPÉRIEURE DE TECHNOLOGIE - FÈS\n", universityFont));
        leftInfo.add(new Phrase("SERVICE DE GESTION DES STOCKS\n", universityFont));
        
        PdfPCell leftCell = new PdfPCell(leftInfo);
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(leftCell);

        // Right Header: Logo image
        java.net.URL logoUrl = getClass().getResource("/ma/estf/magasiner/images/estf-icon.png");
        PdfPCell rightCell;
        if (logoUrl != null) {
            Image img = Image.getInstance(logoUrl);
            img.scaleToFit(80, 80);
            img.setAlignment(Element.ALIGN_RIGHT);
            rightCell = new PdfPCell(img);
        } else {
            rightCell = new PdfPCell(new Phrase("EST-FÈS", universityFont));
        }
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        // Add a line divider
        Paragraph line = new Paragraph();
        line.setSpacingBefore(5);
        line.setSpacingAfter(15);
        line.add(new Phrase("____________________________________________________________________________________________________________", metaFont));
        document.add(line);

        // Title and creation time
        Paragraph title = new Paragraph(titleStr.toUpperCase(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);

        Paragraph generatedDate = new Paragraph("Rapport généré le : " + LocalDateTime.now().format(fileDateFormatter), metaFont);
        generatedDate.setAlignment(Element.ALIGN_CENTER);
        generatedDate.setSpacingAfter(15);
        document.add(generatedDate);
    }

    private PdfPCell createPDFCell(String text, Font font, java.awt.Color bg, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        return cell;
    }

    private void addPDFSignatures(Document document) throws Exception {
        Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, new java.awt.Color(44, 62, 80));
        
        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.setSpacingBefore(30);
        sigTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell cell1 = new PdfPCell(new Phrase("Signature du Magasinier", boldFont));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
        sigTable.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase("Signature du Directeur", boldFont));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        sigTable.addCell(cell2);

        document.add(sigTable);
    }
}
