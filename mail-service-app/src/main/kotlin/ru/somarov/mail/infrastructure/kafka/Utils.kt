package ru.somarov.mail.infrastructure.kafka

import io.micrometer.tracing.Tracer
import io.opentelemetry.api.trace.SpanId
import io.opentelemetry.api.trace.TraceId
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.SenderOptions
import ru.somarov.mail.infrastructure.config.ServiceProps
import java.time.Duration
import kotlin.random.Random

object Utils {
    fun <T> createSenderOptions(serializer: Serializer<T>, props: ServiceProps.KafkaProps): SenderOptions<String, T> {
        val producerProps: MutableMap<String, Any> = HashMap()

        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = props.brokers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java

        return SenderOptions
            .create<String, T>(producerProps)
            .withValueSerializer(serializer)
            .maxInFlight(props.sender.maxInFlight)
    }

    fun createTraceParentHeader(tracer: Tracer): String {
        val min = Random.nextLong(2L, Long.MAX_VALUE - 2)
        val max = Random.nextLong(min, Long.MAX_VALUE)
        var traceId = TraceId.fromLongs(max, min)
        var spanId = SpanId.fromLong(Random.nextLong())

        val traceContext = tracer.currentTraceContext().context()
        if (traceContext != null) {
            traceId = traceContext.traceId()
            spanId = traceContext.spanId()
        }

        return "00-$traceId-$spanId-01"
    }

    fun <T> buildReceiver(
        deserializer: Deserializer<T?>,
        topic: String,
        props: ServiceProps.KafkaProps
    ): KafkaReceiver<String, T?> {
        val consumerProps = HashMap<String, Any>()

        consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = props.brokers
        consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = props.groupId
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = props.maxPollRecords
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = props.offsetResetConfig

        // Here we can set custom thread scheduler (withScheduler()...)
        val consProps = ReceiverOptions.create<String, T>(consumerProps)
            .commitInterval(Duration.ofSeconds(props.commitInterval))
            .withValueDeserializer(deserializer)
            .subscription(listOf(topic))

        return KafkaReceiver.create(consProps)
    }
}
