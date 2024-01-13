package ru.somarov.mail.infrastructure.kafka.consumer.types

import jakarta.validation.Validation
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.consumer.IMessageConsumer
import ru.somarov.mail.infrastructure.kafka.consumer.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import java.time.Duration

abstract class AbstractMessageConsumer<T : Any>(
    private val props: ServiceProps.KafkaProps,
) : IMessageConsumer<T> {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Suppress("TooGenericExceptionCaught") // Should be able to process every exception
    override suspend fun handle(message: T, metadata: MessageMetadata): MessageConsumptionResult {
        log.info("Got $message with metadata $metadata to handle with retry")

        val result = try {
            val constraintViolations = validator.validate(message)
            handleMessage(message, metadata)
        } catch (e: Exception) {
            log.error("Got exception while processing event $message with metadata $metadata", e)
            onFailedMessage(e, message, metadata)
            MessageConsumptionResult(MessageConsumptionResult.MessageConsumptionResultCode.FAILED)
        }
        if (result.code == MessageConsumptionResult.MessageConsumptionResultCode.FAILED) {
            onFailedMessage(null, message, metadata)
        }
        return result
    }

    abstract suspend fun handleMessage(message: T, metadata: MessageMetadata): MessageConsumptionResult

    abstract suspend fun onFailedMessage(e: Exception?, message: T, metadata: MessageMetadata)

    fun <T> buildReceiver(
        deserializer: Deserializer<T?>,
        topic: String
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
