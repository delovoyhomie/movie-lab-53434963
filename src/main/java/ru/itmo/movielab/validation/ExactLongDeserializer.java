package ru.itmo.movielab.validation;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.math.BigDecimal;

// JSON-B по умолчанию может усекать дробные числа при преобразовании в Integer/Long.
public class ExactLongDeserializer implements JsonbDeserializer<Long> {

    public Long deserialize(JsonParser parser, DeserializationContext context, Type type) {
        try {
            var value = parser.getValue();
            if (value == jakarta.json.JsonValue.NULL) {
                return null;
            }
            String number =
                value instanceof jakarta.json.JsonString str ? str.getString() : value.toString();
            return new BigDecimal(number).longValueExact();
        } catch (RuntimeException e) {
            throw new JsonbException("Ожидается целое число в диапазоне Long", e);
        }
    }
}
