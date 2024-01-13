package ru.somarov.mail.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.reactive.ServerHttpRequest
import ru.somarov.mail.infrastructure.http.HttpLoggerFilter

@Configuration
private class HttpLoggingConfig {
    @Bean
    fun httpLoggingFilters(props: ServiceProps): List<HttpLoggerFilter> {
        return props.contour.http.logging.exclusions.map {
            object : HttpLoggerFilter {
                override fun pattern(): Regex {
                    return Regex(it)
                }

                override fun prepareRequestBody(body: String?): String? {
                    return body
                }

                override fun prepareResponseBody(body: String?): String? {
                    return body
                }

                override fun skipRequestLogging(request: ServerHttpRequest, body: String?): Boolean {
                    return true
                }
            }
        }
    }
}
