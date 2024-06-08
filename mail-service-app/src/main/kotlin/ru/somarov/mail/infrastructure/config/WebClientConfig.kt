package ru.somarov.mail.infrastructure.config

import io.micrometer.observation.ObservationRegistry
import io.netty.channel.ChannelOption
import org.springframework.boot.actuate.metrics.web.reactive.client.ObservationWebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.SslProvider
import ru.somarov.mail.infrastructure.webclient.WebClientLoggingFilter
import ru.somarov.mail.infrastructure.webclient.WebClientTracingFilter
import java.time.Duration

@Configuration
private class WebClientConfig(private val registry: ObservationRegistry, private val props: ServiceProps) {
    @Bean
    fun createWebClient(): WebClient.Builder {
        val client = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.contour.http.client.connectionTimeoutMillis)
            .responseTimeout(Duration.ofMillis(props.contour.http.client.connectionTimeoutMillis.toLong()))
            .secure(SslProvider.defaultClientProvider())

        val result = WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(client))
            .filter(WebClientLoggingFilter())
            .filter(WebClientTracingFilter(registry))

        ObservationWebClientCustomizer(registry, DefaultClientRequestObservationConvention()).customize(result)

        return result
    }
}
