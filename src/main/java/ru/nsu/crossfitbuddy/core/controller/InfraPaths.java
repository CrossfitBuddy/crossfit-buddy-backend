package ru.nsu.crossfitbuddy.core.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InfraPaths {
    // actuator
    public static final String ACTUATOR_HEALTH = "/actuator/health";

    // swagger / openApi
    public static final String SWAGGER_UI = "/swagger-ui";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String SWAGGER_RESOURCES = "/swagger-resources";
    public static final String V3_API_DOCS = "/v3/api-docs";

}
