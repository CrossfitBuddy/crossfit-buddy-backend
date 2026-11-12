package ru.nsu.crossfitbuddy.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.nsu.crossfitbuddy.config.SecurityConfig;
import ru.nsu.crossfitbuddy.core.dto.request.UpdateWorkoutFormatRequest;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormatType;
import ru.nsu.crossfitbuddy.core.exception.SecurityErrorHandler;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.service.AccountDetailsService;
import ru.nsu.crossfitbuddy.core.service.JwtService;
import ru.nsu.crossfitbuddy.core.service.WorkoutFormatService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkoutController.class)
@Import(SecurityConfig.class)
class WorkoutControllerSecurityTest {

    private static final String PATH = Paths.WORKOUTS + Paths.WORKOUTS_FORMATS_BY_TYPE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkoutFormatService workoutFormatService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private SecurityErrorHandler securityErrorHandler;

    @MockitoBean
    private AccountDetailsService accountDetailsService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
                workoutFormatService,
                jwtService,
                userAccountRepository,
                securityErrorHandler,
                accountDetailsService
        );
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void adminCanUpdateWorkoutFormatDescription() throws Exception {
        WorkoutFormatType type = WorkoutFormatType.EMOM;
        String newDescription = "Новое описание";

        UpdateWorkoutFormatRequest request =
                new UpdateWorkoutFormatRequest(newDescription);

        WorkoutFormatResponse response = new WorkoutFormatResponse(
                type,
                type.name(),
                newDescription,
                true,
                false
        );

        when(workoutFormatService.updateDescription(type, newDescription)).thenReturn(response);

        mockMvc.perform(patch(PATH, type.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(workoutFormatService).updateDescription(type, request.description());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void userCannotUpdateWorkoutFormatDescription() throws Exception {
        UpdateWorkoutFormatRequest request =
                new UpdateWorkoutFormatRequest("Новое описание");

        WorkoutFormatType type = WorkoutFormatType.EMOM;

        mockMvc.perform(patch(PATH, type.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}