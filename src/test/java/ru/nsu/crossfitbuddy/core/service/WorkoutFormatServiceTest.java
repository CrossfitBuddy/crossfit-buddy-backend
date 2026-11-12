package ru.nsu.crossfitbuddy.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormat;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormatType;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mapper.WorkoutFormatMapper;
import ru.nsu.crossfitbuddy.core.repository.WorkoutFormatRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutFormatServiceTest {

    @Mock
    private WorkoutFormatRepository workoutFormatRepository;

    @Mock
    private WorkoutFormatMapper workoutFormatMapper;

    @InjectMocks
    private WorkoutFormatService workoutFormatService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                workoutFormatRepository,
                workoutFormatMapper
        );
    }

    @Test
    void getAll_shouldReturnMappedResponses() {
        WorkoutFormat emom = createFormat(WorkoutFormatType.EMOM, "EMOM description");
        WorkoutFormat amrap = createFormat(WorkoutFormatType.AMRAP, "AMRAP description");

        WorkoutFormatResponse emomResponse = createResponse(
                WorkoutFormatType.EMOM,
                "EMOM description"
        );
        WorkoutFormatResponse amrapResponse = createResponse(
                WorkoutFormatType.AMRAP,
                "AMRAP description"
        );

        when(workoutFormatRepository.findAll())
                .thenReturn(List.of(emom, amrap));

        when(workoutFormatMapper.toResponse(emom))
                .thenReturn(emomResponse);

        when(workoutFormatMapper.toResponse(amrap))
                .thenReturn(amrapResponse);

        List<WorkoutFormatResponse> result = workoutFormatService.getAll();

        assertThat(result)
                .containsExactly(emomResponse, amrapResponse);

        verify(workoutFormatRepository).findAll();
        verify(workoutFormatMapper).toResponse(emom);
        verify(workoutFormatMapper).toResponse(amrap);
    }

    @Test
    void getAll_whenNoFormats_shouldReturnEmptyList() {
        when(workoutFormatRepository.findAll())
                .thenReturn(List.of());

        List<WorkoutFormatResponse> result = workoutFormatService.getAll();

        assertThat(result).isEmpty();

        verify(workoutFormatRepository).findAll();
        verifyNoInteractions(workoutFormatMapper);
    }

    @Test
    void updateDescription_shouldUpdateEntityAndReturnResponse() {
        WorkoutFormatType type = WorkoutFormatType.EMOM;
        String newDescription = "Новое описание";

        WorkoutFormat format = createFormat(type, "Старое описание");

        WorkoutFormatResponse expectedResponse = createResponse(type, newDescription);

        when(workoutFormatRepository.findById(type))
                .thenReturn(Optional.of(format));

        when(workoutFormatRepository.save(format))
                .thenReturn(format);

        when(workoutFormatMapper.toResponse(format))
                .thenReturn(expectedResponse);

        WorkoutFormatResponse result = workoutFormatService.updateDescription(type, newDescription);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(format.getDescription()).isEqualTo(newDescription);

        verify(workoutFormatRepository).findById(type);
        verify(workoutFormatRepository).save(format);
        verify(workoutFormatMapper).toResponse(format);
    }

    @Test
    void updateDescription_whenFormatNotFound_shouldThrowException() {
        WorkoutFormatType type = WorkoutFormatType.EMOM;
        String newDescription = "Новое описание";

        when(workoutFormatRepository.findById(type))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                workoutFormatService.updateDescription(type, newDescription)
        )
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(type.name());

        verify(workoutFormatRepository).findById(type);
        verify(workoutFormatRepository, never()).save(any());
        verifyNoInteractions(workoutFormatMapper);
    }

    private WorkoutFormat createFormat(WorkoutFormatType type, String description) {
        WorkoutFormat format = new WorkoutFormat();
        format.setType(type);
        format.setName(type.name());
        format.setDescription(description);
        format.setDurationRequired(true);
        format.setIntervalRequired(false);
        return format;
    }

    private WorkoutFormatResponse createResponse(WorkoutFormatType type, String description) {
        return new WorkoutFormatResponse(
                type,
                type.name(),
                description,
                true,
                false
        );
    }
}