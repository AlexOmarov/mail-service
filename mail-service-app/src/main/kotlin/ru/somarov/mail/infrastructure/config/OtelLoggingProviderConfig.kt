package ru.somarov.mail.infrastructure.config

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.sdk.common.internal.OtelVersion
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.samplers.Sampler
import org.springframework.boot.actuate.autoconfigure.tracing.SpanProcessors
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
private class OtelLoggingProviderConfig {

    @Bean
    @ConditionalOnMissingBean
    fun otelSdkLoggerProvider(
        props: ServiceProps,
        resource: Resource,
        spanProcessors: SpanProcessors,
        sampler: Sampler
    ): SdkLoggerProvider {
        val provider = SdkLoggerProvider
            .builder()
            .addLogRecordProcessor(
                BatchLogRecordProcessor.builder(
                    OtlpGrpcLogRecordExporter.builder()
                        .setEndpoint("http://${props.contour.otlp.host}:${props.contour.otlp.logsPort}")
                        .build()
                ).build()
            )
            .setResource(
                Resource.create(
                    Attributes.builder()
                        .put("telemetry.sdk.name", "opentelemetry")
                        .put("telemetry.sdk.language", "java")
                        .put("telemetry.sdk.version", OtelVersion.VERSION)
                        .put("service.name", props.spring.application.name)
                        .build()
                )
            )
            .build()

        return provider
    }
}
