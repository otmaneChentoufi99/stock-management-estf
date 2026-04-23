package ma.estf.magasiner.models.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "articles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer quantityInStock;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantityDamaged = 0;

    @Column(nullable = true)
    private Integer totalReceived;

    @Column(name = "type", nullable = true)
    private String type; // MATERIEL or CONSOMMABLE

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "article_inventory_numbers", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "inventory_number")
    private List<String> availableInventoryNumbers;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LigneBonCommande> lignesBonCommande;
}
