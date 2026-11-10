package ru.nsu.crossfitbuddy.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record WorkoutListResponse(
    @Schema(description = "Тренировки текущей страницы")
    List<WorkoutSummaryResponse> items,

    @Schema(description = "Номер страницы с нуля")
    int page,

    @Schema(description = "Размер страницы")
    int size,

    @Schema(description = "Количество доступных совпадений")
    long totalElements,

    @Schema(description = "Количество страниц")
    int totalPages
) {}
