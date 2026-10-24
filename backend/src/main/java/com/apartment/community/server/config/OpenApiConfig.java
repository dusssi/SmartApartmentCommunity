package com.apartment.community.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI communityOpenApi() {
        String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("Smart Apartment Community API").version("v1")
                        .description("Community-scoped operations API. Roles are resolved from server-side memberships."))
                .components(new Components().addSecuritySchemes(bearer,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(bearer));
    }
}
