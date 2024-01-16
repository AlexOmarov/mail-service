package ru.somarov.mail.infrastructure.rsocket

import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.plugins.RSocketInterceptor
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import ru.somarov.mail.infrastructure.hessian.HessianCodecSupport

class RsocketClientLoggingInterceptor : RSocketInterceptor, RsocketPayloadDeserializer {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val codec = HessianCodecSupport()

    override fun apply(rSocket: RSocket): RSocket {
        return object : RSocket {
            override fun requestResponse(payload: Payload): Mono<Payload> {
                logRequest(payload)
                return rSocket.requestResponse(payload)
                    .doOnSuccess { response -> logResponse(response) }
            }

            private fun logRequest(payload: Payload) {
                val deserializedPayload = getDeserializedPayload(payload, codec)
                log.info("Outgoing rsocket request <- " +
                    "payload: ${deserializedPayload.first}, metadata: ${deserializedPayload.second}")
            }

            private fun logResponse(payload: Payload) {
                val deserializedPayload = getDeserializedPayload(payload, codec)
                log.info(
                    "Incoming rsocket response -> " +
                        "payload: ${deserializedPayload.first}, metadata: ${deserializedPayload.second}"
                )
            }
        }
    }
}
