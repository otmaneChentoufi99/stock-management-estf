package ma.estf.magasiner.services;

import ma.estf.magasiner.dao.DepartmentDao;
import ma.estf.magasiner.models.dto.DepartmentDto;
import ma.estf.magasiner.models.mapper.DepartmentMapper;
import java.util.List;
import java.util.stream.Collectors;

public class DepartmentService {
    private final DepartmentDao dao = new DepartmentDao();

    public DepartmentService() {
        seedFakeDepartments();
    }

    private void seedFakeDepartments() {
        if (dao.findAll().isEmpty()) {
            dao.save(DepartmentMapper.toEntity(DepartmentDto.builder().name("IT & Infrastructure").build()));
            dao.save(DepartmentMapper.toEntity(DepartmentDto.builder().name("Human Resources").build()));
            dao.save(DepartmentMapper.toEntity(DepartmentDto.builder().name("Finance & Accounting").build()));
            dao.save(DepartmentMapper.toEntity(DepartmentDto.builder().name("Marketing & Sales").build()));
        }
    }

    public List<DepartmentDto> getAllDepartments() {
        return dao.findAll().stream().map(DepartmentMapper::toDto).collect(Collectors.toList());
    }

    public void createDepartment(DepartmentDto dto) {
        dao.save(DepartmentMapper.toEntity(dto));
    }
}
