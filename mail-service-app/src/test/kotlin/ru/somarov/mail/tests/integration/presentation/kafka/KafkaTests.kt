package ru.somarov.mail.tests.integration.presentation.kafka

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Import
import org.testcontainers.shaded.org.awaitility.Awaitility
import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderRecord
import reactor.util.retry.Retry
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.infrastructure.kafka.serde.mailbroadcast.MailBroadcastDeserializer
import ru.somarov.mail.presentation.kafka.event.broadcast.MailBroadcast
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
    lateinit var dao: Dao
    override fun beforeEach() {
        reset(dao)
    }

    @Test
    fun `When consumer create mail command comes then create mail in db`() {
        val email = "emailfromkafka@mail.ru"
        val text = UUID.randomUUID().toString()

        val message = CreateMailCommand(email, text)

        val record = ProducerRecord(
            /* topic = */ props.kafka.createMailCommandTopic,
            /* partition = */ null,
            /* timestamp = */ null,
            /* key = */ "key",
            /* value = */ message,
            /* headers = */ mutableListOf<Header>()
        )

        runBlocking {
            createMailCommandSender.send(Flux.just(SenderRecord.create(record, message)))
                .doOnError { e -> log.error("Send failed", e) }
                .asFlow()
                .first()
        }
        Awaitility.await().atMost(Duration.ofSeconds(WAIT_TIMEOUT_FOR_KAFKA_MESSAGE_PROCESSING_SECONDS)).until {
            assertDoesNotThrow {
                verifyBlocking(dao, times(1)) {
                    createMail(eq(email), eq(text))
                }
                true
            }
        }
    }

    @Test
    fun `When create mail command comes then mail broadcast event is sent after saving mail`() {
        val receiver = createReceiver(MailBroadcastDeserializer(mapper), props.kafka.mailBroadcastTopic)
        val recordList = mutableListOf<ConsumerRecord<String, MailBroadcast?>>()
        receiver.receiveAutoAck().concatMap { records ->
            records.map { record ->
                log.info("Got $record with value ${record.value()}")
                recordList.add(record)
                record
            }.count().map {
                log.info("Completed batch of size $it")
            }
        }.onErrorContinue { throwable, obj ->
            log.error("Got exception while trying to process record: $obj", throwable)
        }.doOnTerminate {
            log.error("Custom event receiver terminated")
        }.retryWhen(
            Retry.backoff(
                props.kafka.receiversRetrySettings.attempts,
                Duration.ofSeconds(props.kafka.receiversRetrySettings.periodSeconds)
            ).jitter(props.kafka.receiversRetrySettings.jitter)
        ).subscribe()

        val message = CreateMailCommand("email", "text")

        val record = ProducerRecord(
            /* topic = */ props.kafka.createMailCommandTopic,
            /* partition = */ null,
            /* timestamp = */ null,
            /* key = */ "key",
            /* value = */ message,
            /* headers = */ mutableListOf<Header>()
        )

        runBlocking {
            createMailCommandSender.send(Flux.just(SenderRecord.create(record, message)))
                .doOnError { e -> log.error("Send failed", e) }
                .asFlow()
                .first()
        }
        Awaitility.await().atMost(Duration.ofSeconds(WAIT_TIMEOUT_FOR_KAFKA_MESSAGE_PROCESSING_SECONDS))
            .until { recordList.size == 1 }
        assert(recordList.size == 1)
    }

    private fun <T> createReceiver(
        deserializer: Deserializer<T?>,
        topic: String
    ): KafkaReceiver<String, T?> {
        val consumerProps = HashMap<String, Any>()
        consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = props.kafka.brokers
        consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = UUID.randomUUID()
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = props.kafka.maxPollRecords
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"

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
