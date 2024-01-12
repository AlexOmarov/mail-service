package ru.somarov.mail.infrastructure.config.rsocket

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.ObservationRegistry
import io.rsocket.core.RSocketConnector
import io.rsocket.micrometer.MicrometerRSocketInterceptor
import io.rsocket.micrometer.observation.ObservationRequesterRSocketProxy
import io.rsocket.micrometer.observation.ObservationResponderRSocketProxy
import io.rsocket.plugins.RSocketInterceptor
import org.springframework.boot.rsocket.server.RSocketServerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import reactor.util.retry.Retry
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.hessian.HessianCodecSupport.Companion.HESSIAN_MIME_TYPE
import ru.somarov.mail.infrastructure.hessian.impl.HessianDecoder
import ru.somarov.mail.infrastructure.hessian.impl.HessianEncoder
import java.net.URI
import java.time.Duration

@Configuration
private class RSocketConfig(private val props: ServiceProps) {

    @Bean
    fun rsocketServerCustomizer(
        meterRegistry: MeterRegistry,
        observationRegistry: ObservationRegistry
    ): RSocketServerCustomizer {
        return RSocketServerCustomizer { server ->
            server.interceptors {
                it.forResponder(MicrometerRSocketInterceptor(meterRegistry))
                it.forResponder(RSocketInterceptor { socket ->
                    ObservationResponderRSocketProxy(
                        socket,
                        observationRegistry
                    )
                })
            }
        }
    }

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
    fun rSocketRequester(meterRegistry: MeterRegistry, observationRegistry: ObservationRegistry): RSocketRequester {
        val builder = RSocketRequester.builder()
        return builder
            .rsocketConnector { rSocketConnector: RSocketConnector ->
                rSocketConnector
                    .interceptors {
                        it.forRequester(MicrometerRSocketInterceptor(meterRegistry))
                        it.forRequester(RSocketInterceptor { socket ->
                            ObservationRequesterRSocketProxy(
                                socket,
                                observationRegistry
                            )
                        })
                    }
                    .reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2)))
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
