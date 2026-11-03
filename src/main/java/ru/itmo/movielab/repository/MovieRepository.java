package ru.itmo.movielab.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import ru.itmo.movielab.model.*;
import ru.itmo.movielab.service.AppException;

@ApplicationScoped
public class MovieRepository {

    @PersistenceContext(unitName = "movies")
    EntityManager entityManager;

    private enum FieldType {
        TEXT, LONG, INTEGER, DOUBLE, FLOAT, DATE
    }

    private record Column(String expression, FieldType type) {}

    // Белый список: имена колонок никогда не берём напрямую из HTTP-запроса.
    private static final Map<String, Column> COLUMNS = Map.ofEntries(
        Map.entry("id", new Column("m.id", FieldType.LONG)),
        Map.entry("name", new Column("m.name", FieldType.TEXT)),
        Map.entry("coordinates", new Column("m.coordinates.id", FieldType.LONG)),
        Map.entry("coordinatesX", new Column("m.coordinates.x", FieldType.INTEGER)),
        Map.entry("coordinatesY", new Column("m.coordinates.y", FieldType.FLOAT)),
        Map.entry("creationDate", new Column("m.creationDate", FieldType.DATE)),
        Map.entry("oscarsCount", new Column("m.oscarsCount", FieldType.LONG)),
        Map.entry("budget", new Column("m.budget", FieldType.DOUBLE)),
        Map.entry("totalBoxOffice", new Column("m.totalBoxOffice", FieldType.FLOAT)),
        Map.entry("mpaaRating", new Column("cast(m.mpaaRating as string)", FieldType.TEXT)),
        Map.entry("director", new Column("d.name", FieldType.TEXT)),
        Map.entry("screenwriter", new Column("s.name", FieldType.TEXT)),
        Map.entry("operator", new Column("o.name", FieldType.TEXT)),
        Map.entry("length", new Column("m.length", FieldType.INTEGER)),
        Map.entry("goldenPalmCount", new Column("m.goldenPalmCount", FieldType.LONG)),
        Map.entry("tagline", new Column("m.tagline", FieldType.TEXT)),
        Map.entry("genre", new Column("cast(m.genre as string)", FieldType.TEXT))
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
        if (!COLUMNS.containsKey(column) || !COLUMNS.containsKey(sort)) {
            throw new AppException(400, "Неизвестная колонка");
        }
        String fromClause =
            " from Movie m left join m.director d left join m.screenwriter s left join m.operator o";
        Column filter = COLUMNS.get(column);
        Object filterValue = query;
        String filterClause;
        // Пустой фильтр показывает в том числе строки с NULL.
        if (query.isEmpty()) {
            filterClause = " where :query = ''";
        } else if (filter.type() == FieldType.TEXT) {
            filterClause = " where locate(lower(:query), lower(" + filter.expression() + ")) > 0";
        } else {
            filterValue = parseFilter(query, filter.type());
            filterClause = " where " + filter.expression() + " = :query";
        }
        String sortField = COLUMNS.get(sort).expression();
        long total = entityManager
            .createQuery("select count(m)" + fromClause + filterClause, Long.class)
            .setParameter("query", filterValue)
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
                    (desc ? " desc nulls last" : " asc nulls last") +
                    ", m.id asc",
                Movie.class
            )
            .setParameter("query", filterValue)
            .setFirstResult(Math.multiplyExact(actualPage, size))
            .setMaxResults(size)
            .getResultList();
        return Map.of("items", items, "total", total, "page", actualPage, "size", size);
    }

    private Object parseFilter(String value, FieldType type) {
        try {
            return switch (type) {
                case LONG -> Long.valueOf(value);
                case INTEGER -> Integer.valueOf(value);
                case DOUBLE -> {
                    double number = Double.parseDouble(value);
                    if (!Double.isFinite(number)) {
                        throw new NumberFormatException();
                    }
                    yield number;
                }
                case FLOAT -> {
                    float number = Float.parseFloat(value);
                    if (!Float.isFinite(number)) {
                        throw new NumberFormatException();
                    }
                    yield number;
                }
                case DATE -> LocalDate.parse(value);
                case TEXT -> value;
            };
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new AppException(
                400,
                type == FieldType.DATE
                    ? "Укажите дату в формате ГГГГ-ММ-ДД"
                    : "Укажите корректное число в допустимом диапазоне выбранного поля"
            );
        }
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
