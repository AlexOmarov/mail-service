package ru.somarov.mail.infrastructure.http

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

class LoggingRequestDecorator internal constructor(
    delegate: ServerHttpRequest,
    private val logger: HttpLogger
) : ServerHttpRequestDecorator(delegate) {

    private val body: Flux<DataBuffer>?

    override fun getBody(): Flux<DataBuffer> = body!!

    init {
        body = super.getBody().publishOn(Schedulers.boundedElastic())
            .doOnNext { buffer ->
                val bodyStream = ByteArrayOutputStream()
                val channel = Channels.newChannel(bodyStream)
                buffer.readableByteBuffers().forEach { channel.write(it) }
                logger.logRequest(delegate, String(bodyStream.toByteArray()))
            }
    }
}
