package ru.somarov.mail

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication
class MailServiceApplication

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    @Suppress("SpreadOperator")
    runApplication<MailServiceApplication>(*args)
}
