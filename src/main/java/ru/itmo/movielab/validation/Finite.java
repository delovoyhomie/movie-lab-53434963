package ru.itmo.movielab.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FiniteValidator.class)
public @interface Finite {
    String message() default "должно быть конечным числом";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
