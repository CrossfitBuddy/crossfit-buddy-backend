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
import ru.nsu.crossfitbuddy.core.dto.request.UpdateWorkoutFormatRequest;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormatType;
import ru.nsu.crossfitbuddy.core.security.JwtAuthenticationFilter;
import ru.nsu.crossfitbuddy.core.service.JwtService;
import ru.nsu.crossfitbuddy.core.service.WorkoutFormatService;

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

@WebMvcTest(WorkoutController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkoutControllerTest {

    private static final String FORMATS_URL = Paths.WORKOUTS + Paths.WORKOUTS_FORMATS;
    private static final String FORMAT_BY_TYPE_URL = Paths.WORKOUTS + Paths.WORKOUTS_FORMATS_BY_TYPE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkoutFormatService workoutFormatService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                workoutFormatService,
                jwtService,
                jwtAuthenticationFilter
        );
    }

    @Test
    void getAllWorkoutFormats_shouldReturnOkAndList() throws Exception {
        WorkoutFormatResponse emomResponse = new WorkoutFormatResponse(
                WorkoutFormatType.EMOM,
                "EMOM",
                "Описание EMOM",
                true,
                false
        );

        WorkoutFormatResponse amrapResponse = new WorkoutFormatResponse(
                WorkoutFormatType.AMRAP,
                "AMRAP",
                "Описание AMRAP",
                false,
                false
        );

        when(workoutFormatService.getAll())
                .thenReturn(List.of(emomResponse, amrapResponse));

        mockMvc.perform(get(FORMATS_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("EMOM"))
                .andExpect(jsonPath("$[0].name").value("EMOM"))
                .andExpect(jsonPath("$[0].description").value("Описание EMOM"))
                .andExpect(jsonPath("$[0].durationRequired").value(true))
                .andExpect(jsonPath("$[0].intervalRequired").value(false))
                .andExpect(jsonPath("$[1].type").value("AMRAP"))
                .andExpect(jsonPath("$[1].name").value("AMRAP"))
                .andExpect(jsonPath("$[1].description").value("Описание AMRAP"));

        verify(workoutFormatService).getAll();
    }

    @Test
    void getAllWorkoutFormats_whenEmpty_shouldReturnOkAndEmptyList() throws Exception {
        when(workoutFormatService.getAll())
                .thenReturn(List.of());

        mockMvc.perform(get(FORMATS_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(workoutFormatService).getAll();
    }

    @Test
    void updateWorkoutFormatDescription_shouldReturnOkAndUpdatedResponse() throws Exception {
        WorkoutFormatType type = WorkoutFormatType.EMOM;
        String newDescription = "Новое описание";

        UpdateWorkoutFormatRequest request =
                new UpdateWorkoutFormatRequest(newDescription);

        WorkoutFormatResponse response = new WorkoutFormatResponse(
                type,
                "EMOM",
                newDescription,
                true,
                false
        );

        when(workoutFormatService.updateDescription(type, newDescription))
                .thenReturn(response);

        mockMvc.perform(patch(FORMAT_BY_TYPE_URL, type)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("EMOM"))
                .andExpect(jsonPath("$.name").value("EMOM"))
                .andExpect(jsonPath("$.description").value(newDescription))
                .andExpect(jsonPath("$.durationRequired").value(true))
                .andExpect(jsonPath("$.intervalRequired").value(false));

        verify(workoutFormatService).updateDescription(type, newDescription);
    }

    @Test
    void updateWorkoutFormatDescription_whenDescriptionIsBlank_shouldReturnBadRequest() throws Exception {
        WorkoutFormatType type = WorkoutFormatType.EMOM;

        UpdateWorkoutFormatRequest request =
                new UpdateWorkoutFormatRequest("   ");

        mockMvc.perform(patch(FORMAT_BY_TYPE_URL, type)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(workoutFormatService);
    }

    @Test
    void updateWorkoutFormatDescription_whenDescriptionIsNull_shouldReturnBadRequest() throws Exception {
        WorkoutFormatType type = WorkoutFormatType.EMOM;

        String body = """
                {
                  "description": null
                }
                """;

        mockMvc.perform(patch(FORMAT_BY_TYPE_URL, type)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(workoutFormatService);
    }
}