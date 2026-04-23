package ma.estf.magasiner.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import ma.estf.magasiner.models.entity.Affectation;
import ma.estf.magasiner.models.entity.AffectationItem;
import ma.estf.magasiner.models.entity.Article;
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
        InputStream reportStream = getClass().getResourceAsStream("/ma/estf/magasiner/reports/invoice.jrxml");
        if (reportStream == null) {
            throw new Exception("Invoice report template not found.");
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("affectationId", affectation.getId());
        parameters.put("date", affectation.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        parameters.put("department", affectation.getDepartment() != null ? affectation.getDepartment().getName() : "");
        parameters.put("beneficiary", affectation.getEmployeeName());
        parameters.put("category", affectation.getCategory());
        parameters.put("isMaterial", isMaterial);

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
                        invText = invs.get(0) + " à " + invs.get(invs.size() - 1);
                    }
                }
            }
            invoiceItems.add(new InvoiceItem(article.getReference(), article.getName(), totalQty, invText));
        }

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(invoiceItems);
        parameters.put("itemsDataSource", dataSource);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
        
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
            
            // Generate QR Code
            String qrContent = "Inv: " + item.getInventoryNumber() + " | Art: " + item.getArticle().getName();
            InputStream qrStream = generateQRCodeImage(qrContent, 100, 100);
            parameters.put("qrCodeImage", qrStream);

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

    public void generateLabelsForImportAsync(List<String> inventoryNumbers, String designation) {
        CompletableFuture.runAsync(() -> {
            try {
                InputStream reportStream = getClass().getResourceAsStream("/ma/estf/magasiner/reports/label.jrxml");
                if (reportStream == null) {
                    throw new Exception("Label report template not found.");
                }
                JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

                String dateStr = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                
                int labelCount = 0;
                net.sf.jasperreports.engine.JasperPrint mainPrint = null;
                
                for (String inv : inventoryNumbers) {
                    if (inv == null || inv.isEmpty()) continue;

                    Map<String, Object> parameters = new HashMap<>();
                    parameters.put("inventoryNumber", inv);
                    parameters.put("designation", designation);
                    parameters.put("date", dateStr);

                    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
                    
                    if (mainPrint == null) {
                        mainPrint = jasperPrint;
                    } else {
                        mainPrint.addPage(jasperPrint.getPages().get(0));
                    }
                    labelCount++;
                }
                
                if (mainPrint != null) {
                    String labelFilename = "labels_import_" + System.currentTimeMillis() + ".pdf";
                    JasperExportManager.exportReportToPdfFile(mainPrint, labelFilename);
                    System.out.println("Generated " + labelCount + " import labels into file: " + labelFilename);
                    
                    // Open the PDF
                    java.io.File pdfFile = new java.io.File(labelFilename);
                    if (pdfFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                        try {
                            java.awt.Desktop.getDesktop().open(pdfFile);
                        } catch (Exception ex) {
                            System.err.println("Could not open labels PDF: " + ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error generating import labels: " + e.getMessage());
                e.printStackTrace();
            }
        });
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
