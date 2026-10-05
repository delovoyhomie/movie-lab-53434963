package ru.itmo.movielab.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import java.util.List;

@ApplicationScoped
public class ReferenceRepository {

    @PersistenceContext(unitName = "movies")
    EntityManager entityManager;

    public <T> List<T> all(Class<T> type) {
        return entityManager
            .createQuery("select e from " + type.getSimpleName() + " e order by e.id", type)
            .getResultList();
    }

    public <T> T find(Class<T> type, long id) {
        return entityManager.find(type, id);
    }

    public void save(Object object) {
        entityManager.persist(object);
    }

    public void remove(Object object) {
        entityManager.remove(object);
    }

    public void flush() {
        entityManager.flush();
    }
}
