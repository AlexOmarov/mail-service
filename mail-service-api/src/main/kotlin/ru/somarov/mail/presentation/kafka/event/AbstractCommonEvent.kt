package ru.somarov.mail.presentation.kafka.event

interface AbstractCommonEvent {
    fun getType(): EventType
}
