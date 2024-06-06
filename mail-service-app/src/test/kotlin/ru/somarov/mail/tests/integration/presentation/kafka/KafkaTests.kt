package ru.somarov.mail.tests.integration.presentation.kafka

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Import
import org.testcontainers.shaded.org.awaitility.Awaitility.await
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.infrastructure.kafka.KafkaProducerFacade
import ru.somarov.mail.presentation.kafka.event.command.CreateMailCommand
import ru.somarov.mail.util.KafkaTestConfig
import java.time.Duration
import java.util.UUID

@Import(KafkaTestConfig::class)
class KafkaTests : BaseIntegrationTest() {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    lateinit var createMailCommandSender: KafkaSender<String, CreateMailCommand>

    @SpyBean
    lateinit var producerFacade: KafkaProducerFacade

    @SpyBean
    lateinit var dao: Dao

    override fun beforeEach() {
        reset(dao)
    }

    @Test
    fun `When consumer create mail command comes then create mail in db and send mail broadcast event`() = runBlocking {
        val recordList = mutableListOf<ConsumerRecord<String, String?>>()

        val email = "${UUID.randomUUID()}@mail.ru"
        val text = UUID.randomUUID().toString()
        val record = createProducerRecord(email, text)

        val disposable = createTestReceiver(recordList).subscribe()

        createMailCommandSender
            .send(Flux.just(SenderRecord.create(record, UUID.randomUUID())))
            .doOnError { e -> log.error("Send failed", e) }
            .asFlow()
            .first()

        await()
            .atMost(Duration.ofSeconds(WAIT_TIMEOUT_FOR_KAFKA_MESSAGE_PROCESSING_SECONDS))
            .untilAsserted {
                Assertions.assertAll(
                    {
                        assertDoesNotThrow {
                            verifyBlocking(dao, times(1)) {
                                createMail(eq(email), eq(text))
                            }

                            verifyBlocking(producerFacade, times(1)) {
                                sendMailBroadcast(any(), any(), any())
                            }
                        }
                    },
                    { assert(recordList.size == 1) }
                )
                assert(recordList.size == 1)
            }

        disposable.dispose()

        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted { assert(disposable.isDisposed) }
    }

    private fun createTestReceiver(recordList: MutableList<ConsumerRecord<String, String?>>): Flux<Unit> {
        val receiver = createReceiver(StringDeserializer(), props.kafka.mailBroadcastTopic)
        return receiver.receiveAutoAck()
            .concatMap { records ->
                records
                    .map { record ->
                        log.info("Got $record with value ${record.value()}")
                        recordList.add(record)
                        record
                    }
                    .count()
                    .map { log.info("Completed batch of size $it") }
            }
            .onErrorContinue { ex, obj -> log.error("Got exception while trying to process record: $obj", ex) }
            .doOnTerminate { log.error("Custom event receiver terminated") }
    }

    private fun createProducerRecord(email: String, text: String) = ProducerRecord(
        /* topic = */ props.kafka.createMailCommandTopic,
        /* partition = */ null,
        /* timestamp = */ null,
        /* key = */ "key",
        /* value = */ CreateMailCommand(email, text),
        /* headers = */ mutableListOf<Header>()
    )

    private fun <T> createReceiver(deserializer: Deserializer<T?>, topic: String): KafkaReceiver<String, T?> {
        val consumerProps = HashMap<String, Any>()

        consumerProps[BOOTSTRAP_SERVERS_CONFIG] = props.kafka.brokers
        consumerProps[GROUP_ID_CONFIG] = UUID.randomUUID().toString()
        consumerProps[KEY_DESERIALIZER_CLASS_CONFIG] = deserializer::class.java
        consumerProps[MAX_POLL_RECORDS_CONFIG] = props.kafka.maxPollRecords
        consumerProps[AUTO_OFFSET_RESET_CONFIG] = "latest"

        // Here we can set custom thread scheduler (withScheduler()...)
        val consProps = ReceiverOptions.create<String, T>(consumerProps)
            .commitInterval(Duration.ofSeconds(props.kafka.commitInterval))
            .withValueDeserializer(deserializer)
            .subscription(listOf(topic))

        return KafkaReceiver.create(consProps)
    }

    companion object {
        const val WAIT_TIMEOUT_FOR_KAFKA_MESSAGE_PROCESSING_SECONDS = 5L
    }
}
