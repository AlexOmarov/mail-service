package ru.somarov.mail.infrastructure.http

import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.util.StringUtils
import java.util.Optional

class HttpLogger(private val filters: List<HttpLoggerFilter>) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun logRequest(request: ServerHttpRequest, body: String? = null) {
        val path = request.uri.path
        var preparedBody = body
        val matchedFilters = filters.filter { it.pattern().matches(path) }

        val isRequestLoggable = shouldLog(filters = matchedFilters, path = path, request = request, body = preparedBody)
        if (!isRequestLoggable) {
            return
        }

        matchedFilters.forEach { preparedBody = it.prepareRequestBody(preparedBody) }
        val method = Optional.ofNullable(request.method).orElse(HttpMethod.GET).name()
        val headers = request.headers
        val params = request.queryParams
        val fullPath = getFullPath(request.uri.query ?: "", path)

        if (preparedBody != null) {
            log.info(
                "Body of incoming HTTP request $method $fullPath: headers=$headers, params=$params, body=$preparedBody"
            )
        } else {
            log.info("Incoming HTTP request -> $method $fullPath: headers=$headers, params=$params")
        }
    }

    fun logResponse(request: ServerHttpRequest, response: ServerHttpResponse, body: String? = null) {
        val path = request.uri.path
        val status = response.statusCode
        var preparedBody = body
        val matchedFilters = filters.filter { it.pattern().matches(path) }
        val isRequestLoggable = shouldLog(filters = matchedFilters, path = path, request = request, body = preparedBody)
        if (!isRequestLoggable) {
            return
        }
        matchedFilters.forEach { preparedBody = it.prepareResponseBody(preparedBody) }
        val method = Optional.ofNullable(request.method).orElse(HttpMethod.GET).name()
        val headers = response.headers
        val fullPath = getFullPath(request.uri.query ?: "", path)

        log.info("Outgoing HTTP response <- $method $fullPath $status: headers=$headers, body=$preparedBody")
    }

    private fun excluded(path: String): Boolean {
        return Regex("/actuator.*").matches(path)
    }

    private fun getFullPath(query: String, path: String): String {
        return path + (if (StringUtils.hasText(query)) "?$query" else "")
    }

    private fun shouldLog(
        filters: List<HttpLoggerFilter>?,
        path: String,
        request: ServerHttpRequest,
        body: String?
    ): Boolean {
        if (excluded(path)) {
            return false
        }
        filters?.forEach {
            if (it.skipRequestLogging(request, body)) {
                return false
            }
        }
        return true
    }
}
