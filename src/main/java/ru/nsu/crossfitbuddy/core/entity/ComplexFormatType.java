package ru.nsu.crossfitbuddy.core.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ComplexFormatType {
    EMOM,
    FOR_TIME,
    AMRAP,
    FREE;

    @JsonCreator
    public static ComplexFormatType fromValue(String value) {
        return ComplexFormatType.valueOf(value);
    }
}
