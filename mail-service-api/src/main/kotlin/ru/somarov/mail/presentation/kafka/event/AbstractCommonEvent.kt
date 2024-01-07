package ru.somarov.mail.presentation.kafka.event

abstract class AbstractCommonEvent {
    abstract fun getType(): EventType
}
