package ru.somarov.mail.application.scheduler

import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveMono
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
@ConditionalOnExpression("\${contour.scheduling.load.enabled} and \${contour.scheduling.enabled}")
private class LoadScheduler(
    private val requester: RSocketRequester,
    private val props: ServiceProps
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @SchedulerLock(
        name = "LoadScheduler",
        lockAtMostFor = "\${contour.scheduling.load.lock-max-duration}"
    )
    @Scheduled(fixedDelayString = "\${contour.scheduling.load.delay}", zone = "UTC")
    fun launch() {
        runBlocking {
            logger.info("Started LoadScheduler")
            val credentials = UsernamePasswordMetadata(props.contour.auth.user, props.contour.auth.password)
            // Had to use retrieveMono contextCapture to capture observation,
            // because observation requester proxy creates mono for request using mono defer contextual
            val result = requester
                .route("mail")
                .metadata(credentials, RSOCKET_AUTHENTICATION_MIME_TYPE)
                .data(CreateMailRequest("text", "email"))
                .retrieveMono<StandardResponse<MailResponse>>()
                .contextCapture().awaitSingleOrNull()
            logger.info("RsocketLoadScheduler has been completed, got $result")
        }
    }

    companion object {
        val RSOCKET_AUTHENTICATION_MIME_TYPE: MimeType =
            MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)
    }
}
