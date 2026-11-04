package ru.nsu.crossfitbuddy.core.exception.exercises;

import org.springframework.http.HttpStatus;

import ru.nsu.crossfitbuddy.core.exception.ServiceException;

public class DuplicateMetricsNameExeption extends ServiceException {

    public DuplicateMetricsNameExeption() {
        super(HttpStatus.BAD_REQUEST,
                "Нельзя создать упражнение с несколькими одинаковыми именами метрик");

    }

}
