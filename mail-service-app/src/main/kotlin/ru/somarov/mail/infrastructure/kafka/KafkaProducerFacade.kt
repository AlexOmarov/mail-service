package ru.somarov.mail.infrastructure.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.tracing.Tracer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.asFlow
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.Constants.PAYLOAD_TYPE_HEADER_NAME
import ru.somarov.mail.infrastructure.kafka.Utils.createSenderOptions
import ru.somarov.mail.infrastructure.kafka.Utils.createTraceParentHeader
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import ru.somarov.mail.infrastructure.kafka.serde.dlq.DlqMessageSerializer
import ru.somarov.mail.infrastructure.kafka.serde.mailbroadcast.MailBroadcastSerializer
import ru.somarov.mail.infrastructure.kafka.serde.retry.RetryMessageSerializer
import ru.somarov.mail.presentation.dto.events.DlqMessage
import ru.somarov.mail.presentation.dto.events.RetryMessage
import ru.somarov.mail.presentation.dto.events.event.broadcast.MailBroadcast
import java.time.OffsetDateTime

@Component
class KafkaProducerFacade(mapper: ObjectMapper, private val tracer: Tracer, private val props: ServiceProps) {

    private val log = LoggerFactory.getLogger(KafkaSender::class.java)

    private val retrySender = KafkaSender.create(createSenderOptions(RetryMessageSerializer(mapper), props.kafka))
    private val dlqSender = KafkaSender.create(createSenderOptions(DlqMessageSerializer(mapper), props.kafka))
    private val mailSender = KafkaSender.create(createSenderOptions(MailBroadcastSerializer(mapper), props.kafka))

    suspend fun sendMailBroadcast(
        event: MailBroadcast,
        metadata: MessageMetadata,
        topic: String
    ): SenderResult<MailBroadcast> {
        return sendMessage(
            event,
            event::class.qualifiedName ?: "",
            metadata,
            props.kafka.mailBroadcastTopic,
            mailSender
        )
    }

    suspend fun <T : Any> sendRetry(
        event: T,
        metadata: MessageMetadata
    ): SenderResult<RetryMessage<out Any>> {
        val message = RetryMessage(
            payload = event,
            key = metadata.key,
            processingAttemptNumber = metadata.attempt + 1
        )
        return sendMessage(
            message,
            event::class.qualifiedName ?: "",
            MessageMetadata(OffsetDateTime.now(), "retry_${metadata.key}", 0),
            props.kafka.retryTopic,
            retrySender
        )
    }

    suspend fun <T : Any> sendDlq(
        event: T,
        metadata: MessageMetadata
    ): SenderResult<DlqMessage<out Any>> {
        val message = DlqMessage(
            payload = event,
            key = metadata.key,
            processingAttemptNumber = metadata.attempt + 1
        )
        return sendMessage(
            message,
            event::class.qualifiedName ?: "",
            MessageMetadata(OffsetDateTime.now(), "dlq_${metadata.key}", 0),
            props.kafka.dlqTopic,
            dlqSender
        )
    }

    suspend fun <T : Any> sendMessage(
        message: T,
        payloadQualifiedName: String,
        metadata: MessageMetadata,
        topic: String,
        sender: KafkaSender<String, T>
    ): SenderResult<T> {
        val partition = null
        val timestamp = null
        val traceParent = createTraceParentHeader(tracer)
        val headers = mutableListOf<Header>(
            RecordHeader(TRACE_HEADER_KEY, traceParent.toByteArray()),
            RecordHeader(PAYLOAD_TYPE_HEADER_NAME, (payloadQualifiedName).toByteArray())
        )

        val record = ProducerRecord(
            /* topic = */ topic,
            /* partition = */ partition,
            /* timestamp = */ timestamp,
            /* key = */ metadata.key,
            /* value = */ message,
            /* headers = */ headers
        )

        val result = sender.send(Flux.just(SenderRecord.create(record, message)))
            .doOnError { e -> log.error("Send failed", e) }
            .asFlow()
            .first()

        log.info(
            "Message has been sent: ${result.correlationMetadata()} " +
                "traceParent: $traceParent, " +
                "topic/partition: ${result.recordMetadata().topic()}/${result.recordMetadata().partition()}, " +
                "offset: ${result.recordMetadata().offset()}, " +
                "timestamp: ${result.recordMetadata().timestamp()}"
        )

        return result
    }

    companion object {
        const val TRACE_HEADER_KEY = "traceparent"
    }
}
