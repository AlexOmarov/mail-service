package ru.somarov.mail.infrastructure.http

import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator

internal class LoggingWebExchange(
    logger: HttpLogger,
    delegate: ServerWebExchange
) : ServerWebExchangeDecorator(delegate) {

    private val requestDecorator = LoggingRequestDecorator(delegate.request, logger)

    private val responseDecorator = LoggingResponseDecorator(delegate.request, delegate.response, logger)

    override fun getRequest(): ServerHttpRequest = requestDecorator

    override fun getResponse(): ServerHttpResponse = responseDecorator
}
