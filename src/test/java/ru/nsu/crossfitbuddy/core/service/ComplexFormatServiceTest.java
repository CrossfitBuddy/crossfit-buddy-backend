package ru.nsu.crossfitbuddy.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.nsu.crossfitbuddy.core.dto.response.ComplexFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormat;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mapper.ComplexFormatMapper;
import ru.nsu.crossfitbuddy.core.repository.ComplexFormatRepository;

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
class ComplexFormatServiceTest {

    @Mock
    private ComplexFormatRepository complexFormatRepository;

    @Mock
    private ComplexFormatMapper complexFormatMapper;

    @InjectMocks
    private ComplexFormatService complexFormatService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                complexFormatRepository,
                complexFormatMapper
        );
    }

    @Test
    void getAll_shouldReturnMappedResponses() {
        ComplexFormat emom = createFormat(ComplexFormatType.EMOM, "EMOM description");
        ComplexFormat amrap = createFormat(ComplexFormatType.AMRAP, "AMRAP description");

        ComplexFormatResponse emomResponse = createResponse(
                ComplexFormatType.EMOM,
                "EMOM description"
        );
        ComplexFormatResponse amrapResponse = createResponse(
                ComplexFormatType.AMRAP,
                "AMRAP description"
        );

        when(complexFormatRepository.findAll())
                .thenReturn(List.of(emom, amrap));

        when(complexFormatMapper.toResponse(emom))
                .thenReturn(emomResponse);

        when(complexFormatMapper.toResponse(amrap))
                .thenReturn(amrapResponse);

        List<ComplexFormatResponse> result = complexFormatService.getAll();

        assertThat(result)
                .containsExactly(emomResponse, amrapResponse);

        verify(complexFormatRepository).findAll();
        verify(complexFormatMapper).toResponse(emom);
        verify(complexFormatMapper).toResponse(amrap);
    }

    @Test
    void getAll_whenNoFormats_shouldReturnEmptyList() {
        when(complexFormatRepository.findAll())
                .thenReturn(List.of());

        List<ComplexFormatResponse> result = complexFormatService.getAll();

        assertThat(result).isEmpty();

        verify(complexFormatRepository).findAll();
        verifyNoInteractions(complexFormatMapper);
    }

    @Test
    void updateDescription_shouldUpdateEntityAndReturnResponse() {
        ComplexFormatType type = ComplexFormatType.EMOM;
        String newDescription = "Новое описание";

        ComplexFormat format = createFormat(type, "Старое описание");

        ComplexFormatResponse expectedResponse = createResponse(type, newDescription);

        when(complexFormatRepository.findById(type))
                .thenReturn(Optional.of(format));

        when(complexFormatRepository.save(format))
                .thenReturn(format);

        when(complexFormatMapper.toResponse(format))
                .thenReturn(expectedResponse);

        ComplexFormatResponse result = complexFormatService.updateDescription(type, newDescription);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(format.getDescription()).isEqualTo(newDescription);

        verify(complexFormatRepository).findById(type);
        verify(complexFormatRepository).save(format);
        verify(complexFormatMapper).toResponse(format);
    }

    @Test
    void updateDescription_whenFormatNotFound_shouldThrowException() {
        ComplexFormatType type = ComplexFormatType.EMOM;
        String newDescription = "Новое описание";

        when(complexFormatRepository.findById(type))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                complexFormatService.updateDescription(type, newDescription)
        )
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(type.name());

        verify(complexFormatRepository).findById(type);
        verify(complexFormatRepository, never()).save(any());
        verifyNoInteractions(complexFormatMapper);
    }

    private ComplexFormat createFormat(ComplexFormatType type, String description) {
        ComplexFormat format = new ComplexFormat();
        format.setType(type);
        format.setName(type.name());
        format.setDescription(description);
        return format;
    }

    private ComplexFormatResponse createResponse(ComplexFormatType type, String description) {
        return new ComplexFormatResponse(
                type,
                type.name(),
                description
        );
    }
}