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

    @Column(nullable = true)
    private Integer totalReceived;

    @Column(name = "type", nullable = true)
    private String type; // MATERIEL or CONSOMMABLE

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LigneBonCommande> lignesBonCommande;
}
