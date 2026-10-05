package ru.itmo.movielab.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FiniteValidator implements ConstraintValidator<Finite, Number> {

    public boolean isValid(Number n, ConstraintValidatorContext context) {
        return n == null || Double.isFinite(n.doubleValue());
    }
}
