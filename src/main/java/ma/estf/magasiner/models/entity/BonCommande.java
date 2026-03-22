package ma.estf.magasiner.models.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "bons_commande")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BonCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numero;
    
    private String dateBC;
    private String serviceDemandeur;
    private String statut;

    @OneToMany(mappedBy = "bonCommande", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LigneBonCommande> lignes;
}
