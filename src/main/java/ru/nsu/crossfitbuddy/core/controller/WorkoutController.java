package ru.nsu.crossfitbuddy.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.crossfitbuddy.auth.security.annotation.ActiveRoleIn;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutListResponse;
import ru.nsu.crossfitbuddy.core.service.WorkoutService;
import ru.nsu.crossfitbuddy.core.workout.WorkoutListRequest;
import ru.nsu.crossfitbuddy.core.workout.WorkoutSortingKey;

import java.time.LocalDate;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(Paths.WORKOUTS)
@RequiredArgsConstructor
public class WorkoutController {
    private final WorkoutService workouts;

    @GetMapping
    @Operation(summary = "Получить доступные тренировки",
        description = "TRAINER получает созданные им тренировки. ATHLETE получает собственные, "
            + "назначенные при ACTIVE-подписке и явно shared. Фильтры применяются совместно. "
            + "Дата — время создания; чужие черновики скрыты.")
    @ApiResponse(responseCode = "200", description = "Страница доступных совпадений")
    @ApiResponse(responseCode = "400", description = "Недопустимые параметры")
    @ApiResponse(responseCode = "401", description = "Требуется действующий JWT")
    @ApiResponse(responseCode = "403", description = "Активная роль не поддерживается")
    @ActiveRoleIn({UserRole.ATHLETE, UserRole.TRAINER})
    public ResponseEntity<WorkoutListResponse> list(
        @Parameter(hidden = true)
        @AuthenticationPrincipal
        CurrentUser user,

        @Parameter(description = "Все слова должны входить в название")
        @RequestParam(required = false)
        String q,

        @Parameter(description = "Статусы: любое из значений; параметр можно повторять")
        @RequestParam(required = false)
        List<WorkoutStatus> status,

        @Parameter(description = "Источники относительно пользователя; параметр можно повторять")
        @RequestParam(required = false)
        List<WorkoutType> source,

        @Parameter(description = "Первый день включительно, UTC, YYYY-MM-DD")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateFrom,

        @Parameter(description = "Последний день включительно, UTC, YYYY-MM-DD")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateTo,

        @Parameter(description = "Сортировка")
        @RequestParam(defaultValue = "DATE_DESC")
        WorkoutSortingKey sort,

        @Parameter(description = "Номер страницы с нуля")
        @Min(value = 0, message = "page должен быть неотрицательным")
        @RequestParam(defaultValue = "0")
        int page,

        @Parameter(description = "Размер страницы")
        @Min(value = 1, message = "size должен быть не меньше 1")
        @Max(value = 100, message = "size должен быть не больше 100")
        @RequestParam(defaultValue = "20")
        int size
    ) {
        var request = new WorkoutListRequest(
            q,
            status,
            source,
            dateFrom,
            dateTo,
            sort,
            page,
            size
        );
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(workouts.list(user, request));
    }
}
