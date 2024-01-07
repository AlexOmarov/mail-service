package ru.somarov.mail.infrastructure.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import ru.somarov.mail.infrastructure.config.ServiceProps
import java.time.Duration

object KafkaUtils {
    fun <T> buildReceiver(
        deserializer: Deserializer<T?>,
        topic: String,
        kafkaProps: ServiceProps.KafkaProps
    ): KafkaReceiver<String, T?> {
        val consumerProps = HashMap<String, Any>()
        consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProps.brokers
        consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = kafkaProps.groupId
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = kafkaProps.maxPollRecords
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = kafkaProps.offsetResetConfig

        // Here we can set custom thread scheduler (withScheduler()...)
        val consProps = ReceiverOptions.create<String, T>(consumerProps)
            .commitInterval(Duration.ofSeconds(kafkaProps.commitInterval))
            .withValueDeserializer(deserializer)
            .subscription(listOf(topic))

        return KafkaReceiver.create(consProps)
    }
}
