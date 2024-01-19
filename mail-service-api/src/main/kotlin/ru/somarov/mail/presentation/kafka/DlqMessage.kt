package ru.somarov.mail.presentation.kafka

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class DlqMessage<T : Any>(
    @field:Valid
    val payload: T,
    @field:NotBlank
    val key: String,
    var processingAttemptNumber: Int
)
