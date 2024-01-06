package ru.somarov.mail.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties
data class ServiceProps(val contour: ContourProps) {
    data class ContourProps(
        val instance: String,
        val scheduling: SchedulingProps,
        val mail: MailProps,
        val cache: CacheProps,
        val rsocket: RSocketProps,
        val auth: AuthProps
    )

    data class MailProps(
        val host: String,
        val destinationEmail: String,
        val port: Int,
        val username: String,
        val password: String,
        val protocol: String,
        val tlsEnabled: Boolean,
        val debugEnabled: Boolean,
        val authEnabled: Boolean
    )

    data class SchedulingProps(
        val enabled: Boolean,
        val emailSending: EmailSendingSchedulerProps,
        val threadPoolSize: Int,
    )

    data class RSocketProps(
        val uri: String,
    )

    data class AuthProps(
        val user: String,
        val roles: List<String>,
        val password: String
    )

    data class CacheProps(
        val defaultTtl: Duration,
        val host: String,
        val port: Int,
        val password: String
    )

    data class EmailSendingSchedulerProps(val enabled: Boolean, val daysToCheckForUnsentEmails: Int, val batchSize: Int)
}
