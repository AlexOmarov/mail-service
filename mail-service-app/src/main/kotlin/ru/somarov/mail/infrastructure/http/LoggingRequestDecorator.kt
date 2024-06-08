package ru.somarov.mail.infrastructure.http

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

internal class LoggingRequestDecorator(
    delegate: ServerHttpRequest,
    private val logger: HttpLogger
) : ServerHttpRequestDecorator(delegate) {

    override fun getBody(): Flux<DataBuffer> = super
        .getBody()
        .publishOn(Schedulers.boundedElastic())
        .doOnNext { buffer ->
            // Here can be several log messages if body is huge
            val bodyStream = ByteArrayOutputStream()
            val channel = Channels.newChannel(bodyStream)
            buffer.readableByteBuffers().forEach { channel.write(it) }
            channel.close()
            logger.logRequest(delegate, String(bodyStream.toByteArray()))
            bodyStream.close()
        }
}
