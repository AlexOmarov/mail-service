package ru.somarov.mail.infrastructure.webflux

import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.http.client.reactive.ClientHttpRequestDecorator
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

class WebClientLoggingFilter : ExchangeFilterFunction {
    private val log = LoggerFactory.getLogger(this.javaClass)
    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val originalBodyInserter = request.body()
        val logBuilder = "Outgoing HTTP request <- ${request.method()} ${request.url()}: " +
            "headers=${request.headers().entries.map { it.toString() }}, " +
            "params=${request.attributes().entries.map { it.toString() }}"

        val loggingClientRequest = ClientRequest.from(request)
            .body { req, context -> originalBodyInserter.insert(LoggableDecorator(req, logBuilder), context) }
            .build()

        return next.exchange(loggingClientRequest)
            .map { clientResponse ->
                clientResponse.mutate()
                    .body { flux ->
                        flux.map { dataBuffer ->
                            log.info(
                                "Incoming HTTP response -> ${request.method()} ${request.url()} ${
                                    clientResponse.statusCode().value()
                                }: " +
                                    "headers=${
                                        clientResponse.headers().asHttpHeaders().entries.map { it.toString() }
                                    }, " +
                                    "body=${dataBuffer.toString(StandardCharsets.UTF_8)}"
                            )
                            dataBuffer
                        }
                    }
                    .build()
            }
    }

    @Suppress("kotlin:S6508") // Spring based class, cannot change Void to Unit
    private class LoggableDecorator(req: ClientHttpRequest, private val baseLogMessage: String) :
        ClientHttpRequestDecorator(req) {

        private val log = LoggerFactory.getLogger(this.javaClass)
        private val alreadyLogged = AtomicBoolean(false)

        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            var bodyCopy = body
            val needToLog = alreadyLogged.compareAndSet(false, true)
            if (needToLog) {
                bodyCopy = DataBufferUtils.join(body)
                    .doOnNext { content ->
                        log.info(baseLogMessage + ",body=${content.toString(StandardCharsets.UTF_8)}")
                    }
            }
            return super.writeWith(bodyCopy)
        }

        override fun setComplete(): Mono<Void> { // This is for requests without body (e.g. GET).
            val needToLog = alreadyLogged.compareAndSet(false, true)
            if (needToLog) {
                log.info(baseLogMessage)
            }
            return super.setComplete()
        }
    }
}
