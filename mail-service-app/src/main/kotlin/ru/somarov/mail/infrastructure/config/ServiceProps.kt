package ru.somarov.mail.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties
data class ServiceProps(val contour: ContourProps, val kafka: KafkaProps) {
    data class ContourProps(
        val instance: String,
        val scheduling: SchedulingProps,
        val mail: MailProps,
        val otlp: OtlpProps,
        val cache: CacheProps,
        val rsocket: RSocketProps,
        val http: HttpProps,
        val database: DbProps,
        val auth: AuthProps
    )

    data class HttpProps(
        val logging: HttpLoggingProps,
        val client: WebClientProps,
        val port: Int,
    )

    data class HttpLoggingProps(
        val exclusions: List<String>,
    )

    data class OtlpProps(
        val host: String,
        val logsPort: Int,
    )

    data class WebClientProps(
        val connectionTimeoutMillis: Int,
    )

    data class MailProps(
        val host: String,
        val template: String,
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

    data class KafkaProps(
        val brokers: String,
        val healthTimeoutMillis: Long,
        val healthWarmupTimeoutMillis: Long,
        val consumingEnabled: Boolean,
        val retryConsumingEnabled: Boolean,
        val createMailCommandConsumingEnabled: Boolean,
        val receiversRetrySettings: ReceiversRetryProps,
        val groupId: String,
        val maxPollRecords: Int,
        val offsetResetConfig: String,
        val commitInterval: Long,
        val retryHandlingInterval: Long,
        val retryResendNumber: Int,
        val sender: SenderProps,
        val mailBroadcastTopic: String,
        val createMailCommandTopic: String,
        val retryTopic: String,
        val dlqTopic: String
    )

    data class ReceiversRetryProps(
        val attempts: Long,
        val periodSeconds: Long,
        val jitter: Double,
    )

    data class SenderProps(
        val maxInFlight: Int
    )

    data class AuthProps(
        val user: String,
        val roles: List<String>,
        val password: String,
        val exclusions: List<String>,
    )

    data class CacheProps(
        val defaultTtl: Duration,
        val host: String,
        val port: Int,
        val password: String
    )

    data class DbProps(
        val schema: String
    )

    data class EmailSendingSchedulerProps(
        val enabled: Boolean,
        val daysToCheckForUnsentEmails: Int,
        val delay: Duration,
        val batchSize: Int
    )
}
