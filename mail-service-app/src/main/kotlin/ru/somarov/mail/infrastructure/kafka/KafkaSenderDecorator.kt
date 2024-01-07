package ru.somarov.mail.infrastructure.kafka

import io.micrometer.tracing.Tracer
import io.opentelemetry.api.trace.SpanId
import io.opentelemetry.api.trace.TraceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import ru.somarov.mail.presentation.kafka.DlqMessage
import ru.somarov.mail.presentation.kafka.RetryMessage
import ru.somarov.mail.presentation.kafka.event.EventType
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random

@Component
class KafkaSenderDecorator(
    private val retrySender: KafkaSender<String, RetryMessage<Any>>,
    private val dlqSender: KafkaSender<String, DlqMessage<Any>>,
    private val tracer: Tracer,
    private val props: ServiceProps,
) {
    private val log = LoggerFactory.getLogger(KafkaSender::class.java)

    suspend fun <T> send(
        event: T,
        metadata: MessageMetadata,
        topic: String,
        sender: KafkaSender<String, T>
    ) {
        val partition = null
        val timestamp = null
        val traceParent = getTraceParentHeader()
        val headers = mutableListOf<Header>(RecordHeader(TRACE_HEADER_KEY, traceParent.toByteArray()))

        val record = ProducerRecord(
            /* topic = */ topic,
            /* partition = */ partition,
            /* timestamp = */ timestamp,
            /* key = */ metadata.key,
            /* value = */ event,
            /* headers = */ headers
        )
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch(EmptyCoroutineContext) {
            val result = sender.send(Flux.just(SenderRecord.create(record as ProducerRecord<String, T>, event)))
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
        }.join()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> sendRetry(
        event: T,
        metadata: MessageMetadata,
        payloadType: EventType
    ): SenderResult<RetryMessage<T>> {
        val partition = null
        val timestamp = null
        val traceParent = getTraceParentHeader()
        val headers = mutableListOf<Header>(RecordHeader(TRACE_HEADER_KEY, traceParent.toByteArray()))

        val retryMessage = RetryMessage(
            payload = event,
            key = metadata.key,
            payloadType = payloadType,
            processingAttemptNumber = metadata.attempt + 1
        )

        val record = ProducerRecord(
            /* topic = */ props.kafka.retryTopic,
            /* partition = */ partition,
            /* timestamp = */ timestamp,
            /* key = */ metadata.key,
            /* value = */ retryMessage,
            /* headers = */ headers
        )

        val result =
            CoroutineScope(SupervisorJob()).async(EmptyCoroutineContext) {
                retrySender.send(
                    Flux.just(
                        SenderRecord.create(
                            record as ProducerRecord<String, RetryMessage<Any>>,
                            retryMessage
                        )
                    )
                )
                    .doOnError { e -> log.error("Send failed", e) }
                    .asFlow()
                    .first()
            }.await()

        log.info(
            "Message has been sent: ${result.correlationMetadata()} " +
                "traceParent: $traceParent, " +
                "topic/partition: ${result.recordMetadata().topic()}/${result.recordMetadata().partition()}, " +
                "offset: ${result.recordMetadata().offset()}, " +
                "timestamp: ${result.recordMetadata().timestamp()}"
        )

        return result
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> sendDlq(
        event: T,
        metadata: MessageMetadata,
        payloadType: EventType
    ): SenderResult<DlqMessage<T>> {
        val partition = null
        val timestamp = null
        val traceParent = getTraceParentHeader()
        val headers = mutableListOf<Header>(RecordHeader(TRACE_HEADER_KEY, traceParent.toByteArray()))

        val dlqMessage = DlqMessage(
            payload = event,
            key = metadata.key,
            payloadType = payloadType,
            processingAttemptNumber = metadata.attempt + 1
        )

        val record = ProducerRecord(
            /* topic = */ props.kafka.dlqTopic,
            /* partition = */ partition,
            /* timestamp = */ timestamp,
            /* key = */ metadata.key,
            /* value = */ dlqMessage,
            /* headers = */ headers
        )

        val result =
            CoroutineScope(SupervisorJob()).async(EmptyCoroutineContext) {
                dlqSender.send(
                    Flux.just(
                        SenderRecord.create(
                            record as ProducerRecord<String, DlqMessage<Any>>,
                            dlqMessage
                        )
                    )
                )
                    .doOnError { e -> log.error("Send failed", e) }
                    .asFlow()
                    .first()
            }.await()

        log.info(
            "Message has been sent: ${result.correlationMetadata()} " +
                "traceParent: $traceParent, " +
                "topic/partition: ${result.recordMetadata().topic()}/${result.recordMetadata().partition()}, " +
                "offset: ${result.recordMetadata().offset()}, " +
                "timestamp: ${result.recordMetadata().timestamp()}"
        )

        return result
    }

    private fun getTraceParentHeader(): String {
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

    companion object {
        const val TRACE_HEADER_KEY = "traceparent"
    }
}
