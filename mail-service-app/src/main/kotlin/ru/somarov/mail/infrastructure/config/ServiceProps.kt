package ru.somarov.mail.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties
data class ServiceProps(val contour: ContourProps) {
    data class ContourProps(val instance: String, val scheduling: SchedulingProps, val mail: MailProps)
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

    data class EmailSendingSchedulerProps(val enabled: Boolean, val daysToCheckForUnsentEmails: Int, val batchSize: Int)
}
