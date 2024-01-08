package ru.somarov.mail.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import ru.somarov.mail.infrastructure.kafka.KafkaConsumerLauncherDecorator
import ru.somarov.mail.infrastructure.kafka.observability.KafkaClusterHealthIndicator
import ru.somarov.mail.infrastructure.kafka.observability.KafkaTracePropagator
import ru.somarov.mail.infrastructure.kafka.serde.dlq.DlqMessageSerializer
import ru.somarov.mail.infrastructure.kafka.serde.retry.RetryMessageSerializer
import ru.somarov.mail.presentation.kafka.DlqMessage
import ru.somarov.mail.presentation.kafka.RetryMessage

@Configuration
private class KafkaConfig(private val props: ServiceProps) {
    private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Bean
    fun retryEventSender(mapper: ObjectMapper): KafkaSender<String, RetryMessage<Any>> {
        return KafkaSender.create(senderProps(RetryMessageSerializer(mapper)))
    }

    @Bean
    fun dlqEventSender(mapper: ObjectMapper): KafkaSender<String, DlqMessage<Any>> {
        return KafkaSender.create(senderProps(DlqMessageSerializer(mapper)))
    }

    @Bean
    fun adminClient(): AdminClient {
        val configs: MutableMap<String, Any> = HashMap()
        configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = props.kafka.brokers
        return AdminClient.create(configs)
    }

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

    // TODO: empty check if kafka is disabled
    @Bean
    fun kafkaClusterHealth(kafkaAdminClient: AdminClient): ReactiveHealthIndicator {
        val indicator = KafkaClusterHealthIndicator(kafkaAdminClient, props.kafka.healthTimeoutMillis)
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

    private fun <T> senderProps(serializer: Serializer<T>): SenderOptions<String, T> {
        val producerProps: MutableMap<String, Any> = HashMap()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = props.kafka.brokers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java

        return SenderOptions.create<String, T>(producerProps)
            .withValueSerializer(serializer)
            .maxInFlight(props.kafka.sender.maxInFlight)
    }
}
