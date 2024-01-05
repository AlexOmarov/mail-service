package ru.somarov.mail.infrastructure.config

import io.micrometer.observation.ObservationRegistry
import org.springframework.boot.actuate.metrics.web.reactive.client.ObservationWebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import ru.somarov.mail.infrastructure.webclient.WebClientLoggingFilter
import ru.somarov.mail.infrastructure.webclient.WebClientTracingFilter

@Configuration
private class WebClientConfig(private val registry: ObservationRegistry) {
    @Bean
    fun createWebClient(): WebClient.Builder {
        val result = WebClient.builder()
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create(
                        ConnectionProvider.builder("connection-provider").build()
                    )
                )
            )
            .filter(WebClientLoggingFilter())
            .filter(WebClientTracingFilter(registry))

        ObservationWebClientCustomizer(registry, DefaultClientRequestObservationConvention()).customize(result)

        return result
    }
}
