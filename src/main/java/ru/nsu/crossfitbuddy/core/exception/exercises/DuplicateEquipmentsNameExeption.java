package ru.nsu.crossfitbuddy.core.exception.exercises;

import org.springframework.http.HttpStatus;

import ru.nsu.crossfitbuddy.core.exception.ServiceException;

public class DuplicateEquipmentsNameExeption extends ServiceException {

    public DuplicateEquipmentsNameExeption() {
        super(HttpStatus.BAD_REQUEST,
                "Нельзя создать упражнение с несколькими одинаковыми именами оборудования ");

    }

}
