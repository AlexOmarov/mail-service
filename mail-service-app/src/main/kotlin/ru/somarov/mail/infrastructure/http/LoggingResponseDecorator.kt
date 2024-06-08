package ru.somarov.mail.infrastructure.http

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

internal class LoggingResponseDecorator internal constructor(
    private var request: ServerHttpRequest,
    delegate: ServerHttpResponse,
    private val httpLogger: HttpLogger
) : ServerHttpResponseDecorator(delegate) {
    @Suppress("kotlin:S6508") // Spring based class, cannot change Void to Unit
    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
        return super.writeWith(
            Flux.from(body)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext { buffer ->
                    // Here can be several log messages if body is huge
                    val bodyStream = ByteArrayOutputStream()
                    val channel = Channels.newChannel(bodyStream)
                    buffer.readableByteBuffers().forEach { channel.write(it) }
                    channel.close()
                    httpLogger.logResponse(request, delegate, String(bodyStream.toByteArray()))
                    bodyStream.close()
                }
        )
    }
}
