package ru.nsu.crossfitbuddy.core.exception.exercises;

import org.springframework.http.HttpStatus;

import ru.nsu.crossfitbuddy.core.exception.ApiException;

public class DuplicateMetricsNameExeption extends ApiException {

    public DuplicateMetricsNameExeption() {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Нельзя создать упражнение с несколькими одинаковыми именами метрик");

    }

}
