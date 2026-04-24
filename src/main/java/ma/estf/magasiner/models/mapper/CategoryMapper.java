package ma.estf.magasiner.models.mapper;

import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.models.entity.Category;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryMapper {
    public static CategoryDto toDto(Category category) {
        if (category == null) return null;
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static Category toEntity(CategoryDto dto) {
        if (dto == null) return null;
        return Category.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }

    public static List<CategoryDto> toDtoList(List<Category> categories) {
        if (categories == null) return null;
        return categories.stream().map(CategoryMapper::toDto).collect(Collectors.toList());
    }

    public static List<Category> toEntityList(List<CategoryDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(CategoryMapper::toEntity).collect(Collectors.toList());
    }
}
