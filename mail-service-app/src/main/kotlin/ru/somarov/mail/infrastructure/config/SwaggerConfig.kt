package ru.somarov.mail.infrastructure.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@SecurityScheme(
    type = SecuritySchemeType.HTTP,
    name = "basicAuth",
    scheme = "basic")
private class SwaggerConfig {
    @Bean
    fun groupedApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("default")
            .pathsToMatch("/**")
            .build()
    }

    @Bean
    fun openApi(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Mail Service")
                    .description("API сервиса Mail Service")
            )
    }
}
