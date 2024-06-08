package ru.somarov.mail.infrastructure.http

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
private class LoggingWebFilter(private val filters: List<HttpLoggerFilter>) : WebFilter {

    @Suppress("kotlin:S6508") // Spring based class, cannot change Void to Unit
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val logger = HttpLogger(filters)
        val logExchange = LoggingWebExchange(logger, exchange)
        return Mono.just(logExchange)
            .map { logger.logRequest(it.request); it }
            .flatMap { chain.filter(it) }
            .doFinally { logger.logResponse(logExchange.request, logExchange.response, null) }
            .contextCapture()
    }
}
