package ma.estf.magasiner.models.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationDto {
    private Long id;
    private LocalDateTime date;
    private String employeeName;
    private DepartmentDto department;
    private List<AffectationItemDto> items;
}
