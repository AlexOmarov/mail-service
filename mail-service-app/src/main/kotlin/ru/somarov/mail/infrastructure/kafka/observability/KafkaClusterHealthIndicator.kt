package ru.somarov.mail.infrastructure.kafka.observability

import kotlinx.coroutines.reactor.mono
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.common.KafkaFuture
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import reactor.core.publisher.Mono
import ru.somarov.mail.infrastructure.config.ServiceProps
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class KafkaClusterHealthIndicator(private val kafkaProps: ServiceProps.KafkaProps) : ReactiveHealthIndicator {

    private val adminClient: AdminClient

    init {
        val configs: MutableMap<String, Any> = HashMap()
        configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProps.brokers
        adminClient = AdminClient.create(configs)
    }

    override fun health(): Mono<Health> {
        return mono { checkKafkaHealth() }
            .timeout(Duration.ofMillis(kafkaProps.healthTimeoutMillis))
            .onErrorResume { Mono.just(Health.down(IllegalStateException("Kafka unavailable")).build()) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun checkKafkaHealth(): Health {
        return try {
            adminClient.describeCluster().clusterId().await()
            Health.up().build()
        } catch (ex: Exception) {
            Health.down(ex).build()
        }
    }

    private suspend fun <T> KafkaFuture<T>.await(): T = suspendCoroutine { continuation ->
        this.whenComplete { result, exception ->
            if (exception != null) {
                continuation.resumeWithException(exception)
            } else {
                continuation.resume(result)
            }
        }
    }
}
