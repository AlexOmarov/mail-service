package ru.somarov.mail.presentation.consumers

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.Consumer
import ru.somarov.mail.infrastructure.kafka.Metadata
import ru.somarov.mail.infrastructure.kafka.Producer
import ru.somarov.mail.infrastructure.kafka.Producer.ProducerProps
import ru.somarov.mail.infrastructure.kafka.Result
import ru.somarov.mail.presentation.dto.event.RetryMessage
import ru.somarov.mail.presentation.dto.event.command.CreateMailCommand

@Component
class MailConsumer(
    private val service: MailService,
    private val props: ServiceProps,
    mapper: ObjectMapper,
    registry: ObservationRegistry,
) : Consumer<CreateMailCommand>(
    props = ConsumerProps(
        topic = props.kafka.createMailCommandTopic,
        name = "MailConsumer",
        delaySeconds = 0,
        strategy = ExecutionStrategy.PARALLEL,
        enabled = props.kafka.createMailCommandConsumingEnabled,
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
    clazz = CreateMailCommand::class.java,
    mapper = mapper
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

    override suspend fun handleMessage(message: CreateMailCommand, metadata: Metadata): Result {
        service.createMail(message.email, message.text)
        return Result(Result.Code.OK)
    }

    override suspend fun onFailedMessage(e: Exception?, message: CreateMailCommand, metadata: Metadata) {
        log.error("Got unsuccessful message processing: $message, exception ${e?.message}", e)
        val retryMessage = RetryMessage(payload = message, key = metadata.key, attempt = metadata.attempt + 1)
        if (metadata.attempt < props.kafka.retryResendNumber)
            retryProducer.send(retryMessage, metadata)
        else
            dlqProducer.send(retryMessage, metadata)
    }
}
