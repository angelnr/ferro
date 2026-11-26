package com.ferro.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Metadatos de OpenAPI; Springdoc publica /api/api-docs y /api/docs. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI ferroOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ferro API")
                        .version("v1")
                        .description("Tracker de entrenamientos de fuerza. Errores RFC 7807 con code estable."))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
