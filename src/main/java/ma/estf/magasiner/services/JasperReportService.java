package ma.estf.magasiner.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import ma.estf.magasiner.models.entity.Affectation;
import ma.estf.magasiner.models.entity.AffectationItem;
import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.entity.BonCommande;
import ma.estf.magasiner.dao.BonCommandeDao;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

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

    private static final Map<String, JasperReport> compiledReportsCache = new java.util.concurrent.ConcurrentHashMap<>();

    private JasperReport getOrCompileReport(String jrxmlPath) throws Exception {
        JasperReport report = compiledReportsCache.get(jrxmlPath);
        if (report == null) {
            synchronized (compiledReportsCache) {
                report = compiledReportsCache.get(jrxmlPath);
                if (report == null) {
                    try (InputStream reportStream = getClass().getResourceAsStream(jrxmlPath)) {
                        if (reportStream == null) {
                            throw new Exception("Report template not found: " + jrxmlPath);
                        }
                        report = JasperCompileManager.compileReport(reportStream);
                        compiledReportsCache.put(jrxmlPath, report);
                    }
                }
            }
        }
        return report;
    }

    public static class InvoiceItem {
        private String reference;
        private String designation;
        private Integer quantity;
        private String inventoryNumbers;
        private String caracteristique;
        private Double prixUnit;

        public InvoiceItem(String reference, String designation, Integer quantity, String inventoryNumbers, String caracteristique, Double prixUnit) {
            this.reference = reference;
            this.designation = designation;
            this.quantity = quantity;
            this.inventoryNumbers = inventoryNumbers;
            this.caracteristique = caracteristique;
            this.prixUnit = prixUnit;
        }

        public String getReference() { return reference; }
        public String getDesignation() { return designation; }
        public Integer getQuantity() { return quantity; }
        public String getInventoryNumbers() { return inventoryNumbers; }
        public String getCaracteristique() { return caracteristique; }
        public Double getPrixUnit() { return prixUnit; }
    }

    public void generateInvoiceAsync(Affectation affectation) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean isMaterial = "MATERIEL".equals(affectation.getCategory());
                
                // 1. Generate Invoice
                generateInvoice(affectation);

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

    public void generateTransformationReportAsync(Affectation target, String sourceEmployee) {
        CompletableFuture.runAsync(() -> {
            try {
                generateTransformationReport(target, sourceEmployee);
            } catch (Exception e) {
                System.err.println("Error generating transformation report: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public java.io.File generateInvoice(Affectation affectation) throws Exception {
        boolean isMaterial = "MATERIEL".equals(affectation.getCategory());
        String templatePath = isMaterial ? "/ma/estf/magasiner/reports/material_fiche.jrxml" : "/ma/estf/magasiner/reports/invoice.jrxml";
        JasperReport jasperReport = getOrCompileReport(templatePath);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(
                "LOGO_PATH",
                getClass()
                        .getResource("/ma/estf/magasiner/images/estf-icon.jpg")
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

        String allBcNumeros = affectation.getItems().stream()
                .map(AffectationItem::getBcNumero)
                .filter(b -> b != null && !b.trim().isEmpty() && !"-".equals(b.trim()))
                .distinct()
                .collect(Collectors.joining(", "));
        parameters.put("bc", allBcNumeros.isEmpty() ? "-" : allBcNumeros);

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
            invoiceItems.add(new InvoiceItem("", article.getName(), totalQty, invText, article.getCaracteristique(), article.getPrixUnit()));
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

            String allCaracteristiques = invoiceItems.stream()
                .filter(item -> item.getCaracteristique() != null && !item.getCaracteristique().isEmpty())
                .map(item -> "• <b>" + item.getDesignation() + "</b> (" + (item.getPrixUnit() != null ? String.format("%.2f", item.getPrixUnit()) : "-") + " DH) : " + item.getCaracteristique())
                .collect(Collectors.joining("<br/>"));
            if (allCaracteristiques.isEmpty()) allCaracteristiques = "-";

            InvoiceItem summary = new InvoiceItem(allRefs, allDesignations, totalQty, allInvs, allCaracteristiques, 0.0);
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
        return pdfFile;
    }

    public void generateTransformationReport(Affectation target, String sourceEmployee) throws Exception {
        String templatePath = "/ma/estf/magasiner/reports/transformation_fiche.jrxml";
        JasperReport jasperReport = getOrCompileReport(templatePath);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(
                "LOGO_PATH",
                getClass()
                        .getResource("/ma/estf/magasiner/images/estf-icon.jpg")
                        .toString()
        );
        parameters.put("affectationId", target.getId());
        parameters.put("date", target.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        parameters.put("department", target.getDepartment() != null ? target.getDepartment().getName() : "");
        parameters.put("beneficiary", target.getEmployeeName());
        parameters.put("sourceBeneficiary", sourceEmployee);

        String allFournisseurs = target.getItems().stream()
                .map(AffectationItem::getFournisseur)
                .filter(f -> f != null && !f.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
        parameters.put("fournisseur", allFournisseurs.isEmpty() ? "-" : allFournisseurs);

        String allBcNumeros = target.getItems().stream()
                .map(AffectationItem::getBcNumero)
                .filter(b -> b != null && !b.trim().isEmpty() && !"-".equals(b.trim()))
                .distinct()
                .collect(Collectors.joining(", "));
        parameters.put("bc", allBcNumeros.isEmpty() ? "-" : allBcNumeros);

        List<InvoiceItem> invoiceItems = new ArrayList<>();
        Map<Article, List<AffectationItem>> grouped = target.getItems().stream()
                .collect(Collectors.groupingBy(AffectationItem::getArticle, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Article, List<AffectationItem>> entry : grouped.entrySet()) {
            Article article = entry.getKey();
            List<AffectationItem> items = entry.getValue();
            int totalQty = items.stream().mapToInt(AffectationItem::getQuantity).sum();

            List<String> invs = items.stream()
                    .map(AffectationItem::getInventoryNumber)
                    .filter(inv -> inv != null && !inv.trim().isEmpty())
                    .sorted()
                    .collect(Collectors.toList());
            String invText = "-";
            if (!invs.isEmpty()) {
                if (invs.size() == 1) {
                    invText = invs.get(0);
                } else {
                    invText = "de " + invs.get(0) + " à " + invs.get(invs.size() - 1);
                }
            }
            invoiceItems.add(new InvoiceItem("", article.getName(), totalQty, invText, article.getCaracteristique(), article.getPrixUnit()));
        }

        // Combine all items into one record for a single-page material fiche
        String allDesignations = invoiceItems.stream()
            .map(item -> "• <b>" + item.getDesignation() + "</b> : <i>" + item.getInventoryNumbers() + "</i>")
            .collect(Collectors.joining("<br/>"));
        String allRefs = invoiceItems.stream().map(InvoiceItem::getReference).collect(Collectors.joining(", "));
        int totalQty = invoiceItems.stream().mapToInt(InvoiceItem::getQuantity).sum();
        String allInvs = invoiceItems.stream().map(InvoiceItem::getInventoryNumbers).filter(s -> !"-".equals(s)).collect(Collectors.joining(", "));
        if (allInvs.isEmpty()) allInvs = "-";

        String allCaracteristiques = invoiceItems.stream()
            .filter(item -> item.getCaracteristique() != null && !item.getCaracteristique().isEmpty())
            .map(item -> "• <b>" + item.getDesignation() + "</b> (" + (item.getPrixUnit() != null ? String.format("%.2f", item.getPrixUnit()) : "-") + " DH) : " + item.getCaracteristique())
            .collect(Collectors.joining("<br/>"));
        if (allCaracteristiques.isEmpty()) allCaracteristiques = "-";

        InvoiceItem summary = new InvoiceItem(allRefs, allDesignations, totalQty, allInvs, allCaracteristiques, 0.0);
        JRDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(summary));

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        
        String filename = "fiche_transformation_" + target.getId() + ".pdf";
        File pdfFile = new File(filename);
        JasperExportManager.exportReportToPdfFile(jasperPrint, pdfFile.getAbsolutePath());
        
        System.out.println("Transformation report saved to " + pdfFile.getAbsolutePath());

        if (java.awt.Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().open(pdfFile);
            } catch (Exception ex) {
                // Ignore
            }
        }
    }

    public java.io.File generateReturnReport(Affectation affectation, List<AffectationItem> returnedItems) throws Exception {
        String templatePath = "/ma/estf/magasiner/reports/return_fiche.jrxml";
        JasperReport jasperReport = getOrCompileReport(templatePath);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(
                "LOGO_PATH",
                getClass()
                        .getResource("/ma/estf/magasiner/images/estf-icon.jpg")
                        .toString()
        );
        parameters.put("affectationId", affectation.getId());
        parameters.put("date", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        parameters.put("department", affectation.getDepartment() != null ? affectation.getDepartment().getName() : "");
        parameters.put("beneficiary", affectation.getEmployeeName());
        parameters.put("category", affectation.getCategory());
        parameters.put("isMaterial", true);

        String allFournisseurs = returnedItems.stream()
                .map(AffectationItem::getFournisseur)
                .filter(f -> f != null && !f.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
        parameters.put("fournisseur", allFournisseurs.isEmpty() ? "-" : allFournisseurs);

        List<InvoiceItem> invoiceItems = new ArrayList<>();
        Map<Article, List<AffectationItem>> grouped = returnedItems.stream()
                .collect(Collectors.groupingBy(AffectationItem::getArticle, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Article, List<AffectationItem>> entry : grouped.entrySet()) {
            Article article = entry.getKey();
            List<AffectationItem> items = entry.getValue();
            int totalQty = items.stream().mapToInt(AffectationItem::getQuantity).sum();

            List<String> invs = items.stream()
                    .map(AffectationItem::getInventoryNumber)
                    .filter(inv -> inv != null && !inv.trim().isEmpty())
                    .sorted()
                    .collect(Collectors.toList());
            String invText = "-";
            if (!invs.isEmpty()) {
                if (invs.size() == 1) {
                    invText = invs.get(0);
                } else {
                    invText = "de " + invs.get(0) + " à " + invs.get(invs.size() - 1);
                }
            }
            invoiceItems.add(new InvoiceItem("", article.getName(), totalQty, invText, article.getCaracteristique(), article.getPrixUnit()));
        }

        // Combine all items into one record for a single-page material fiche with clean HTML formatting
        String allDesignations = invoiceItems.stream()
            .map(item -> "• <b>" + item.getDesignation() + "</b> : <i>" + item.getInventoryNumbers() + "</i>")
            .collect(Collectors.joining("<br/>"));
        String allRefs = invoiceItems.stream().map(InvoiceItem::getReference).collect(Collectors.joining(", "));
        int totalQty = invoiceItems.stream().mapToInt(InvoiceItem::getQuantity).sum();
        String allInvs = invoiceItems.stream().map(InvoiceItem::getInventoryNumbers).filter(s -> !"-".equals(s)).collect(Collectors.joining(", "));
        if (allInvs.isEmpty()) allInvs = "-";

        String allCaracteristiques = invoiceItems.stream()
            .filter(item -> item.getCaracteristique() != null && !item.getCaracteristique().isEmpty())
            .map(item -> "• <b>" + item.getDesignation() + "</b> : " + item.getCaracteristique())
            .collect(Collectors.joining("<br/>"));
        if (allCaracteristiques.isEmpty()) allCaracteristiques = "-";

        InvoiceItem summary = new InvoiceItem(allRefs, allDesignations, totalQty, allInvs, allCaracteristiques, 0.0);
        JRDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(summary));

        parameters.put("itemsDataSource", new JRBeanCollectionDataSource(invoiceItems));

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        
        String filename = "bon_retour_" + affectation.getId() + "_" + System.currentTimeMillis() + ".pdf";
        java.io.File pdfFile = new java.io.File(filename);
        JasperExportManager.exportReportToPdfFile(jasperPrint, pdfFile.getAbsolutePath());
        
        System.out.println("Return report saved to " + pdfFile.getAbsolutePath());
        return pdfFile;
    }

    private void generateLabels(Affectation affectation) throws Exception {
        JasperReport jasperReport = getOrCompileReport("/ma/estf/magasiner/reports/label.jrxml");

        String dateStr = affectation.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        
        int labelCount = 0;
        JasperPrint mainPrint = null;

        BonCommandeDao bcDao = new BonCommandeDao();

        for (AffectationItem item : affectation.getItems()) {
            if (item.getInventoryNumber() == null || item.getInventoryNumber().isEmpty() || item.getInventoryNumber().equals("-")) continue;

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("inventoryNumber", item.getInventoryNumber());
            parameters.put("designation", item.getArticle().getName());

            // Fetch Exercice from BonCommande
            String bcNumero = item.getBcNumero();
            String exerciceVal = null;
            if (bcNumero != null && !bcNumero.trim().isEmpty() && !"-".equals(bcNumero)) {
                BonCommande bcEntity = bcDao.findByNumero(bcNumero.trim());
                if (bcEntity != null) {
                    exerciceVal = bcEntity.getExercice();
                }
            }

            String yearStr;
            String fullYearStr;
            String dateVal;
            if (exerciceVal != null && !exerciceVal.trim().isEmpty()) {
                dateVal = exerciceVal.trim();
                String cleanExercice = exerciceVal.trim();
                if (cleanExercice.length() >= 2) {
                    yearStr = cleanExercice.substring(cleanExercice.length() - 2);
                } else {
                    yearStr = cleanExercice;
                }
                fullYearStr = cleanExercice;
            } else {
                dateVal = dateStr;
                yearStr = String.valueOf(affectation.getDate().getYear()).substring(2);
                fullYearStr = String.valueOf(affectation.getDate().getYear());
            }

            String affectationName = affectation.getDepartment() != null ? affectation.getDepartment().getName() : "";

            parameters.put("date", dateVal);
            parameters.put("year", yearStr);
            parameters.put("fullYear", fullYearStr);
            parameters.put("bc", item.getBcNumero() != null ? item.getBcNumero() : "-");
            parameters.put("fournisseur", item.getFournisseur() != null ? item.getFournisseur() : "-");
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
