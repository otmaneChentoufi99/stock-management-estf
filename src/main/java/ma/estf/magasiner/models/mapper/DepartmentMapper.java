package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.entity.Department;
import ma.estf.magasiner.models.dto.DepartmentDto;

public class DepartmentMapper {
    public static DepartmentDto toDto(Department entity) {
        if (entity == null) return null;
        return DepartmentDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    public static Department toEntity(DepartmentDto dto) {
        if (dto == null) return null;
        return Department.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }
}
