package ma.estf.magasiner.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import ma.estf.magasiner.models.entity.Affectation;
import ma.estf.magasiner.models.entity.AffectationItem;
import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.entity.BonCommande;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class JasperReportService {

    public static class InvoiceItem {
        private String reference;
        private String designation;
        private Integer quantity;
        private String inventoryNumbers;

        public InvoiceItem(String reference, String designation, Integer quantity, String inventoryNumbers) {
            this.reference = reference;
            this.designation = designation;
            this.quantity = quantity;
            this.inventoryNumbers = inventoryNumbers;
        }

        public String getReference() { return reference; }
        public String getDesignation() { return designation; }
        public Integer getQuantity() { return quantity; }
        public String getInventoryNumbers() { return inventoryNumbers; }
    }

    public void generateInvoiceAsync(Affectation affectation) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean isMaterial = "MATERIEL".equals(affectation.getCategory());
                
                // 1. Generate Invoice
                generateInvoice(affectation, isMaterial);

                // 2. Generate Labels for Material
                if (isMaterial) {
                    generateLabels(affectation);
                }
            } catch (Exception e) {
                System.err.println("Error generating JasperReports invoice: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void generateInvoice(Affectation affectation, boolean isMaterial) throws Exception {
        String templatePath = isMaterial ? "/ma/estf/magasiner/reports/material_fiche.jrxml" : "/ma/estf/magasiner/reports/invoice.jrxml";
        InputStream reportStream = getClass().getResourceAsStream(templatePath);
        if (reportStream == null) {
            throw new Exception("Report template not found: " + templatePath);
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(
                "LOGO_PATH",
                getClass()
                        .getResource("/ma/estf/magasiner/images/estf-icon.png")
                        .toString()
        );
        parameters.put("affectationId", affectation.getId());
        parameters.put("date", affectation.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        parameters.put("department", affectation.getDepartment() != null ? affectation.getDepartment().getName() : "");
        parameters.put("beneficiary", affectation.getEmployeeName());
        parameters.put("category", affectation.getCategory());
        parameters.put("isMaterial", isMaterial);

        String allFournisseurs = affectation.getItems().stream()
                .map(AffectationItem::getFournisseur)
                .filter(f -> f != null && !f.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
        parameters.put("fournisseur", allFournisseurs.isEmpty() ? "-" : allFournisseurs);

        List<InvoiceItem> invoiceItems = new ArrayList<>();
        Map<Article, List<AffectationItem>> grouped = affectation.getItems().stream()
                .collect(Collectors.groupingBy(AffectationItem::getArticle, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Article, List<AffectationItem>> entry : grouped.entrySet()) {
            Article article = entry.getKey();
            List<AffectationItem> items = entry.getValue();
            int totalQty = items.stream().mapToInt(AffectationItem::getQuantity).sum();

            String invText = "-";
            if (isMaterial) {
                List<String> invs = items.stream()
                        .map(AffectationItem::getInventoryNumber)
                        .filter(inv -> inv != null && !inv.trim().isEmpty())
                        .sorted()
                        .collect(Collectors.toList());
                if (!invs.isEmpty()) {
                    if (invs.size() == 1) {
                        invText = invs.get(0);
                    } else {
                        invText = "de " + invs.get(0) + " à " + invs.get(invs.size() - 1);
                    }
                }
            }
            invoiceItems.add(new InvoiceItem(article.getReference(), article.getName(), totalQty, invText));
        }

        JRDataSource dataSource;
        if (isMaterial) {
            // Combine all items into one record for a single-page material fiche with clean HTML formatting
            String allDesignations = invoiceItems.stream()
                .map(item -> "• <b>" + item.getDesignation() + "</b> : <i>" + item.getInventoryNumbers() + "</i>")
                .collect(Collectors.joining("<br/>"));
            String allRefs = invoiceItems.stream().map(InvoiceItem::getReference).collect(Collectors.joining(", "));
            int totalQty = invoiceItems.stream().mapToInt(InvoiceItem::getQuantity).sum();
            String allInvs = invoiceItems.stream().map(InvoiceItem::getInventoryNumbers).filter(s -> !"-".equals(s)).collect(Collectors.joining(", "));
            if (allInvs.isEmpty()) allInvs = "-";

            InvoiceItem summary = new InvoiceItem(allRefs, allDesignations, totalQty, allInvs);
            dataSource = new JRBeanCollectionDataSource(Collections.singletonList(summary));
        } else {
            // For consumables, use an empty data source because the table uses itemsDataSource parameter
            dataSource = new JREmptyDataSource();
        }

        parameters.put("itemsDataSource", new JRBeanCollectionDataSource(invoiceItems));

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        
        String filename = "bon_affectation_" + affectation.getId() + ".pdf";
        File pdfFile = new File(filename);
        JasperExportManager.exportReportToPdfFile(jasperPrint, pdfFile.getAbsolutePath());
        
        System.out.println("Invoice saved to " + pdfFile.getAbsolutePath());

        // Attempt to open the generated PDF
        if (java.awt.Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().open(pdfFile);
            } catch (Exception ex) {
                // Ignore if it can't open
            }
        }
    }

    private void generateLabels(Affectation affectation) throws Exception {
        InputStream reportStream = getClass().getResourceAsStream("/ma/estf/magasiner/reports/label.jrxml");
        if (reportStream == null) {
            throw new Exception("Label report template not found.");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        String dateStr = affectation.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        
        int labelCount = 0;
        JasperPrint mainPrint = null;

        for (AffectationItem item : affectation.getItems()) {
            if (item.getInventoryNumber() == null || item.getInventoryNumber().isEmpty() || item.getInventoryNumber().equals("-")) continue;

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("inventoryNumber", item.getInventoryNumber());
            parameters.put("designation", item.getArticle().getName());
            parameters.put("date", dateStr);
            String yearStr = String.valueOf(affectation.getDate().getYear()).substring(2);
            parameters.put("year", yearStr);
            parameters.put("bc", item.getBcNumero() != null ? item.getBcNumero() : "-");
            parameters.put("fournisseur", item.getFournisseur() != null ? item.getFournisseur() : "-");
            
            // Extract Department for Affectation
            String affectationName = affectation.getDepartment() != null ? affectation.getDepartment().getName() : "";
            parameters.put("affectation", affectationName);

            // Generate QR Code
            //  String qrContent = "Inv: " + item.getInventoryNumber() + " | Art: " + item.getArticle().getName();
            //  InputStream qrStream = generateQRCodeImage(qrContent, 100, 100);
            //  parameters.put("qrCodeImage", qrStream);

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
            
            if (mainPrint == null) {
                mainPrint = jasperPrint;
            } else {
                mainPrint.addPage(jasperPrint.getPages().get(0));
            }
            labelCount++;
        }

        if (mainPrint != null) {
            String labelFilename = "labels_affectation_" + affectation.getId() + ".pdf";
            File pdfFile = new File(labelFilename);
            JasperExportManager.exportReportToPdfFile(mainPrint, pdfFile.getAbsolutePath());
            System.out.println("Generated " + labelCount + " labels into: " + labelFilename);

            if (java.awt.Desktop.isDesktopSupported()) {
                try {
                    java.awt.Desktop.getDesktop().open(pdfFile);
                } catch (Exception ex) {
                    // Ignore
                }
            }
        }
    }

    private InputStream generateQRCodeImage(String text, int width, int height) throws Exception {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = barcodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        return new ByteArrayInputStream(os.toByteArray());
    }
}
