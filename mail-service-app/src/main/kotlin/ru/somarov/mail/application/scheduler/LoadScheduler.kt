package ru.somarov.mail.application.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.observation.ObservationRegistry
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
import reactor.kafka.sender.SenderResult
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.Metadata
import ru.somarov.mail.infrastructure.kafka.Producer
import ru.somarov.mail.infrastructure.kafka.Producer.ProducerProps
import ru.somarov.mail.presentation.dto.event.command.CreateMailCommand
import ru.somarov.mail.presentation.dto.request.CreateMailRequest
import ru.somarov.mail.presentation.dto.response.MailResponse
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

/**
 * Scheduler, which isn't a part of application base, but exists for imitating a load on service's APIs
 * */
@Component
@ConditionalOnExpression("\${contour.scheduling.load.enabled} and \${contour.scheduling.enabled}")
private class LoadScheduler(
    private val requester: RSocketRequester,
    private val props: ServiceProps,
    mapper: ObjectMapper,
    registry: ObservationRegistry,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val mailProducer = Producer<CreateMailCommand>(
        mapper,
        ProducerProps(props.kafka.brokers, props.kafka.sender.maxInFlight, props.kafka.mailBroadcastTopic), registry
    )
    private val poisonPillProducer = Producer<String>(
        mapper,
        ProducerProps(props.kafka.brokers, props.kafka.sender.maxInFlight, props.kafka.mailBroadcastTopic), registry
    )

    init {
        val producerProps: MutableMap<String, Any> = HashMap()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = props.kafka.brokers
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
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

                val rsocketGetMailResponse = getMailUsingRsocket(rsocketCreateMailResponse.mail.id)
                logger.info("Rsocket get mail response: $rsocketGetMailResponse")

                logger.info("LoadScheduler has been completed")
            }
        } catch (e: Exception) {
            logger.error("Got exception while processing Load scheduler: $e")
        }
    }

    private suspend fun sendPoisonPillUsingKafka(): SenderResult<UUID> {
        return poisonPillProducer.send("Poison pill", Metadata(OffsetDateTime.now(), "key", 0))
    }

    private suspend fun createMailUsingKafka(): SenderResult<UUID> {
        return mailProducer.send(CreateMailCommand("email", "text"), Metadata(OffsetDateTime.now(), "key", 0))
    }

    private suspend fun getMailUsingRsocket(id: UUID): MailResponse {
        val credentials = UsernamePasswordMetadata(props.contour.auth.user, props.contour.auth.password)
        // Had to use retrieveMono contextCapture to capture observation,
        // because observation requester proxy creates mono for request using mono defer contextual
        return requester
            .route("mail.$id")
            .metadata(credentials, RSOCKET_AUTHENTICATION_MIME_TYPE)
            .retrieveMono<MailResponse>()
            .contextCapture().awaitSingle()
    }

    private suspend fun createMailUsingHttp(): MailResponse {
        val webClient = WebClient.builder().baseUrl("http://localhost:${props.contour.http.port}").build()
        val request = CreateMailRequest("text", "test@gmail.com")
        return webClient.post()
            .uri("/mails")
            .body(BodyInserters.fromValue(request))
            .header(HttpHeaders.AUTHORIZATION, getAuthHeaderValue())
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<MailResponse>() {})
            .awaitSingle()
    }

    private suspend fun createMailUsingRsocket(): MailResponse {
        val credentials = UsernamePasswordMetadata(props.contour.auth.user, props.contour.auth.password)
        // Had to use retrieveMono contextCapture to capture observation,
        // because observation requester proxy creates mono for request using mono defer contextual
        return requester
            .route("mail")
            .metadata(credentials, RSOCKET_AUTHENTICATION_MIME_TYPE)
            .data(CreateMailRequest("text", "email"))
            .retrieveMono<MailResponse>()
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
