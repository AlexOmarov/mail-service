package ru.somarov.mail.presentation.kafka

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import ru.somarov.mail.presentation.kafka.event.CommonEvent

data class RetryMessage<T : CommonEvent>(
    @field:Valid
    val payload: T,
    @field:NotBlank
    val key: String,
    var processingAttemptNumber: Int
)
