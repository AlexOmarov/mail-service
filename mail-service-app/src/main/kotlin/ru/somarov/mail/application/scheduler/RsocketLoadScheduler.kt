package ru.somarov.mail.application.scheduler

import io.micrometer.observation.ObservationRegistry
import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveAndAwaitOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.stereotype.Component
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.presentation.rsocket.request.CreateMailRequest
import ru.somarov.mail.presentation.rsocket.response.MailResponse
import ru.somarov.mail.presentation.rsocket.response.standard.StandardResponse

@Component
@ConditionalOnExpression("\${contour.scheduling.rsocket-requester.enabled} and \${contour.scheduling.enabled}")
private class RsocketLoadScheduler(
    private val requester: RSocketRequester,
    private val props: ServiceProps,
    private val observationRegistry: ObservationRegistry
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @SchedulerLock(
        name = "RsocketLoadScheduler",
        lockAtMostFor = "\${contour.scheduling.rsocket-requester.lock-max-duration}"
    )
    @Scheduled(fixedDelayString = "\${contour.scheduling.rsocket-requester.delay}", zone = "UTC")
    fun launch() {
        runBlocking {
            logger.info("Started RsocketLoadScheduler")
            val credentials = UsernamePasswordMetadata(props.contour.auth.user, props.contour.auth.password)
            val response = requester
                .route("mail")
                .metadata(credentials, RSOCKET_AUTHENTICATION_MIME_TYPE)
                .data(CreateMailRequest("text", "email"))
                .retrieveAndAwaitOrNull<StandardResponse<MailResponse>>()
            logger.info("RsocketLoadScheduler has been completed, got $response")
        }
    }

    companion object {
        val RSOCKET_AUTHENTICATION_MIME_TYPE: MimeType =
            MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)

        val RSOCKET_TRACING_MIME_TYPE: MimeType =
            MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_TRACING_ZIPKIN.string)
    }
}