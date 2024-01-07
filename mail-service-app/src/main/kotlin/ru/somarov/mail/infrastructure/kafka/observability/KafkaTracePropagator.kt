package ru.somarov.mail.infrastructure.kafka.observability

import io.micrometer.observation.Observation
import io.micrometer.observation.transport.ReceiverContext
import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler
import io.micrometer.tracing.propagation.Propagator
import org.apache.kafka.clients.consumer.ConsumerRecord
import reactor.kafka.receiver.observation.KafkaRecordReceiverContext

class KafkaTracePropagator(tracer: Tracer, propagator: Propagator) :
    PropagatingReceiverTracingObservationHandler<ReceiverContext<ConsumerRecord<Any, Any>>>(tracer, propagator) {
    override fun tagSpan(context: ReceiverContext<ConsumerRecord<Any, Any>>, span: Span) {
        for (tag in context.highCardinalityKeyValues) {
            span.tag(tag.key, tag.value)
        }
    }

    override fun supportsContext(context: Observation.Context): Boolean {
        return context is KafkaRecordReceiverContext && super.supportsContext(context)
    }
}
