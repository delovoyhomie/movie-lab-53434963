package ru.itmo.movielab.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import java.util.*;
import ru.itmo.movielab.model.*;
import ru.itmo.movielab.service.AppException;

@ApplicationScoped
public class MovieRepository {

    @PersistenceContext(unitName = "movies")
    EntityManager entityManager;

    // Белый список: имена колонок никогда не берём напрямую из HTTP-запроса.
    private static final Map<String, String> COLUMNS = Map.of(
        "name",
        "m.name",
        "tagline",
        "m.tagline",
        "genre",
        "cast(m.genre as string)",
        "mpaaRating",
        "cast(m.mpaaRating as string)",
        "director",
        "d.name",
        "screenwriter",
        "s.name",
        "operator",
        "o.name"
    );

    public Map<String, Object> page(
        int page,
        int size,
        String column,
        String query,
        String sort,
        boolean desc
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(400, "Страница ≥ 0, размер от 1 до 100");
        }
        if (!COLUMNS.containsKey(column) || !(COLUMNS.containsKey(sort) || sort.equals("id"))) {
            throw new AppException(400, "Неизвестная колонка");
        }
        String fromClause =
            " from Movie m left join m.director d left join m.screenwriter s left join m.operator o";
        String filterClause =
            " where locate(lower(:query), lower(" + COLUMNS.get(column) + ")) > 0";
        // Пустой фильтр показывает в том числе строки с NULL.
        if (query.isEmpty()) {
            filterClause = " where :query = ''";
        }
        String sortField = sort.equals("id") ? "m.id" : COLUMNS.get(sort);
        long total = entityManager
            .createQuery("select count(m)" + fromClause + filterClause, Long.class)
            .setParameter("query", query)
            .getSingleResult();
        long lastPage = total == 0 ? 0 : (total - 1) / size;
        int actualPage = (int) Math.min(page, lastPage);
        List<Movie> items = entityManager
            .createQuery(
                "select m" +
                    fromClause +
                    filterClause +
                    " order by " +
                    sortField +
                    (desc ? " desc" : " asc") +
                    ", m.id asc",
                Movie.class
            )
            .setParameter("query", query)
            .setFirstResult(Math.multiplyExact(actualPage, size))
            .setMaxResults(size)
            .getResultList();
        return Map.of("items", items, "total", total, "page", actualPage, "size", size);
    }

    public Movie find(long id) {
        return entityManager.find(Movie.class, id);
    }

    public void save(Movie movie) {
        entityManager.persist(movie);
    }

    public void flush() {
        entityManager.flush();
    }

    public void remove(Movie movie) {
        entityManager.remove(movie);
    }

    public <T> T reference(Class<T> type, Long id) {
        if (id == null) {
            return null;
        }
        T object = entityManager.find(type, id);
        if (object == null) {
            throw new AppException(
                400,
                "Связанный объект " + type.getSimpleName() + " #" + id + " не найден"
            );
        }
        return object;
    }

    // Небольшая учебная система: сериализуем записи, включая перераспределение наград.
    public void lockWrites() {
        entityManager
            .createNativeQuery("LOCK TABLE movies IN SHARE ROW EXCLUSIVE MODE")
            .executeUpdate();
    }

    public Object revision() {
        return entityManager
            .createNativeQuery("select value from app_revision where id=1")
            .getSingleResult();
    }
}
