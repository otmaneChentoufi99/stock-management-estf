package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.entity.Affectation;
import ma.estf.magasiner.models.dto.AffectationDto;
import java.util.stream.Collectors;

public class AffectationMapper {
    public static AffectationDto toDto(Affectation entity) {
        if (entity == null) return null;
        return AffectationDto.builder()
                .id(entity.getId())
                .date(entity.getDate())
                .employeeName(entity.getEmployeeName())
                .department(DepartmentMapper.toDto(entity.getDepartment()))
                .category(entity.getCategory())
                .status(entity.getStatus())
                .dateEnd(entity.getDateEnd())
                .items(entity.getItems() != null ? 
                       entity.getItems().stream().map(AffectationItemMapper::toDto).collect(Collectors.toList()) : null)
                .build();
    }

    public static Affectation toEntity(AffectationDto dto) {
        if (dto == null) return null;
        return Affectation.builder()
                .id(dto.getId())
                .date(dto.getDate())
                .employeeName(dto.getEmployeeName())
                .department(DepartmentMapper.toEntity(dto.getDepartment()))
                .category(dto.getCategory())
                .status(dto.getStatus())
                .dateEnd(dto.getDateEnd())
                .items(dto.getItems() != null ? 
                       dto.getItems().stream().map(AffectationItemMapper::toEntity).collect(Collectors.toList()) : null)
                .build();
    }
}
