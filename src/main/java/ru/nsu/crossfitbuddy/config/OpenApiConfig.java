package ru.nsu.crossfitbuddy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI crossfitBuddyOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CrossfitBuddy API")
                .version("v1")
                .description("REST API for athlete, trainer and administrator scenarios"));
    }
}
