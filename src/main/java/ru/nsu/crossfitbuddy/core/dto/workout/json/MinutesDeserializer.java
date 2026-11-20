package ru.nsu.crossfitbuddy.core.dto.workout.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

public final class MinutesDeserializer extends StdDeserializer<BigDecimal> {
    public MinutesDeserializer() {
        super(BigDecimal.class);
    }

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context)
    throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)
            && !parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
            return (BigDecimal) context.handleUnexpectedToken(BigDecimal.class, parser);
        }
        return parser.getDecimalValue();
    }
}
