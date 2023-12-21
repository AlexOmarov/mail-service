package ru.somarov.mail.application.scheduler

import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.somarov.mail.application.service.EmailService
import ru.somarov.mail.infrastructure.config.ServiceProps
import java.time.OffsetDateTime

@Component
@ConditionalOnExpression("\${contour.scheduling.email-sending.enabled} and \${contour.scheduling.enabled}")
class EmailSendingScheduler(private val service: EmailService, private val props: ServiceProps) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @SchedulerLock(
        name = "EmailSendingScheduler",
        lockAtMostFor = "\${contour.scheduling.email-sending.lock-max-duration}"
    )
    @Scheduled(fixedDelayString = "\${contour.scheduling.email-sending.delay}", zone = "UTC")
    fun launch() {
        runBlocking {
            logger.info("Started EmailSendingScheduler")
            val count = service.sendNewEmails(
                OffsetDateTime.now()
                    .minusDays(props.contour.scheduling.emailSending.daysToCheckForUnsentEmails.toLong())
            )
            logger.info("EmailSendingScheduler has been completed. Send $count emails")
        }
    }
}
