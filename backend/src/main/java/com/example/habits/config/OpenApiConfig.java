package com.example.habits.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Minimal OpenAPI metadata; springdoc serves /v3/api-docs + /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Habit Tracker API")
                .version("v1")
                .description("""
                        Session-authenticated REST API (base path /api/v1).
                        CSRF: fetch GET /api/v1/auth/csrf, then send the raw
                        XSRF-TOKEN cookie value in the X-XSRF-TOKEN header on
                        unsafe methods."""));
    }
}
