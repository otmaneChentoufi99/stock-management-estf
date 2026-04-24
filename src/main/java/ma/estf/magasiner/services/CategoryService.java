package ma.estf.magasiner.services;

import ma.estf.magasiner.dao.CategoryDao;
import ma.estf.magasiner.models.dto.CategoryDto;
import ma.estf.magasiner.models.entity.Category;
import ma.estf.magasiner.models.mapper.CategoryMapper;

import java.util.List;
import java.util.Optional;

public class CategoryService {
    private final CategoryDao categoryDao;

    public CategoryService() {
        this.categoryDao = new CategoryDao();
    }

    public List<CategoryDto> findAll() {
        return CategoryMapper.toDtoList(categoryDao.findAll());
    }

    public CategoryDto save(CategoryDto categoryDto) {
        Category category = CategoryMapper.toEntity(categoryDto);
        categoryDao.save(category);
        return CategoryMapper.toDto(category);
    }

    public void update(CategoryDto categoryDto) {
        categoryDao.update(CategoryMapper.toEntity(categoryDto));
    }

    public void delete(Category id) {
        categoryDao.delete(id);
    }

    public Optional<CategoryDto> findByName(String name) {
        return categoryDao.findByName(name).map(CategoryMapper::toDto);
    }
}
