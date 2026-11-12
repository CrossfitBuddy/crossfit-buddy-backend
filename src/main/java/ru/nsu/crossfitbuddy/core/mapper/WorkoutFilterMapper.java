package ru.nsu.crossfitbuddy.core.mapper;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.workout.WorkoutFilter;
import ru.nsu.crossfitbuddy.core.workout.WorkoutListRequest;
import ru.nsu.crossfitbuddy.core.workout.WorkoutSortingKey;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WorkoutFilterMapper {
    public WorkoutFilter toFilter(WorkoutListRequest request) {
        String q = request.q();
        int page = request.page();
        int size = request.size();
        var statuses = request.statuses();
        var sources = request.sources();
        var dateFrom = request.dateFrom();
        var dateTo = request.dateTo();
        WorkoutSortingKey sort = request.sort();
        String search = q == null ? "" : q;
        List<String> words = Arrays.stream(search.split("(?U)\\s+"))
            .filter(word -> !word.isBlank()).toList();
        if ((long) page * size > Integer.MAX_VALUE) throw invalid("Слишком большое смещение страницы.");
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw invalid("dateFrom должен быть не позже dateTo.");
        }
        return new WorkoutFilter(
            words.stream().distinct().map(WorkoutFilterMapper::searchPattern).toList(),
            nonNullValues(statuses),
            nonNullValues(sources),
            dateFrom == null ? null : dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant(),
            dateTo == null ? null : dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
            sort,
            page,
            size
        );
    }

    private static String searchPattern(String word) {
        return "%" + word.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    private static <T> Set<T> nonNullValues(List<T> values) {
        return values == null ? Set.of() : values.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static ApiException invalid(String message) {
        return new ApiException(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            message
        );
    }
}
