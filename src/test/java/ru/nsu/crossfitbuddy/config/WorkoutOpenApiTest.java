package ru.nsu.crossfitbuddy.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class WorkoutOpenApiTest {
    @Test
    @DisplayName("Настройка OpenAPI")
    void configuresBearerAuthenticationWithoutAdvertisingUnimplementedEndpoints() {
        var api = new OpenApiConfig().crossfitBuddyOpenApi();
        assertThat(api.getComponents().getSecuritySchemes().get("bearerAuth").getScheme())
            .isEqualTo("bearer");
        assertThat(api.getInfo().getVersion()).isEqualTo("v1");
        assertThat(api.getPaths()).isNull();
    }
}
