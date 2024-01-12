package ru.somarov.mail.infrastructure.config

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Configuration
import java.util.TimeZone

@Configuration
@EnableConfigurationProperties
@ConfigurationPropertiesScan
private class AppConfig(private val registry: MeterRegistry, private val buildProps: BuildProperties) {
    @PostConstruct
    fun init() {
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE))
        Gauge.builder("project_version") { METER_VALUE }
            .description("Version of project in tag")
            .tag("version", buildProps.version)
            .register(registry)
    }

    // TODO: dynamic creation of HttpLoggerFilter for all regexes from service props
    companion object {
        private const val METER_VALUE = 1
        private const val TIMEZONE = "UTC"
    }
}
