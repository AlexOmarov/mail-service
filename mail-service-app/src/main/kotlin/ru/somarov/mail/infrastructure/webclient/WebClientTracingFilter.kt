package ru.somarov.mail.infrastructure.webclient

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class WebClientTracingFilter(private val registry: ObservationRegistry) : ExchangeFilterFunction {
    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        return next.exchange(request)
            .map { clientResponse -> clientResponse.mutate().body { flux -> addObservation(flux) }.build() }
    }

    private fun addObservation(flux: Flux<DataBuffer>): Flux<DataBuffer> {
        val obs = registry.currentObservation ?: Observation.createNotStarted("WebClientTracing", registry)
        return flux.contextWrite { it.put(ObservationThreadLocalAccessor.KEY, obs) }
    }
}
