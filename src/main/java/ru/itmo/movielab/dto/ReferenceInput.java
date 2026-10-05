package ru.itmo.movielab.dto;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import ru.itmo.movielab.validation.ExactIntegerDeserializer;
import ru.itmo.movielab.validation.ExactLongDeserializer;

public class ReferenceInput {

    @JsonbTypeDeserializer(ExactLongDeserializer.class)
    public Long version;

    @JsonbTypeDeserializer(ExactIntegerDeserializer.class)
    public Integer x;

    public Double y;
    public Float z;
    public String name;
    public String eyeColor;
    public String hairColor;

    @JsonbTypeDeserializer(ExactLongDeserializer.class)
    public Long locationId;

    public Double height;
    public String nationality;
}
