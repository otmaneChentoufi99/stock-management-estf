package ma.estf.magasiner.services;

import ma.estf.magasiner.dao.AffectationDao;
import ma.estf.magasiner.dao.ArticleDao;
import ma.estf.magasiner.dao.DepartmentDao;
import ma.estf.magasiner.dao.HibernateUtil;
import ma.estf.magasiner.models.dto.AffectationDto;
import ma.estf.magasiner.models.dto.AffectationItemDto;
import ma.estf.magasiner.models.entity.Affectation;
import ma.estf.magasiner.models.entity.AffectationItem;
import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.entity.Department;
import ma.estf.magasiner.models.mapper.AffectationMapper;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AffectationService {
    private final AffectationDao affectationDao = new AffectationDao();
    private final DepartmentDao departmentDao = new DepartmentDao();

    public java.io.File checkoutCart(AffectationDto affectationDto, boolean isMaterial) throws Exception {
        if (affectationDto.getItems() == null || affectationDto.getItems().isEmpty()) {
            throw new Exception("Cart is empty.");
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            
            Department dept = null;
            if (affectationDto.getDepartment() != null) {
                dept = session.get(Department.class, affectationDto.getDepartment().getId());
            }

            Affectation affectation = Affectation.builder()
                    .date(LocalDateTime.now())
                    .employeeName(affectationDto.getEmployeeName())
                    .department(dept)
                    .category(isMaterial ? "MATERIEL" : "CONSOMMABLE")
                    .items(new ArrayList<>())
                    .build();

            for (AffectationItemDto itemDto : affectationDto.getItems()) {
                Article article = session.get(Article.class, itemDto.getArticle().getId());
                if (article == null || article.getQuantityInStock() < itemDto.getQuantity()) {
                    throw new Exception("Insufficient stock for article: " + itemDto.getArticle().getName());
                }
                
                // Record OUT movement and Update stock
                String target = (dept != null) ? dept.getName() : affectation.getEmployeeName();
                new MovementService().recordMovement(
                    session,
                    ma.estf.magasiner.models.entity.MovementType.OUT,
                    article.getId(),
                    itemDto.getQuantity(),
                    "STOCK",
                    target,
                    "AFFECTATION-" + affectation.getId()
                );

                if (isMaterial && itemDto.getQuantity() > 1) {
                    String currentInv = itemDto.getInventoryNumber();
                    for (int i = 0; i < itemDto.getQuantity(); i++) {
                        AffectationItem item = AffectationItem.builder()
                                .affectation(affectation)
                                .article(article)
                                .quantity(1)
                                .inventoryNumber(currentInv)
                                .condition("GOOD")
                                .build();
                        affectation.getItems().add(item);
                        currentInv = generateNextInventoryNumber(currentInv);
                    }
                } else {
                    AffectationItem item = AffectationItem.builder()
                            .affectation(affectation)
                            .article(article)
                            .quantity(itemDto.getQuantity())
                            .inventoryNumber(itemDto.getInventoryNumber())
                            .condition("GOOD")
                            .build();
    
                    affectation.getItems().add(item);
                }
            }

            session.persist(affectation);
            tx.commit();
            
            return generateInvoice(affectation, isMaterial);
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    public List<AffectationDto> getAllAffectations() {
        List<Affectation> entities = affectationDao.findAll();
        return entities.stream()
                .map(AffectationMapper::toDto)
                .collect(Collectors.toList());
    }

    private String generateNextInventoryNumber(String current) {
        if (current == null || current.trim().isEmpty()) return current;
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("^(.*?)(\\d+)$");
        java.util.regex.Matcher m = p.matcher(current.trim());
        if (m.matches()) {
            String prefix = m.group(1);
            String numberStr = m.group(2);
            try {
                long number = Long.parseLong(numberStr);
                number++;
                String formatStr = "%0" + numberStr.length() + "d";
                return prefix + String.format(formatStr, number);
            } catch (NumberFormatException e) {
                return current + "-1";
            }
        }
        return current + "-1";
    }

    private java.io.File generateInvoice(Affectation affectation, boolean isMaterial) throws Exception {
        String filename = "bon_affectation_" + affectation.getId() + ".pdf";
        java.io.File pdfFile = new java.io.File(filename);
        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
        com.lowagie.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(pdfFile));
        document.open();
        
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
        if (isMaterial) {
            titleFont.setColor(new java.awt.Color(0, 102, 204));
        }
        com.lowagie.text.Font boldFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.NORMAL);

        document.add(new com.lowagie.text.Paragraph("UNIVERSITE SIDI MOHAMED BEN ABDELLAH", boldFont));
        document.add(new com.lowagie.text.Paragraph("ECOLE SUPERIEURE DE TECHNOLOGIE - FES", boldFont));
        document.add(new com.lowagie.text.Paragraph("SERVICE ECONOMIQUE", normalFont));
        document.add(new com.lowagie.text.Paragraph("\n"));

        String titleText = "BON DE SORTIE MAGASIN N° " + affectation.getId() + (isMaterial ? " (Matériel)" : " (Consommable)");
        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(titleText, titleFont);
        title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(title);
        document.add(new com.lowagie.text.Paragraph("\n"));

        com.lowagie.text.pdf.PdfPTable infoTable = new com.lowagie.text.pdf.PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.getDefaultCell().setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        
        infoTable.addCell(new com.lowagie.text.Phrase("Fait a Fes, le: " + affectation.getDate().toLocalDate().toString(), normalFont));
        infoTable.addCell(new com.lowagie.text.Phrase("Departement: " + (affectation.getDepartment() != null ? affectation.getDepartment().getName() : ""), normalFont));
        infoTable.addCell(new com.lowagie.text.Phrase("Beneficiaire: " + (affectation.getEmployeeName() != null ? affectation.getEmployeeName() : ""), normalFont));
        infoTable.addCell(new com.lowagie.text.Phrase(" ", normalFont));
        document.add(infoTable);
        document.add(new com.lowagie.text.Paragraph("\n"));

        int numCols = isMaterial ? 4 : 3;
        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(numCols);
        table.setWidthPercentage(100);
        if (isMaterial) {
            table.setWidths(new float[]{2f, 4f, 2f, 3f});
        } else {
            table.setWidths(new float[]{2f, 5f, 2f});
        }
        
        String[] headers = isMaterial ? new String[]{"Reference", "Designation", "Qte Livree", "N° Inventaire"} : new String[]{"Reference", "Designation", "Qte Livree"};
        
        java.awt.Color headerColor = isMaterial ? new java.awt.Color(173, 216, 230) : new java.awt.Color(230, 230, 230); // Light blue for material, gray for consumable
        for (String h : headers) {
            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(h, boldFont));
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBackgroundColor(headerColor);
            table.addCell(cell);
        }

        for (ma.estf.magasiner.models.entity.AffectationItem item : affectation.getItems()) {
            com.lowagie.text.pdf.PdfPCell refCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(item.getArticle().getReference(), normalFont));
            refCell.setPadding(5);
            table.addCell(refCell);

            com.lowagie.text.pdf.PdfPCell nameCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(item.getArticle().getName(), normalFont));
            nameCell.setPadding(5);
            table.addCell(nameCell);

            com.lowagie.text.pdf.PdfPCell qtyCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.valueOf(item.getQuantity()), normalFont));
            qtyCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            qtyCell.setPadding(5);
            table.addCell(qtyCell);
            
            if (isMaterial) {
                String invText = item.getInventoryNumber() != null && !item.getInventoryNumber().trim().isEmpty() ? item.getInventoryNumber() : "-";
                com.lowagie.text.pdf.PdfPCell invCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(invText, normalFont));
                invCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                invCell.setPadding(5);
                table.addCell(invCell);
            }
        }
        document.add(table);
        document.add(new com.lowagie.text.Paragraph("\n\n\n"));

        com.lowagie.text.pdf.PdfPTable sigTable = new com.lowagie.text.pdf.PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.getDefaultCell().setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        
        com.lowagie.text.pdf.PdfPCell cell1 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Signature du Demandeur", boldFont));
        cell1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell1.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        sigTable.addCell(cell1);
        
        com.lowagie.text.pdf.PdfPCell cell2 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Signature du Magasinier", boldFont));
        cell2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell2.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        sigTable.addCell(cell2);
        
        document.add(sigTable);
        document.close();
        System.out.println("Invoice saved to " + pdfFile.getAbsolutePath());
        return pdfFile;
    }

    public void transferItems(Long assignmentId, Long articleId, int quantity, String newEmployeeName, Department newDept) throws Exception {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Affectation source = session.get(Affectation.class, assignmentId);
            if (source == null) throw new Exception("Source assignment not found.");

            // Find the item
            AffectationItem sourceItem = source.getItems().stream()
                .filter(i -> i.getArticle().getId().equals(articleId))
                .findFirst()
                .orElseThrow(() -> new Exception("Article not found in this assignment."));

            if (sourceItem.getQuantity() < quantity) throw new Exception("Insufficient quantity in assignment.");

            // Create new Assignment
            Affectation target = Affectation.builder()
                .date(LocalDateTime.now())
                .employeeName(newEmployeeName)
                .department(newDept)
                .category(source.getCategory())
                .build();
            session.persist(target);

            AffectationItem targetItem = AffectationItem.builder()
                .affectation(target)
                .article(sourceItem.getArticle())
                .quantity(quantity)
                .inventoryNumber(sourceItem.getInventoryNumber()) // Carry over inv number if single item
                .condition(sourceItem.getCondition())
                .build();
            session.persist(targetItem);

            // Log TRANSFER
            String fromEnt = (source.getDepartment() != null) ? source.getDepartment().getName() : source.getEmployeeName();
            String toEnt = (newDept != null) ? newDept.getName() : newEmployeeName;
            
            new MovementService().recordMovement(session, ma.estf.magasiner.models.entity.MovementType.TRANSFER, articleId, quantity, fromEnt, toEnt, "TRANSFER-FROM-" + assignmentId);

            // Update source
            sourceItem.setQuantity(sourceItem.getQuantity() - quantity);

            // Check if all items in source are 0
            boolean allEmpty = source.getItems().stream().allMatch(i -> i.getQuantity() == 0);
            if (allEmpty) {
                source.setStatus("CLOSED");
                source.setDateEnd(LocalDateTime.now());
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void returnToInventory(Long assignmentId, Long articleId, int quantity, String condition) throws Exception {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Affectation source = session.get(Affectation.class, assignmentId);
            if (source == null) throw new Exception("Assignment not found.");

            AffectationItem sourceItem = source.getItems().stream()
                .filter(i -> i.getArticle().getId().equals(articleId))
                .findFirst()
                .orElseThrow(() -> new Exception("Article not found in assignment."));

            if (sourceItem.getQuantity() < quantity) throw new Exception("Insufficient quantity to return.");

            // Log RETURN
            String fromEnt = (source.getDepartment() != null) ? source.getDepartment().getName() : source.getEmployeeName();
            
            ma.estf.magasiner.models.entity.MovementType mType = (condition.equals("DAMAGED") || condition.equals("BROKEN")) 
                ? ma.estf.magasiner.models.entity.MovementType.DAMAGE 
                : ma.estf.magasiner.models.entity.MovementType.RETURN;
            
            new MovementService().recordMovement(session, mType, articleId, quantity, fromEnt, "STOCK", "RETURN-FROM-" + assignmentId);

            // Update assignment
            sourceItem.setQuantity(sourceItem.getQuantity() - quantity);
            sourceItem.setCondition(condition);

            boolean allEmpty = source.getItems().stream().allMatch(i -> i.getQuantity() == 0);
            if (allEmpty) {
                source.setStatus("CLOSED");
                source.setDateEnd(LocalDateTime.now());
            }

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void transferAllItems(Long assignmentId, String newEmployeeName, Department newDept) throws Exception {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Affectation source = session.get(Affectation.class, assignmentId);
            if (source == null) throw new Exception("Source assignment not found.");

            // Create target assignment
            Affectation target = Affectation.builder()
                .date(LocalDateTime.now())
                .employeeName(newEmployeeName)
                .department(newDept)
                .category(source.getCategory())
                .items(new ArrayList<>())
                .build();
            session.persist(target);

            String fromEnt = (source.getDepartment() != null) ? source.getDepartment().getName() : source.getEmployeeName();
            String toEnt = (newDept != null) ? newDept.getName() : newEmployeeName;

            for (AffectationItem sourceItem : source.getItems()) {
                if (sourceItem.getQuantity() > 0) {
                    int qty = sourceItem.getQuantity();
                    
                    AffectationItem targetItem = AffectationItem.builder()
                        .affectation(target)
                        .article(sourceItem.getArticle())
                        .quantity(qty)
                        .inventoryNumber(sourceItem.getInventoryNumber())
                        .condition(sourceItem.getCondition())
                        .build();
                    session.persist(targetItem);

                    new MovementService().recordMovement(session, ma.estf.magasiner.models.entity.MovementType.TRANSFER, sourceItem.getArticle().getId(), qty, fromEnt, toEnt, "TRANSFER-ALL-FROM-" + assignmentId);

                    sourceItem.setQuantity(0);
                }
            }

            source.setStatus("CLOSED");
            source.setDateEnd(LocalDateTime.now());

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public void returnAllItems(Long assignmentId, String condition) throws Exception {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Affectation source = session.get(Affectation.class, assignmentId);
            if (source == null) throw new Exception("Assignment not found.");

            String fromEnt = (source.getDepartment() != null) ? source.getDepartment().getName() : source.getEmployeeName();
            ma.estf.magasiner.models.entity.MovementType mType = (condition.equals("DAMAGED") || condition.equals("BROKEN")) 
                ? ma.estf.magasiner.models.entity.MovementType.DAMAGE 
                : ma.estf.magasiner.models.entity.MovementType.RETURN;

            for (AffectationItem item : source.getItems()) {
                if (item.getQuantity() > 0) {
                    new MovementService().recordMovement(session, mType, item.getArticle().getId(), item.getQuantity(), fromEnt, "STOCK", "RETURN-ALL-FROM-" + assignmentId);
                    item.setQuantity(0);
                    item.setCondition(condition);
                }
            }

            source.setStatus("CLOSED");
            source.setDateEnd(LocalDateTime.now());

            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
