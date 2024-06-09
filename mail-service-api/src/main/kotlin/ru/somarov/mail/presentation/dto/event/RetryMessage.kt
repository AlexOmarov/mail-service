package ru.somarov.mail.presentation.dto.event

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated

@Validated
data class RetryMessage<T>(
    @field:Valid
    val payload: T,
    @field:NotBlank
    val key: String,
    val attempt: Int
)
