package ru.somarov.mail.infrastructure.kafka.observability

import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import reactor.core.Disposable
import reactor.core.publisher.Mono
import java.time.LocalDateTime

class KafkaReceiverHealthIndicator(private val disposable: Disposable) : ReactiveHealthIndicator {
    private val log = LoggerFactory.getLogger(KafkaReceiverHealthIndicator::class.java)
    override fun health(): Mono<Health> {
        log.info("Got ${LocalDateTime.now()} before check")
        val builder = Health.Builder()
        if (disposable.isDisposed) {
            builder.down()
        } else {
            builder.up()
        }
        return Mono.fromCallable {
            val build = builder.build()
            log.info("Got ${LocalDateTime.now()} after check")
            build
        }
    }
}
