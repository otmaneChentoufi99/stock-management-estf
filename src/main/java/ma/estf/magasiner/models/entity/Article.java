package ma.estf.magasiner.models.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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

    @Column(nullable = true)
    private String caracteristique;

    @Column(nullable = true)
    private Double prixUnit;

    @Column(nullable = false)
    private Integer quantityInStock;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantityDamaged = 0;

    @Column(nullable = true)
    private Integer totalReceived;



    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "article_categories",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "article_inventory_numbers", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "inventory_number")
    private List<String> availableInventoryNumbers;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LigneBonCommande> lignesBonCommande;
}
