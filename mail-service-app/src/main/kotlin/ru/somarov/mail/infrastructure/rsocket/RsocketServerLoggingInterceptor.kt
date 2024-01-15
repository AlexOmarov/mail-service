package ru.somarov.mail.infrastructure.rsocket

import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.plugins.RSocketInterceptor
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

class RsocketServerLoggingInterceptor : RSocketInterceptor {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun apply(rSocket: RSocket): RSocket {
        return object : RSocket {
            override fun requestResponse(payload: Payload): Mono<Payload> {
                logRequest(payload)
                return rSocket.requestResponse(payload)
                    .doOnSuccess { response -> logResponse(response) }
            }

            private fun logRequest(payload: Payload) {
                log.info("Incoming rsocket request -> payload: ${payload.dataUtf8}, metadata: ${payload.metadataUtf8}")
            }

            private fun logResponse(response: Payload) {
                log.info(
                    "Outgoing rsocket response <- " +
                        "payload: ${response.dataUtf8}, metadata: ${response.metadataUtf8}"
                )
            }
        }
    }
}
