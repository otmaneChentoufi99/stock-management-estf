package ma.estf.magasiner.models.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "affectation_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "affectation_id", nullable = false)
    private Affectation affectation;

    @ManyToOne
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = true)
    private String inventoryNumber;

    @Builder.Default
    @Column(nullable = false)
    private String condition = "GOOD"; // GOOD, DAMAGED, BROKEN

    @Column(nullable = true)
    private String bcNumero;

    @Column(nullable = true)
    private String fournisseur;
}
