package org.hms.patient.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String BASIC_SCHEME = "basicAuth";

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(BASIC_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                        )
                )

                .addSecurityItem(new SecurityRequirement().addList(BASIC_SCHEME));
    }
}
