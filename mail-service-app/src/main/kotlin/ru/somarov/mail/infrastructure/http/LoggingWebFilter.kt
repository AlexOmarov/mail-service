package ru.somarov.mail.infrastructure.http

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class LoggingWebFilter(private val filters: List<HttpLoggerFilter>) : WebFilter {

    @Suppress("kotlin:S6508")
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val logger = HttpLogger(filters)
        return Mono.just(LoggingWebExchange(logger, exchange))
            .map { logger.logRequest(it.request); it }
            .flatMap { chain.filter(it) }
            .contextCapture()
    }
}
