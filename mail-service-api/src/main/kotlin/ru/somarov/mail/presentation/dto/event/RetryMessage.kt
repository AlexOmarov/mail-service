package ru.somarov.mail.presentation.dto.event

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import kotlinx.serialization.Serializable
import org.springframework.validation.annotation.Validated

@Serializable
@Validated
data class RetryMessage<T>(
    @field:Valid
    val payload: T,
    @field:NotBlank
    val key: String,
    val attempt: Int
)
