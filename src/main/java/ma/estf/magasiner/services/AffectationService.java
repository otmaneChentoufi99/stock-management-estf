package ma.estf.magasiner.services;

import ma.estf.magasiner.dao.AffectationDao;
import ma.estf.magasiner.dao.DepartmentDao;
import ma.estf.magasiner.dao.HibernateUtil;
import ma.estf.magasiner.models.dto.AffectationDto;
import ma.estf.magasiner.models.dto.AffectationItemDto;
import ma.estf.magasiner.models.entity.Affectation;
import ma.estf.magasiner.models.entity.AffectationItem;
import ma.estf.magasiner.models.entity.Article;
import ma.estf.magasiner.models.entity.Department;
import ma.estf.magasiner.models.entity.BonCommande;
import ma.estf.magasiner.models.entity.LigneBonCommande;
import ma.estf.magasiner.models.mapper.AffectationMapper;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.*;
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

            session.persist(affectation);

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
                
                String bcNumVal = "-";
                String fournisseurVal = "-";
                if (article.getLignesBonCommande() != null && !article.getLignesBonCommande().isEmpty()) {
                    LigneBonCommande lbc = article.getLignesBonCommande().get(0);
                    if (lbc.getBonCommande() != null) {
                        bcNumVal = lbc.getBonCommande().getNumero();
                        fournisseurVal = lbc.getBonCommande().getFournisseur();
                    }
                }

                if (isMaterial && itemDto.getQuantity() > 1) {
                    for (int i = 0; i < itemDto.getQuantity(); i++) {
                        String invNum = "-";
                        if (article.getAvailableInventoryNumbers() != null && !article.getAvailableInventoryNumbers().isEmpty()) {
                            invNum = article.getAvailableInventoryNumbers().remove(0);
                        }
                        AffectationItem item = AffectationItem.builder()
                                .affectation(affectation)
                                .article(article)
                                .quantity(1)
                                .inventoryNumber(invNum)
                                .condition("GOOD")
                                .bcNumero(bcNumVal)
                                .fournisseur(fournisseurVal)
                                .build();
                        affectation.getItems().add(item);
                    }
                } else {
                    String invNum = itemDto.getInventoryNumber();
                    if (isMaterial && article.getAvailableInventoryNumbers() != null && !article.getAvailableInventoryNumbers().isEmpty()) {
                        invNum = article.getAvailableInventoryNumbers().remove(0);
                    }
                    AffectationItem item = AffectationItem.builder()
                            .affectation(affectation)
                            .article(article)
                            .quantity(itemDto.getQuantity())
                            .inventoryNumber(invNum)
                            .condition("GOOD")
                            .bcNumero(bcNumVal)
                            .fournisseur(fournisseurVal)
                            .build();
    
                    affectation.getItems().add(item);
                }
                
                // Save updated article with reduced inventory numbers list
                session.merge(article);
            }

            session.persist(affectation);
            tx.commit();
            
            new JasperReportService().generateInvoiceAsync(affectation);
            return null;
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



    public java.io.File transferItems(Long assignmentId, Map<Long, Integer> itemsToTransfer, String newEmployeeName, Long newDeptId) throws Exception {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Affectation source = session.get(Affectation.class, assignmentId);
            if (source == null) throw new Exception("Source assignment not found.");

            Department dept = null;
            if (newDeptId != null) {
                dept = session.get(Department.class, newDeptId);
            }

            // Create new Assignment
            Affectation target = Affectation.builder()
                .date(LocalDateTime.now())
                .employeeName(newEmployeeName)
                .department(dept)
                .category(source.getCategory())
                .items(new ArrayList<>())
                .build();
            session.persist(target);

            String fromEnt = (source.getDepartment() != null) ? source.getDepartment().getName() : source.getEmployeeName();
            String toEnt = (dept != null) ? dept.getName() : newEmployeeName;

            for (Map.Entry<Long, Integer> entry : itemsToTransfer.entrySet()) {
                Long itemId = entry.getKey();
                Integer qty = entry.getValue();

                AffectationItem sourceItem = session.get(AffectationItem.class, itemId);
                if (sourceItem == null) continue;
                if (sourceItem.getQuantity() < qty) throw new Exception("Insufficient quantity for article: " + sourceItem.getArticle().getName());

                AffectationItem targetItem = AffectationItem.builder()
                    .affectation(target)
                    .article(sourceItem.getArticle())
                    .quantity(qty)
                    .inventoryNumber(sourceItem.getInventoryNumber())
                    .condition(sourceItem.getCondition())
                    .bcNumero(sourceItem.getBcNumero())
                    .fournisseur(sourceItem.getFournisseur())
                    .sourceItemId(sourceItem.getId())
                    .build();
                target.getItems().add(targetItem);
                
                new MovementService().recordMovement(session, ma.estf.magasiner.models.entity.MovementType.TRANSFER, sourceItem.getArticle().getId(), qty, fromEnt, toEnt, "TRANSFER-FROM-" + assignmentId);

                sourceItem.setQuantity(sourceItem.getQuantity() - qty);
            }

            // Check if all items in source are 0
            boolean allEmpty = source.getItems().stream().allMatch(i -> i.getQuantity() <= 0);
            if (allEmpty) {
                source.setStatus("CLOSED");
                source.setDateEnd(LocalDateTime.now());
            }

            session.persist(target);
            tx.commit();
            
            if ("MATERIEL".equals(target.getCategory())) {
                new JasperReportService().generateTransformationReport(target, source.getEmployeeName());
                return null; // The file is opened by JasperReportService
            } else {
                return new JasperReportService().generateInvoice(target);
            }
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public java.io.File returnToInventory(Long assignmentId, List<ma.estf.magasiner.models.dto.AffectationItemDto> itemsToReturn) throws Exception {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Affectation source = session.get(Affectation.class, assignmentId);
            if (source == null) throw new Exception("Assignment not found.");

            if (!"MATERIEL".equals(source.getCategory())) {
                throw new Exception("Le retour au magasin n'est pas autorisé pour les articles consommables.");
            }

            List<AffectationItem> returnedEntities = new ArrayList<>();
            String fromEnt = (source.getDepartment() != null) ? source.getDepartment().getName() : source.getEmployeeName();

            for (ma.estf.magasiner.models.dto.AffectationItemDto dto : itemsToReturn) {
                AffectationItem sourceItem = session.get(AffectationItem.class, dto.getId());
                if (sourceItem == null) continue;
                
                int qty = dto.getQuantity();

                if (sourceItem.getQuantity() < qty) throw new Exception("Insufficient quantity to return for: " + sourceItem.getArticle().getName());

                // Record return movement (which automatically increments quantityInStock)
                new MovementService().recordMovement(session, ma.estf.magasiner.models.entity.MovementType.RETURN, sourceItem.getArticle().getId(), qty, fromEnt, "STOCK", "RETURN-FROM-" + assignmentId);

                sourceItem.setQuantity(sourceItem.getQuantity() - qty);
                sourceItem.setCondition("RETURNED");

                // Restore inventory number back to available pool
                Article article = sourceItem.getArticle();
                if (article != null) {
                    String invNum = sourceItem.getInventoryNumber();
                    if (invNum != null && !invNum.isEmpty() && !"-".equals(invNum)) {
                        List<String> invList = article.getAvailableInventoryNumbers();
                        if (invList == null) {
                            invList = new ArrayList<>();
                            article.setAvailableInventoryNumbers(invList);
                        }
                        if (!invList.contains(invNum)) {
                            invList.add(invNum);
                            Collections.sort(invList);
                        }
                    }
                    session.merge(article);
                }

                // Create a temporary item for PDF generation
                AffectationItem temp = AffectationItem.builder()
                        .article(sourceItem.getArticle())
                        .quantity(qty)
                        .inventoryNumber(sourceItem.getInventoryNumber())
                        .condition("RETURNED")
                        .bcNumero(sourceItem.getBcNumero())
                        .fournisseur(sourceItem.getFournisseur())
                        .build();
                returnedEntities.add(temp);
            }

            boolean allEmpty = source.getItems().stream().allMatch(i -> i.getQuantity() <= 0);
            if (allEmpty) {
                source.setStatus("CLOSED");
                source.setDateEnd(LocalDateTime.now());
            }

            tx.commit();
            return new JasperReportService().generateReturnReport(source, returnedEntities);
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
                        .sourceItemId(sourceItem.getId())
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

    public void cancelAffectation(Long assignmentId) throws Exception {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Affectation affectation = session.get(Affectation.class, assignmentId);
            if (affectation == null) {
                throw new Exception("Assignment not found.");
            }

            if (!"ACTIVE".equals(affectation.getStatus())) {
                throw new Exception("Only ACTIVE assignments can be canceled.");
            }

            String targetName = (affectation.getDepartment() != null) ? affectation.getDepartment().getName() : affectation.getEmployeeName();

            for (AffectationItem item : affectation.getItems()) {
                Article article = item.getArticle();
                if (article != null && item.getQuantity() > 0) {
                    boolean restoredToSource = false;

                    if (item.getSourceItemId() != null) {
                        AffectationItem sourceItem = session.get(AffectationItem.class, item.getSourceItemId());
                        if (sourceItem != null) {
                            // Restore quantity to original/source item
                            sourceItem.setQuantity(sourceItem.getQuantity() + item.getQuantity());
                            
                            Affectation sourceAff = sourceItem.getAffectation();
                            if (sourceAff != null && "CLOSED".equals(sourceAff.getStatus())) {
                                sourceAff.setStatus("ACTIVE");
                                sourceAff.setDateEnd(null);
                                session.merge(sourceAff);
                            }
                            session.merge(sourceItem);

                            String sourceName = "-";
                            if (sourceAff != null) {
                                sourceName = (sourceAff.getDepartment() != null) ? sourceAff.getDepartment().getName() : sourceAff.getEmployeeName();
                            }

                            // Record TRANSFER movement back to the original source beneficiary/dept
                            new MovementService().recordMovement(
                                session,
                                ma.estf.magasiner.models.entity.MovementType.TRANSFER,
                                article.getId(),
                                item.getQuantity(),
                                targetName,
                                sourceName,
                                "CANCEL-TRANSFER-" + assignmentId
                            );

                            restoredToSource = true;
                        }
                    }

                    if (!restoredToSource) {
                        // 1. Revert quantity in stock
                        article.setQuantityInStock(article.getQuantityInStock() + item.getQuantity());

                        // 2. Revert inventory number if applicable
                        if ("MATERIEL".equals(affectation.getCategory())) {
                            String invNum = item.getInventoryNumber();
                            if (invNum != null && !invNum.isEmpty() && !"-".equals(invNum)) {
                                List<String> invList = article.getAvailableInventoryNumbers();
                                if (invList == null) {
                                    invList = new ArrayList<>();
                                    article.setAvailableInventoryNumbers(invList);
                                }
                                if (!invList.contains(invNum)) {
                                    invList.add(invNum);
                                    Collections.sort(invList);
                                }
                            }
                        }

                        // 3. Record CORRECTION movement to stock
                        new MovementService().recordMovement(
                            session,
                            ma.estf.magasiner.models.entity.MovementType.CORRECTION,
                            article.getId(),
                            item.getQuantity(),
                            targetName,
                            "STOCK",
                            "CANCEL-AFFECTATION-" + assignmentId
                        );

                        session.merge(article);
                    }
                }

                // Zero out the item's active quantity
                item.setQuantity(0);
                item.setCondition("CANCELED");
                session.merge(item);
            }

            // 4. Set overall assignment status to CANCELED
            affectation.setStatus("CANCELED");
            affectation.setDateEnd(LocalDateTime.now());
            session.merge(affectation);

            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }
}
