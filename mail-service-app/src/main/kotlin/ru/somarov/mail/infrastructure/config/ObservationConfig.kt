package ru.somarov.mail.infrastructure.config

import io.micrometer.observation.ObservationRegistry
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import io.rsocket.micrometer.observation.ByteBufGetter
import io.rsocket.micrometer.observation.ByteBufSetter
import io.rsocket.micrometer.observation.RSocketRequesterTracingObservationHandler
import io.rsocket.micrometer.observation.RSocketResponderTracingObservationHandler
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
private class ObservationConfig(
    private val observationRegistry: ObservationRegistry,
    private val tracer: Tracer,
    private val propagator: Propagator
) {
    @PostConstruct
    fun init() {
        observationRegistry
            .observationConfig()
            .observationHandler(
                RSocketRequesterTracingObservationHandler(
                    tracer, propagator, ByteBufSetter(), false
                )
            )
            .observationHandler(
                RSocketResponderTracingObservationHandler(
                    tracer, propagator, ByteBufGetter(), false
                )
            )
    }
}