package ru.somarov.mail.infrastructure.config

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
private class MicrometerConfig {
    @Bean
    fun customize(props: ServiceProps): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer {
            it.config().commonTags(
                "application", props.spring.application.name,
                "instance", props.contour.instance
            )
        }
    }

    @Bean
    fun meterRegistry(sdk: OpenTelemetry): MeterRegistry {
        return OpenTelemetryMeterRegistry.create(sdk)
    }
}
