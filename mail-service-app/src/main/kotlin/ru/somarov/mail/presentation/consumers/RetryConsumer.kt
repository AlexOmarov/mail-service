package ru.somarov.mail.presentation.consumers

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.Consumer
import ru.somarov.mail.infrastructure.kafka.Metadata
import ru.somarov.mail.infrastructure.kafka.Producer
import ru.somarov.mail.infrastructure.kafka.Producer.ProducerProps
import ru.somarov.mail.infrastructure.kafka.Result
import ru.somarov.mail.infrastructure.kafka.Result.Code.FAILED
import ru.somarov.mail.infrastructure.kafka.Result.Code.OK
import ru.somarov.mail.presentation.dto.event.RetryMessage
import java.time.OffsetDateTime

@Component
@Suppress("UNCHECKED_CAST")
class RetryConsumer(
    private val props: ServiceProps,
    private val consumers: List<Consumer<out Any>>,
    mapper: ObjectMapper,
    registry: ObservationRegistry,
) : Consumer<RetryMessage<Any>>(
    props = ConsumerProps(
        topic = props.kafka.retryTopic,
        name = "RetryConsumer",
        delaySeconds = 10,
        strategy = ExecutionStrategy.SEQUENTIAL,
        enabled = props.kafka.retryConsumingEnabled,
        brokers = props.kafka.brokers,
        groupId = props.kafka.groupId,
        offsetResetConfig = props.kafka.offsetResetConfig,
        commitInterval = props.kafka.commitInterval,
        maxPollRecords = props.kafka.maxPollRecords,
        reconnectAttempts = props.kafka.receiversRetrySettings.attempts,
        reconnectJitter = props.kafka.receiversRetrySettings.jitter,
        reconnectPeriodSeconds = props.kafka.receiversRetrySettings.periodSeconds
    ),
    registry = registry,
    mapper = mapper,
    clazz = RetryMessage::class.java as Class<RetryMessage<Any>>
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    private val retryProducer = Producer<RetryMessage<out Any>>(
        mapper,
        ProducerProps(props.kafka.brokers, props.kafka.sender.maxInFlight, props.kafka.retryTopic), registry
    )
    private val dlqProducer = Producer<RetryMessage<out Any>>(
        mapper,
        ProducerProps(props.kafka.brokers, props.kafka.sender.maxInFlight, props.kafka.dlqTopic), registry
    )

    override suspend fun handleMessage(
        message: RetryMessage<Any>,
        metadata: Metadata
    ): Result {
        return if (props.kafka.retryResendNumber > message.attempt) retryMessage(message) else Result(FAILED)
    }

    override suspend fun onFailedMessage(e: Exception?, message: RetryMessage<Any>, metadata: Metadata) {
        log.error("Got unsuccessful message processing: $message, exception ${e?.message}", e)
        if (metadata.attempt < props.kafka.retryResendNumber)
            retryProducer.send(message, metadata)
        else
            dlqProducer.send(message, metadata)
    }

    private suspend fun retryMessage(message: RetryMessage<Any>): Result {
        return if (message.payload::class != RetryMessage::class) {
            val consumer = consumers.firstOrNull { it.supports(message.payload::class.java) } as Consumer<Any>?
            if (consumer == null) {
                log.warn("Cannot find consumer for message $message. Skip message processing")
                Result(OK)
            } else {
                consumer.handleMessage(
                    message.payload,
                    Metadata(
                        OffsetDateTime.now(),
                        message.key,
                        message.attempt
                    )
                )
            }
        } else {
            throw IllegalArgumentException("Got retry message $message with payload not of type CommonEvent")
        }
    }
}
