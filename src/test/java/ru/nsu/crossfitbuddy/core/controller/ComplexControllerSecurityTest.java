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
import ru.nsu.crossfitbuddy.core.dto.request.UpdateComplexFormatRequest;
import ru.nsu.crossfitbuddy.core.dto.response.ComplexFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.exception.SecurityErrorHandler;
import ru.nsu.crossfitbuddy.core.repository.UserAccountRepository;
import ru.nsu.crossfitbuddy.core.service.AccountDetailsService;
import ru.nsu.crossfitbuddy.core.service.JwtService;
import ru.nsu.crossfitbuddy.core.service.ComplexFormatService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplexController.class)
@Import(SecurityConfig.class)
class ComplexControllerSecurityTest {

    private static final String PATH = Paths.COMPLEX + Paths.COMPLEX_FORMATS_BY_TYPE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComplexFormatService complexFormatService;

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
                complexFormatService,
                jwtService,
                userAccountRepository,
                securityErrorHandler,
                accountDetailsService
        );
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void adminCanUpdateComplexFormatDescription() throws Exception {
        ComplexFormatType type = ComplexFormatType.EMOM;
        String newDescription = "Новое описание";

        UpdateComplexFormatRequest request =
                new UpdateComplexFormatRequest(newDescription);

        ComplexFormatResponse response = new ComplexFormatResponse(
                type,
                type.name(),
                newDescription
        );

        when(complexFormatService.updateDescription(type, newDescription)).thenReturn(response);

        mockMvc.perform(patch(PATH, type.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(complexFormatService).updateDescription(type, request.description());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void userCannotUpdateComplexFormatDescription() throws Exception {
        UpdateComplexFormatRequest request =
                new UpdateComplexFormatRequest("Новое описание");

        ComplexFormatType type = ComplexFormatType.EMOM;

        mockMvc.perform(patch(PATH, type.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}