package ma.estf.magasiner.dao;
import ma.estf.magasiner.models.entity.Department;

public class DepartmentDao extends GenericDaoImpl<Department, Long> {
    public DepartmentDao() {
        super(Department.class);
    }
}
