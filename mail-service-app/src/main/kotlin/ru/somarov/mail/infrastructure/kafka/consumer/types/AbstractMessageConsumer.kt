package ru.somarov.mail.infrastructure.kafka.consumer.types

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validation
import org.slf4j.LoggerFactory
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.consumer.IMessageConsumer
import ru.somarov.mail.infrastructure.kafka.consumer.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata

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
            if (constraintViolations.isNotEmpty()) {
                throw ConstraintViolationException(constraintViolations)
            }
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
}
