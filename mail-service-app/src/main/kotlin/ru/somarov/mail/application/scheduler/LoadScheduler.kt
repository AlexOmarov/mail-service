package ru.somarov.mail.application.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveMono
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.stereotype.Component
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderResult
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.KafkaProducerFacade
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import ru.somarov.mail.infrastructure.kafka.serde.createmailcommand.CreateMailCommandSerializer
import ru.somarov.mail.presentation.kafka.event.command.CreateMailCommand
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID
import ru.somarov.mail.presentation.http.request.CreateMailRequest as CreateMailRequestHttp
import ru.somarov.mail.presentation.http.response.MailResponse as MailResponseHttp
import ru.somarov.mail.presentation.http.response.standard.StandardResponse as StandardResponseHttp
import ru.somarov.mail.presentation.rsocket.request.CreateMailRequest as CreateMailRequestRsocket
import ru.somarov.mail.presentation.rsocket.response.MailRsocketResponse as MailResponseRsocket
import ru.somarov.mail.presentation.rsocket.response.standard.StandardRsocketResponse as StandardResponseRsocket

/**
 * Scheduler, which isn't a part of application base, but exists for imitating a load on service's APIs
 * */
@Component
@ConditionalOnExpression("\${contour.scheduling.load.enabled} and \${contour.scheduling.enabled}")
private class LoadScheduler(
    private val requester: RSocketRequester,
    private val props: ServiceProps,
    private val producerFacade: KafkaProducerFacade,
    objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private lateinit var sender: KafkaSender<String, CreateMailCommand>
    private lateinit var poisonPillSender: KafkaSender<String, String>

    init {
        val producerProps: MutableMap<String, Any> = HashMap()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = props.kafka.brokers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java

        sender = KafkaSender.create(
            SenderOptions.create<String, CreateMailCommand>(producerProps)
                .withValueSerializer(CreateMailCommandSerializer(objectMapper))
                .maxInFlight(props.kafka.sender.maxInFlight)
        )

        poisonPillSender = KafkaSender.create(
            SenderOptions.create<String, String>(producerProps)
                .withValueSerializer { _, data -> objectMapper.writeValueAsBytes(data) }
                .maxInFlight(props.kafka.sender.maxInFlight)
        )
    }

    @SchedulerLock(
        name = "LoadScheduler",
        lockAtMostFor = "\${contour.scheduling.load.lock-max-duration}"
    )
    @Scheduled(fixedDelayString = "\${contour.scheduling.load.delay}", zone = "UTC")
    @Suppress("TooGenericExceptionCaught") // Have to catch each exception
    fun launch() {
        try {
            runBlocking {
                logger.info("Started LoadScheduler")

                val rsocketCreateMailResponse = createMailUsingRsocket()
                logger.info("Rsocket create mail response: $rsocketCreateMailResponse")

                val httpCreateMailResponse = createMailUsingHttp()
                logger.info("Http create mail response: $httpCreateMailResponse")

                val kafkaCreateMailResponse = createMailUsingKafka()
                logger.info("Kafka create mail response: $kafkaCreateMailResponse")

                val kafkaPoisonPillResponse = sendPoisonPillUsingKafka()
                logger.info("Kafka poison pill response: $kafkaPoisonPillResponse")

                val rsocketGetMailResponse = getMailUsingRsocket(rsocketCreateMailResponse.response.mail.id)
                logger.info("Rsocket get mail response: $rsocketGetMailResponse")

                logger.info("LoadScheduler has been completed")
            }
        } catch (e: Exception) {
            logger.error("Got exception while processing Load scheduler: $e")
        }
    }

    private suspend fun sendPoisonPillUsingKafka(): SenderResult<String> {
        return producerFacade.sendMessage(
            "Poison pill",
            String::class.qualifiedName ?: "",
            MessageMetadata(OffsetDateTime.now(), "key", 0),
            props.kafka.createMailCommandTopic,
            poisonPillSender
        )
    }

    private suspend fun createMailUsingKafka(): SenderResult<CreateMailCommand> {
        return producerFacade.sendMessage(
            CreateMailCommand("email", "text"),
            CreateMailCommand::class.qualifiedName ?: "",
            MessageMetadata(OffsetDateTime.now(), "key", 0),
            props.kafka.createMailCommandTopic,
            sender
        )
    }

    private suspend fun getMailUsingRsocket(id: UUID): StandardResponseRsocket<MailResponseRsocket> {
        val credentials = UsernamePasswordMetadata(props.contour.auth.user, props.contour.auth.password)
        // Had to use retrieveMono contextCapture to capture observation,
        // because observation requester proxy creates mono for request using mono defer contextual
        return requester
            .route("mail.$id")
            .metadata(credentials, RSOCKET_AUTHENTICATION_MIME_TYPE)
            .retrieveMono<StandardResponseRsocket<MailResponseRsocket>>()
            .contextCapture().awaitSingle()
    }

    private suspend fun createMailUsingHttp(): StandardResponseHttp<MailResponseHttp> {
        val webClient = WebClient.builder().baseUrl("http://localhost:${props.contour.http.port}").build()
        val request = CreateMailRequestHttp("text", "test@gmail.com")
        return webClient.post()
            .uri("/mails")
            .body(BodyInserters.fromValue(request))
            .header(HttpHeaders.AUTHORIZATION, getAuthHeaderValue())
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<StandardResponseHttp<MailResponseHttp>>() {})
            .awaitSingle()
    }

    private suspend fun createMailUsingRsocket(): StandardResponseRsocket<MailResponseRsocket> {
        val credentials = UsernamePasswordMetadata(props.contour.auth.user, props.contour.auth.password)
        // Had to use retrieveMono contextCapture to capture observation,
        // because observation requester proxy creates mono for request using mono defer contextual
        return requester
            .route("mail")
            .metadata(credentials, RSOCKET_AUTHENTICATION_MIME_TYPE)
            .data(CreateMailRequestRsocket("text", "email"))
            .retrieveMono<StandardResponseRsocket<MailResponseRsocket>>()
            .contextCapture().awaitSingle()
    }

    private fun getAuthHeaderValue(): String {
        val credentials = "${props.contour.auth.user}:${props.contour.auth.password}"
        val encodedAuth = Base64.getEncoder().encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
        return "Basic $encodedAuth"
    }

    companion object {
        val RSOCKET_AUTHENTICATION_MIME_TYPE: MimeType =
            MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)
    }
}
