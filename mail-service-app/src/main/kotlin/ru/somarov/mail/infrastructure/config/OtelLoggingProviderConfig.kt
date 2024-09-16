package ru.somarov.mail.infrastructure.config

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.common.internal.OtelVersion
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.samplers.Sampler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
private class OtelLoggingProviderConfig {

    @Bean
    fun otelSdkLoggerProvider(props: ServiceProps): SdkLoggerProvider {
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
                        .put("telemetry.sdk.language", "kotlin")
                        .put("telemetry.sdk.version", OtelVersion.VERSION)
                        .put("service.name", props.spring.application.name)
                        .put("service.instance", props.contour.instance)
                        .build()
                )
            )
            .build()

        return provider
    }

    @Bean
    fun otelSdkMeterProvider(props: ServiceProps): SdkMeterProvider {
        return SdkMeterProvider.builder()
            .registerMetricReader(
                PeriodicMetricReader.builder(
                    OtlpGrpcMetricExporter.builder()
                        .setEndpoint("http://${props.contour.otlp.host}:${props.contour.otlp.metricsPort}")
                        .build()
                ).build()
            )
            .setResource(
                Resource.create(
                    Attributes.builder()
                        .put("telemetry.sdk.name", "opentelemetry")
                        .put("telemetry.sdk.language", "kotlin")
                        .put("telemetry.sdk.version", OtelVersion.VERSION)
                        .put("service.name", props.spring.application.name)
                        .put("service.instance", props.contour.instance)
                        .build()
                )
            )
            .build()
    }

    @Bean
    fun otelSdkTracerProvider(props: ServiceProps): SdkTracerProvider {
        val sampler = Sampler.parentBased(Sampler.traceIdRatioBased(props.management.tracing.sampling.probability))
        val resource = Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.of(
                        AttributeKey.stringKey("service.name"),
                        props.spring.application.name
                    )
                )
            )

        val builder = SdkTracerProvider.builder().setSampler(sampler).setResource(resource)
            .addSpanProcessor(
                BatchSpanProcessor.builder(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint("http://${props.contour.otlp.host}:${props.contour.otlp.tracesPort}")
                        .build()
                ).build()
            )
            .setResource(
                Resource.create(
                    Attributes.builder()
                        .put("telemetry.sdk.name", "opentelemetry")
                        .put("telemetry.sdk.language", "kotlin")
                        .put("telemetry.sdk.version", OtelVersion.VERSION)
                        .put("service.name", props.spring.application.name)
                        .put("service.instance", props.contour.instance)
                        .build()
                )
            )
        return builder.build()
    }
}
