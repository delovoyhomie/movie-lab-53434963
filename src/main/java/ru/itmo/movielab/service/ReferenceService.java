package ru.itmo.movielab.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.*;
import ru.itmo.movielab.dto.ReferenceInput;
import ru.itmo.movielab.model.*;
import ru.itmo.movielab.repository.*;

@ApplicationScoped
@Transactional
public class ReferenceService {

    @Inject
    ReferenceRepository repository;

    @Inject
    MovieRepository movieRepository;

    @Inject
    ValidationService validation;

    private Class<?> entityType(String kind) {
        return switch (kind) {
            case "coordinates" -> Coordinates.class;
            case "persons" -> Person.class;
            case "locations" -> Location.class;
            default -> throw new AppException(404, "Неизвестный справочник");
        };
    }

    public List<?> list(String kind) {
        return repository.all(entityType(kind));
    }

    public Object get(String kind, long id) {
        Object entity = repository.find(entityType(kind), id);
        if (entity == null) {
            throw new AppException(404, "Объект справочника не найден");
        }
        return entity;
    }

    public Object save(String kind, Long id, ReferenceInput input) {
        if (input == null) {
            throw new AppException(400, "Тело запроса не задано");
        }
        movieRepository.lockWrites();
        Object entity =
            id == null
                ? switch (kind) {
                      case "coordinates" -> new Coordinates();
                      case "persons" -> new Person();
                      case "locations" -> new Location();
                      default -> throw new AppException(404, "Неизвестный справочник");
                  }
                : get(kind, id);
        if (id != null) {
            validation.version(input.version, version(entity));
        }
        if (entity instanceof Coordinates coordinates) {
            coordinates.setX(input.x);
            coordinates.setY(input.y == null ? null : input.y.floatValue());
        } else if (entity instanceof Location location) {
            if (
                input.y == null ||
                !Double.isFinite(input.y) ||
                input.y != Math.rint(input.y) ||
                input.y < Integer.MIN_VALUE ||
                input.y > Integer.MAX_VALUE
            ) {
                throw new AppException(
                    400,
                    "location.y: необходимо целое число в диапазоне Integer"
                );
            }
            if (input.z == null) {
                throw new AppException(400, "location.z обязательно");
            }
            location.setX(input.x);
            location.setY(input.y.intValue());
            location.setZ(input.z);
            location.setName(input.name);
        } else if (entity instanceof Person person) {
            person.setName(input.name);
            person.setEyeColor(validation.enumValue(Color.class, input.eyeColor));
            person.setHairColor(validation.enumValue(Color.class, input.hairColor));
            person.setLocation(movieRepository.reference(Location.class, input.locationId));
            person.setHeight(input.height);
            person.setNationality(validation.enumValue(Country.class, input.nationality));
        }
        validation.check(entity);
        if (id == null) {
            repository.save(entity);
        }
        repository.flush();
        return entity;
    }

    private long version(Object entity) {
        if (entity instanceof Person person) {
            return person.getVersion();
        }
        if (entity instanceof Coordinates coordinates) {
            return coordinates.getVersion();
        }
        return ((Location) entity).getVersion();
    }

    public void delete(String kind, long id, Long version) {
        movieRepository.lockWrites();
        Object entity = get(kind, id);
        validation.version(version, version(entity));
        repository.remove(entity);
        repository.flush();
    }

    public Map<String, Object> options() {
        return Map.of(
            "coordinates",
            list("coordinates"),
            "persons",
            list("persons"),
            "locations",
            list("locations"),
            "ratings",
            MpaaRating.values(),
            "genres",
            MovieGenre.values(),
            "colors",
            Color.values(),
            "countries",
            Country.values()
        );
    }
}
