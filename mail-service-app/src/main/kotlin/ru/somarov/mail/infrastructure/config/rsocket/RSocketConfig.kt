package ru.somarov.mail.infrastructure.config.rsocket

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.ObservationRegistry
import io.rsocket.core.RSocketConnector
import io.rsocket.loadbalance.LoadbalanceRSocketClient
import io.rsocket.micrometer.MicrometerRSocketInterceptor
import io.rsocket.micrometer.observation.ObservationRequesterRSocketProxy
import io.rsocket.micrometer.observation.ObservationResponderRSocketProxy
import io.rsocket.plugins.RSocketInterceptor
import org.springframework.boot.rsocket.server.RSocketServerCustomizer
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.codec.cbor.KotlinSerializationCborDecoder
import org.springframework.http.codec.cbor.KotlinSerializationCborEncoder
import org.springframework.messaging.handler.MessagingAdviceBean
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import org.springframework.web.method.ControllerAdviceBean
import reactor.util.retry.Retry
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.rsocket.RsocketClientLoggingInterceptor
import ru.somarov.mail.infrastructure.rsocket.RsocketServerLoggingInterceptor
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
                it.forResponder(RsocketServerLoggingInterceptor())
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
    fun messageHandler(context: ApplicationContext): RSocketMessageHandler {
        val handler = RSocketMessageHandler()
        ControllerAdviceBean.findAnnotatedBeans(context).forEach {
            handler.registerMessagingAdvice(ControllerAdviceWrapper(it))
        }
        handler.rSocketStrategies = RSocketStrategies.builder()
            .encoders { it.add(KotlinSerializationCborEncoder()) }
            .decoders { it.add(KotlinSerializationCborDecoder()) }
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
                        it.forRequester(RsocketClientLoggingInterceptor())
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
            .dataMimeType(MediaType.APPLICATION_CBOR)
            .rsocketStrategies(
                RSocketStrategies.builder()
                    .encoders { it.addAll(listOf(KotlinSerializationCborEncoder(), SimpleAuthenticationEncoder())) }
                    .decoders { it.add(KotlinSerializationCborDecoder()) }
                    .build()
            )
            .websocket(URI.create(props.contour.rsocket.uri))
    }

    private class ControllerAdviceWrapper(private val delegate: ControllerAdviceBean) : MessagingAdviceBean {
        override fun getOrder() = delegate.order

        override fun getBeanType(): Class<*>? = delegate.beanType

        override fun resolveBean(): Any = delegate.resolveBean()

        override fun isApplicableToBeanType(beanType: Class<*>) = delegate.isApplicableToBeanType(beanType)
    }
}
