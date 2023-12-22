package ru.somarov.mail.infrastructure.http

import org.springframework.http.server.reactive.ServerHttpRequest

interface HttpLoggerFilter {
    fun pattern(): Regex
    fun prepareRequestBody(body: String?): String?
    fun prepareResponseBody(body: String?): String?
    fun skipRequestLogging(request: ServerHttpRequest, body: String?): Boolean
}
