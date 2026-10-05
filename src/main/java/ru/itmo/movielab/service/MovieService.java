package ru.itmo.movielab.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Map;
import ru.itmo.movielab.dto.MovieInput;
import ru.itmo.movielab.model.*;
import ru.itmo.movielab.repository.MovieRepository;

@ApplicationScoped
@Transactional
public class MovieService {

    @Inject
    MovieRepository repository;

    @Inject
    ValidationService validation;

    public Map<String, Object> page(
        int page,
        int size,
        String column,
        String q,
        String sort,
        boolean desc
    ) {
        return repository.page(page, size, column, q, sort, desc);
    }

    public Movie get(long id) {
        Movie movie = repository.find(id);
        if (movie == null) {
            throw new AppException(404, "Фильм #" + id + " не найден");
        }
        return movie;
    }

    public Movie create(MovieInput input) {
        repository.lockWrites();
        Movie movie = new Movie();
        fillMovie(movie, input);
        validation.check(movie);
        // @PrePersist повторно устанавливает серверную дату при сохранении.
        repository.save(movie);
        repository.flush();
        return movie;
    }

    public Movie update(long id, MovieInput input) {
        repository.lockWrites();
        Movie movie = get(id);
        validation.version(input.version, movie.getVersion());
        fillMovie(movie, input);
        validation.check(movie);
        repository.flush();
        return movie;
    }

    public void delete(long id, Long version) {
        repository.lockWrites();
        Movie movie = get(id);
        validation.version(version, movie.getVersion());
        repository.remove(movie);
        repository.flush();
    }

    public Object revision() {
        return repository.revision();
    }

    private void fillMovie(Movie movie, MovieInput input) {
        if (input == null) {
            throw new AppException(400, "Тело запроса не задано");
        }
        if (input.budget == null || input.totalBoxOffice == null) {
            throw new AppException(400, "budget и totalBoxOffice обязательны");
        }
        movie.setName(input.name);
        movie.setCoordinates(repository.reference(Coordinates.class, input.coordinatesId));
        movie.setOscarsCount(input.oscarsCount);
        movie.setBudget(input.budget);
        movie.setTotalBoxOffice(input.totalBoxOffice);
        movie.setMpaaRating(validation.enumValue(MpaaRating.class, input.mpaaRating));
        movie.setDirector(repository.reference(Person.class, input.directorId));
        movie.setScreenwriter(repository.reference(Person.class, input.screenwriterId));
        movie.setOperator(repository.reference(Person.class, input.operatorId));
        movie.setLength(input.length);
        movie.setGoldenPalmCount(input.goldenPalmCount);
        movie.setTagline(input.tagline);
        movie.setGenre(validation.enumValue(MovieGenre.class, input.genre));
    }
}
