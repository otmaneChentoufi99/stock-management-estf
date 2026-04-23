package ma.estf.magasiner.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_sequences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSequence {
    @Id
    @Column(name = "id", nullable = false)
    private String id; // e.g., "MATERIAL_INV"

    @Column(name = "next_value", nullable = false)
    private Long nextValue;
}
