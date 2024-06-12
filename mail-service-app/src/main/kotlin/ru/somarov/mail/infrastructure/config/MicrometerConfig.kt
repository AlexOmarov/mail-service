package ru.somarov.mail.infrastructure.config

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
private class MicrometerConfig {
    @Bean
    fun customize(props: ServiceProps, env: Environment):
        MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer {
            it.config().commonTags(
                "application", props.spring.application.name,
                "instance", props.contour.instance
            )
        }
    }
}
