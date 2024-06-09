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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import ru.somarov.mail.infrastructure.kafka.Consumer
import ru.somarov.mail.infrastructure.kafka.observability.KafkaClusterHealthIndicator
import ru.somarov.mail.infrastructure.kafka.observability.KafkaReceiverHealthIndicator
import ru.somarov.mail.infrastructure.kafka.observability.KafkaTracePropagator

@Configuration
private class KafkaConfig(private val props: ServiceProps) {
    private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Bean
    fun kafkaConsumersHealth(consumers: List<Consumer<out Any>>): CompositeReactiveHealthContributor {
        val indicators = mutableMapOf<String, KafkaReceiverHealthIndicator>()
        if (props.kafka.consumingEnabled) {
            consumers.forEach { consumer ->
                consumer.start()?.let { indicators[consumer.getName()] = it }
            }
        } else {
            logger.info("Consumers are disabled: $props")
        }
        return CompositeReactiveHealthContributor.fromMap(indicators)
    }

    @Bean
    fun kafkaClusterHealth(props: ServiceProps): KafkaClusterHealthIndicator {
        val indicator = KafkaClusterHealthIndicator(props.kafka)
        if (props.kafka.consumingEnabled) {
            // warmup
            runBlocking {
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    withTimeoutOrNull(props.kafka.healthWarmupTimeoutMillis) {
                        indicator.health()
                    }
                }
            }
        } else {
            logger.info("Cluster health is disabled: $props")
        }
        return indicator
    }

    @Bean
    @ConditionalOnMissingBean
    // Should be executed before handling message trace
    @Order(MicrometerTracingAutoConfiguration.DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER - 1)
    fun kafkaTracePropagator(tracer: Tracer, propagator: Propagator) = KafkaTracePropagator(tracer, propagator)
}
