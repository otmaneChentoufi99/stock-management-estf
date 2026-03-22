package ma.estf.magasiner.dao;

import java.util.List;

public interface GenericDao<T, ID> {
    void save(T entity);
    void update(T entity);
    void delete(T entity);
    T findById(ID id);
    List<T> findAll();
}
