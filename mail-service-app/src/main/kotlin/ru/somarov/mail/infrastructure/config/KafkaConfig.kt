package ru.somarov.mail.infrastructure.config

import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import ru.somarov.mail.infrastructure.kafka.KafkaConsumerLauncherDecorator
import ru.somarov.mail.infrastructure.kafka.observability.KafkaClusterHealthIndicator
import ru.somarov.mail.infrastructure.kafka.observability.KafkaTracePropagator

@Configuration
private class KafkaConfig(private val props: ServiceProps) {
    private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Bean
    fun kafkaReceiversHealth(decorator: KafkaConsumerLauncherDecorator): CompositeReactiveHealthContributor {
        var handler = CompositeReactiveHealthContributor.fromMap(HashMap<String, ReactiveHealthIndicator>())
        if (props.kafka.consumingEnabled) {
            handler = decorator.launchConsumers()
        } else {
            logger.info("Kafka is disabled: $props")
        }
        return handler
    }

    @Bean
    fun kafkaClusterHealth(props: ServiceProps): ReactiveHealthIndicator {
        val indicator = KafkaClusterHealthIndicator(props.kafka)
        if (props.kafka.consumingEnabled) {
            val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            // warmup
            runBlocking {
                ioScope.launch {
                    withTimeoutOrNull(props.kafka.healthWarmupTimeoutMillis) {
                        indicator.health()
                    }
                }
            }
        } else {
            logger.info("Kafka is disabled: $props")
        }
        return indicator
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(Tracer::class)
    // Should be executed before handling message trace
    @Order(MicrometerTracingAutoConfiguration.DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER - 1)
    fun kafkaTracePropagator(tracer: Tracer, propagator: Propagator): KafkaTracePropagator {
        return KafkaTracePropagator(tracer, propagator)
    }
}
