package ru.nsu.crossfitbuddy.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.nsu.crossfitbuddy.core.dto.request.UpdateComplexFormatRequest;
import ru.nsu.crossfitbuddy.core.dto.response.ComplexFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.security.JwtAuthenticationFilter;
import ru.nsu.crossfitbuddy.core.service.JwtService;
import ru.nsu.crossfitbuddy.core.service.ComplexFormatService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplexController.class)
@AutoConfigureMockMvc(addFilters = false)
class ComplexControllerTest {

    private static final String FORMATS_URL = Paths.COMPLEX + Paths.COMPLEX_FORMATS;
    private static final String FORMAT_BY_TYPE_URL = Paths.COMPLEX + Paths.COMPLEX_FORMATS_BY_TYPE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ComplexFormatService complexFormatService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                complexFormatService,
                jwtService,
                jwtAuthenticationFilter
        );
    }

    @Test
    void getAllComplexFormats_shouldReturnOkAndList() throws Exception {
        ComplexFormatResponse emomResponse = new ComplexFormatResponse(
                ComplexFormatType.EMOM,
                "EMOM",
                "Описание EMOM"
        );

        ComplexFormatResponse amrapResponse = new ComplexFormatResponse(
                ComplexFormatType.AMRAP,
                "AMRAP",
                "Описание AMRAP"
        );

        when(complexFormatService.getAll())
                .thenReturn(List.of(emomResponse, amrapResponse));

        mockMvc.perform(get(FORMATS_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("EMOM"))
                .andExpect(jsonPath("$[0].name").value("EMOM"))
                .andExpect(jsonPath("$[0].description").value("Описание EMOM"))
                .andExpect(jsonPath("$[1].type").value("AMRAP"))
                .andExpect(jsonPath("$[1].name").value("AMRAP"))
                .andExpect(jsonPath("$[1].description").value("Описание AMRAP"));

        verify(complexFormatService).getAll();
    }

    @Test
    void getAllComplexFormats_whenEmpty_shouldReturnOkAndEmptyList() throws Exception {
        when(complexFormatService.getAll())
                .thenReturn(List.of());

        mockMvc.perform(get(FORMATS_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(complexFormatService).getAll();
    }

    @Test
    void updateComplexFormatDescription_shouldReturnOkAndUpdatedResponse() throws Exception {
        ComplexFormatType type = ComplexFormatType.EMOM;
        String newDescription = "Новое описание";

        UpdateComplexFormatRequest request =
                new UpdateComplexFormatRequest(newDescription);

        ComplexFormatResponse response = new ComplexFormatResponse(
                type,
                "EMOM",
                newDescription
        );

        when(complexFormatService.updateDescription(type, newDescription))
                .thenReturn(response);

        mockMvc.perform(patch(FORMAT_BY_TYPE_URL, type)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("EMOM"))
                .andExpect(jsonPath("$.name").value("EMOM"))
                .andExpect(jsonPath("$.description").value(newDescription));

        verify(complexFormatService).updateDescription(type, newDescription);
    }

    @Test
    void updateComplexFormatDescription_whenDescriptionIsBlank_shouldReturnBadRequest() throws Exception {
        ComplexFormatType type = ComplexFormatType.EMOM;

        UpdateComplexFormatRequest request =
                new UpdateComplexFormatRequest("   ");

        mockMvc.perform(patch(FORMAT_BY_TYPE_URL, type)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(complexFormatService);
    }

    @Test
    void updateComplexFormatDescription_whenDescriptionIsNull_shouldReturnBadRequest() throws Exception {
        ComplexFormatType type = ComplexFormatType.EMOM;

        String body = """
                {
                  "description": null
                }
                """;

        mockMvc.perform(patch(FORMAT_BY_TYPE_URL, type)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(complexFormatService);
    }
}