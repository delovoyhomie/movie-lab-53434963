package ru.itmo.movielab.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ValidationService {

    @Inject
    Validator validator;

    public void check(Object object) {
        var errors = validator.validate(object);
        if (!errors.isEmpty()) {
            throw new AppException(
                400,
                errors
                    .stream()
                    .map(e -> e.getPropertyPath() + ": " + e.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "))
            );
        }
    }

    public <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new AppException(
                400,
                "Недопустимое значение " + type.getSimpleName() + ": " + value
            );
        }
    }

    public void version(Long received, long actual) {
        if (received == null || received != actual) {
            throw new AppException(
                409,
                "Объект уже изменён. Закройте форму, откройте заново и повторите изменения."
            );
        }
    }
}
