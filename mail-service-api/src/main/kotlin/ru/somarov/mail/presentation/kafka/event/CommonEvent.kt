package ru.somarov.mail.presentation.kafka.event

fun interface CommonEvent {
    fun getType(): EventType
}
