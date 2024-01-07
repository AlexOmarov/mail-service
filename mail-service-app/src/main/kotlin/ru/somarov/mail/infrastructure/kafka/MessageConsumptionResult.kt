package ru.somarov.mail.infrastructure.kafka

data class MessageConsumptionResult(val code: MessageConsumptionResultCode) {
    enum class MessageConsumptionResultCode { OK, FAILED }
}
