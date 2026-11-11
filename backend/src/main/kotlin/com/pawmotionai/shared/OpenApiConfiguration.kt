package com.pawmotionai.shared

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    @Bean
    fun petlifeOpenApi(): OpenAPI = OpenAPI()
        .info(Info().title("PETLIFE API").version("v1").description("Owner, partner and admin APIs for PETLIFE."))
        .components(
            Components().addSecuritySchemes(
                "bearer",
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList("bearer"))
}
