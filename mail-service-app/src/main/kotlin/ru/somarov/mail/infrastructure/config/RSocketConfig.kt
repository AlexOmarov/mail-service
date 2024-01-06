package ru.somarov.mail.infrastructure.config

import io.rsocket.core.RSocketConnector
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import reactor.util.retry.Retry
import ru.somarov.mail.infrastructure.hessian.HessianCodecSupport.Companion.HESSIAN_MIME_TYPE
import ru.somarov.mail.infrastructure.hessian.impl.HessianDecoder
import ru.somarov.mail.infrastructure.hessian.impl.HessianEncoder
import java.net.URI
import java.time.Duration

@Configuration
private class RSocketConfig(private val props: ServiceProps) {

    @Bean
    fun messageHandler(): RSocketMessageHandler {
        val handler = RSocketMessageHandler()
        handler.rSocketStrategies = RSocketStrategies.builder()
            .encoders { it.add(HessianEncoder()) }
            .decoders { it.add(HessianDecoder()) }
            .build()
        return handler
    }

    @Bean
    fun rSocketRequester(): RSocketRequester {
        val builder = RSocketRequester.builder()
        return builder
            .rsocketConnector { rSocketConnector: RSocketConnector ->
                rSocketConnector.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2)))
            }
            .dataMimeType(HESSIAN_MIME_TYPE)
            .rsocketStrategies(
                RSocketStrategies.builder()
                    .encoders { it.add(HessianEncoder()); it.add(SimpleAuthenticationEncoder()) }
                    .decoders { it.add(HessianDecoder()) }
                    .build()
            )
            .websocket(URI.create(props.contour.rsocket.uri))
    }
}
