package ru.somarov.mail.infrastructure.config

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
private class OtelAppenderConfig(private val sdk: OpenTelemetrySdk) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        OpenTelemetryAppender.install(sdk)
    }
}
