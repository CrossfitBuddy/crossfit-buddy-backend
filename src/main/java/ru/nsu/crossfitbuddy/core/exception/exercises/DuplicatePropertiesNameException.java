package ru.nsu.crossfitbuddy.core.exception.exercises;

import org.springframework.http.HttpStatus;

import ru.nsu.crossfitbuddy.core.exception.ApiException;

public class DuplicatePropertiesNameException extends ApiException {

  public DuplicatePropertiesNameException() {
    super(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
        "Нельзя создать свойства упражнения с одинаковыми именами");

  }

}
