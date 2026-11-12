package ru.nsu.crossfitbuddy.core.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "Доступная тренировка")
public record WorkoutSummaryResponse(
    @Schema(description = "Идентификатор тренировки")
    UUID id,

    @Schema(description = "Название", nullable = true)
    String title,

    @Schema(description = "Дата создания в UTC")
    Instant date,

    @Schema(description = "Общий статус тренировки")
    WorkoutStatus status,

    @Schema(description = "Источник относительно текущего пользователя")
    WorkoutType source
) {}
