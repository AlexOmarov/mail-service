package ru.somarov.mail.infrastructure.rsocket

import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.plugins.RSocketInterceptor
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

class RsocketClientLoggingInterceptor : RSocketInterceptor {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun apply(rSocket: RSocket): RSocket {
        return object : RSocket {
            override fun requestResponse(payload: Payload): Mono<Payload> {
                logRequest(payload)
                return rSocket.requestResponse(payload)
                    .doOnSuccess { response -> logResponse(response) }
            }

            @Suppress("kotlin:S1135") // Should fix it soon
            private fun logRequest(payload: Payload) {
                // TODO: convert from hessian to json
                log.info("Outgoing rsocket request <- payload: ${payload.dataUtf8}, metadata: ${payload.metadataUtf8}")
            }

            private fun logResponse(response: Payload) {
                log.info(
                    "Incoming rsocket response -> " +
                        "payload: ${response.dataUtf8}, metadata: ${response.metadataUtf8}"
                )
            }
        }
    }
}
