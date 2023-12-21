package ru.somarov.mail.infrastructure.config

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class MicrometerConfig {
    @Bean
    fun customize(props: ServiceProps, env: Environment, buildProperties: BuildProperties):
        MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer {
            it.config().commonTags(
                "service", buildProperties.name,
                "instance", props.contour.instance
            )
        }
    }
}
