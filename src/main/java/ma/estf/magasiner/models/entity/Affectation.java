package ma.estf.magasiner.models.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "affectations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Affectation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private String employeeName;

    @ManyToOne(optional = true)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "affectation", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<AffectationItem> items;
}
