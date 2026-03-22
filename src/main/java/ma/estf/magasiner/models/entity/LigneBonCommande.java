package ma.estf.magasiner.models.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lignes_bon_commande")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneBonCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bon_commande_id", nullable = false)
    private BonCommande bonCommande;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false)
    private int quantiteCommandee;

    @Column(nullable = false)
    private int quantiteLivree;
}
