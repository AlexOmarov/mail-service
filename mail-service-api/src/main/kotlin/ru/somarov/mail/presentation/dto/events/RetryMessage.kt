package ru.somarov.mail.presentation.dto.events

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class RetryMessage<T>(
    @field:Valid
    val payload: T,
    @field:NotBlank
    val key: String,
    var processingAttemptNumber: Int
)
