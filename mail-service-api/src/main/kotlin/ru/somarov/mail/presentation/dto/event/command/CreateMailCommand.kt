package ru.somarov.mail.presentation.dto.event.command

import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated

@Validated
data class CreateMailCommand(
    @field:NotBlank(message = "Client email must not be blank")
    val email: String,
    @field:NotBlank(message = "Text must not be blank")
    val text: String
) : java.io.Serializable
