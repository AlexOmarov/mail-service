package ru.somarov.mail.infrastructure.kafka

import io.micrometer.core.instrument.kotlin.asContextElement
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.reactor.mono
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.kafka.support.micrometer.KafkaRecordReceiverContext
import org.springframework.stereotype.Component
import reactor.core.Disposables
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.consumer.IMessageConsumer
import ru.somarov.mail.infrastructure.kafka.consumer.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import ru.somarov.mail.infrastructure.kafka.observability.KafkaReceiverHealthIndicator
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import java.util.function.Supplier

@Component
class KafkaConsumerLauncherDecorator(
    private val props: ServiceProps,
    private val consumers: List<IMessageConsumer<out Any>>,
    private val registry: ObservationRegistry
) {
    private val log = LoggerFactory.getLogger(KafkaConsumerLauncherDecorator::class.java)
    private val disposables = Disposables.composite()

    fun launchConsumers(): CompositeReactiveHealthContributor {
        val registry: MutableMap<String, ReactiveHealthIndicator> = HashMap()
        startConsumers(registry)
        return CompositeReactiveHealthContributor.fromMap(registry)
    }

    @PreDestroy
    private fun closeConsumers() {
        disposables.dispose()
    }

    @Suppress("UNCHECKED_CAST")
    private fun startConsumers(registry: MutableMap<String, ReactiveHealthIndicator>) {
        consumers
            .filter { it.enabled() }
            .forEach {
                val disposable = consume(it as IMessageConsumer<Any>).subscribe()
                disposables.add(disposable)
                registry[it.getName()] = KafkaReceiverHealthIndicator(disposable)
            }
    }

    private fun consume(consumer: IMessageConsumer<Any>): Flux<Long> {
        return getRecordsBatches(consumer)
            .concatMap { batch -> handleBatch(batch, consumer) }
            .doOnSubscribe { log.info("${consumer.getName()} receiver started") }
            .doOnTerminate { log.info("${consumer.getName()} receiver terminated") }
            .doOnError { throwable -> log.error("Got exception while processing records", throwable) }
            .retryWhen(getRetrySettings())
    }

    private fun getRecordsBatches(consumer: IMessageConsumer<Any>): Flux<Flux<ConsumerRecord<String, Any?>>> {
        log.info("Starting ${consumer.getName()} receiver")
        var flux = consumer.getReceiver().receiveAutoAck()
        if (consumer.getDelaySeconds() != null) {
            flux = flux.delayElements(Duration.ofSeconds(consumer.getDelaySeconds()!!))
        }
        return flux
    }

    private fun handleBatch(records: Flux<ConsumerRecord<String, Any?>>, consumer: IMessageConsumer<Any>): Mono<Long> {
        return records
            .groupBy { record -> record.partition() }
            .flatMap { partitionRecords ->
                if (consumer.getExecutionStrategy() == IMessageConsumer.ExecutionStrategy.SEQUENTIAL) {
                    // Process records within each partition sequentially
                    partitionRecords.concatMap { record -> handleRecord(record, consumer) }
                } else {
                    // Process records within each partition parallel
                    partitionRecords.flatMap { record -> handleRecord(record, consumer) }
                }.count()
            }
            .reduce(Long::plus) // Sum the counts across all partitions
            .map { totalProcessedRecords ->
                log.info("Completed batch of size $totalProcessedRecords")
                totalProcessedRecords
            }
    }

    private fun getRetrySettings(): Retry {
        return Retry
            .backoff(
                props.kafka.receiversRetrySettings.attempts,
                Duration.ofSeconds(props.kafka.receiversRetrySettings.periodSeconds)
            )
            .jitter(props.kafka.receiversRetrySettings.jitter)
    }

    private fun handleRecord(
        record: ConsumerRecord<String, Any?>,
        receiver: IMessageConsumer<Any>
    ): Mono<MessageConsumptionResult>? {
        val observation = Observation.createNotStarted(
            "kafka_observation_${UUID.randomUUID()}",
            { KafkaRecordReceiverContext(record, receiver.getName()) { UUID.randomUUID().toString() } },
            registry
        )

        return observation.observe(
            Supplier<Mono<MessageConsumptionResult>> {
                if (record.value() == null) {
                    log.warn("Got empty value for record $record")
                    // Would be great to send messages which cannot be serialized to dlq here
                    Mono.just(MessageConsumptionResult(MessageConsumptionResult.MessageConsumptionResultCode.FAILED))
                } else {
                    mono(registry.asContextElement()) {
                        receiver.handle(record.value()!!, MessageMetadata(OffsetDateTime.now(), record.key(), 0))
                    }
                }
            }
        )
    }
}
