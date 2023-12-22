package ru.somarov.mail.infrastructure.webflux

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

class WebClientTracingFilter(private val registry: ObservationRegistry) : ExchangeFilterFunction {
    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        return next.exchange(request)
            .map { clientResponse ->
                clientResponse.mutate()
                    .body { flux ->
                        val obs =
                            registry.currentObservation ?: Observation.createNotStarted("WebClientTracing", registry)
                        flux.contextWrite { it.put(ObservationThreadLocalAccessor.KEY, obs) }
                    }
                    .build()
            }
    }
}
