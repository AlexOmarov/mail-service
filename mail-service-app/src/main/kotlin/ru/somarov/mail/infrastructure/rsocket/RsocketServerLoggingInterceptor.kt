package ru.somarov.mail.infrastructure.rsocket

import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.plugins.RSocketInterceptor
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import ru.somarov.mail.infrastructure.rsocket.RsocketUtil.getDeserializedPayload

class RsocketServerLoggingInterceptor : RSocketInterceptor {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun apply(rSocket: RSocket): RSocket {
        return object : RSocket {
            override fun requestResponse(payload: Payload): Mono<Payload> {
                val deserializedRequest = getDeserializedPayload(payload)
                log.info(
                    "Incoming rsocket request -> ${deserializedRequest.third}: " +
                        "payload: ${deserializedRequest.first}, metadata: ${deserializedRequest.second}"
                )
                return rSocket.requestResponse(payload)
                    .doOnSuccess {
                        val deserializedResponse = getDeserializedPayload(it)
                        log.info(
                            "Outgoing rsocket response <- ${deserializedRequest.third}: " +
                                "payload: ${deserializedResponse.first}, " +
                                "request metadata: ${deserializedRequest.second}, " +
                                "response metadata: ${deserializedResponse.second}"
                        )
                    }
            }
        }
    }
}
