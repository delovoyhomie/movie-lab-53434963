package ru.itmo.movielab.dto;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import ru.itmo.movielab.validation.ExactIntegerDeserializer;
import ru.itmo.movielab.validation.ExactLongDeserializer;

// Отдельная входная модель: id и creationDate клиент изменить не может.
public class MovieInput {

    @JsonbTypeDeserializer(ExactLongDeserializer.class)
    public Long version;

    public String name;

    @JsonbTypeDeserializer(ExactLongDeserializer.class)
    public Long coordinatesId;

    @JsonbTypeDeserializer(ExactLongDeserializer.class)
    public Long oscarsCount;

    public Double budget;
    public Float totalBoxOffice;
    public String mpaaRating;

    @JsonbTypeDeserializer(ExactLongDeserializer.class)
    public Long directorId;

    @JsonbTypeDeserializer(ExactLongDeserializer.class)
    public Long screenwriterId;

    @JsonbTypeDeserializer(ExactLongDeserializer.class)
    public Long operatorId;

    @JsonbTypeDeserializer(ExactIntegerDeserializer.class)
    public Integer length;

    @JsonbTypeDeserializer(ExactLongDeserializer.class)
    public Long goldenPalmCount;

    public String tagline;
    public String genre;
}
