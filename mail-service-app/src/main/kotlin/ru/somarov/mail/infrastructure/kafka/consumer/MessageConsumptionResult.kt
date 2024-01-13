package ru.somarov.mail.infrastructure.kafka.consumer

data class MessageConsumptionResult(val code: MessageConsumptionResultCode) {
    enum class MessageConsumptionResultCode { OK, FAILED }
}
