package ru.itmo.movielab.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.*;
import ru.itmo.movielab.model.*;
import ru.itmo.movielab.repository.SpecialRepository;

@ApplicationScoped
@Transactional
public class SpecialService {

    @Inject
    SpecialRepository repository;

    @Inject
    ValidationService validation;

    public Map<String, Object> deleteTagline(String text) {
        requireText(text);
        Object id = repository.deleteTagline(text);
        return id == null ? Map.of("deleted", false) : Map.of("deleted", true, "id", id);
    }

    public List<Movie> contains(String text) {
        requireText(text);
        return repository.contains(text);
    }

    public List<Movie> less(String genre) {
        requireGenre(genre);
        return repository.less(genre);
    }

    public List<Person> writers() {
        return repository.writers();
    }

    public Map<String, Object> redistribute(String source, String target) {
        requireGenre(source);
        requireGenre(target);
        if (source.equals(target)) {
            throw new AppException(400, "Жанры должны отличаться");
        }
        return Map.of("transferred", repository.redistribute(source, target));
    }

    private void requireText(String text) {
        if (text == null) {
            throw new AppException(400, "Строка обязательна (может быть пустой)");
        }
    }

    private void requireGenre(String genre) {
        if (validation.enumValue(MovieGenre.class, genre) == null) {
            throw new AppException(400, "Укажите жанр");
        }
    }
}
