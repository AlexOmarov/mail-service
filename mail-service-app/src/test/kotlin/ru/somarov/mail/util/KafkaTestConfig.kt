package ru.somarov.mail.util

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.presentation.dto.event.command.CreateMailCommand

@TestConfiguration
@AutoConfigureObservability
class KafkaTestConfig(private val props: ServiceProps) {
    private val testTopic: String = "test"

    @PostConstruct
    fun createTopics() {
        val admin = AdminClient.create(
            mapOf(Pair(BOOTSTRAP_SERVERS_CONFIG, props.kafka.brokers))
        )
        admin.createTopics(
            listOf(
                NewTopic(
                    /* name = */ props.kafka.createMailCommandTopic,
                    /* numPartitions = */ 1,
                    /* replicationFactor = */ 1
                ),
                NewTopic(
                    /* name = */ props.kafka.dlqTopic,
                    /* numPartitions = */ 1,
                    /* replicationFactor = */ 1
                ),
                NewTopic(
                    /* name = */ props.kafka.retryTopic,
                    /* numPartitions = */ 1,
                    /* replicationFactor = */ 1
                ),
                NewTopic(
                    /* name = */ props.kafka.mailBroadcastTopic,
                    /* numPartitions = */ 1,
                    /* replicationFactor = */ 1
                ),
                NewTopic(
                    /* name = */ testTopic,
                    /* numPartitions = */ 3,
                    /* replicationFactor = */ 1
                )
            )
        )
    }

    @Bean
    fun createMailCommandSender(mapper: ObjectMapper): KafkaSender<String, CreateMailCommand> {
        return createSender { _, data ->
            try {
                mapper.writeValueAsBytes(data)
            } catch (e: Exception) {
                throw SerializationException("Error when serializing CommonEvent to byte[]", e)
            }
        }
    }

    private fun <T> createSender(serializer: Serializer<T>): KafkaSender<String, T> {
        val producerProps: MutableMap<String, Any> = HashMap()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = props.kafka.brokers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = serializer::class.java

        return KafkaSender.create(
            SenderOptions.create<String, T>(producerProps)
                .withValueSerializer(serializer)
                .maxInFlight(props.kafka.sender.maxInFlight)
        )
    }
}
