package ru.somarov.mail.infrastructure.config

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
private class MicrometerConfig {
    @Bean
    fun customize(props: ServiceProps, env: Environment, buildProperties: BuildProperties):
        MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer {
            it.config().commonTags(
                "application", buildProperties.group,
                "instance", props.contour.instance
            )
        }
    }
}
