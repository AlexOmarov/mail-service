package ru.somarov.mail.presentation.dto.events.event.command

import jakarta.validation.constraints.NotBlank
import ru.somarov.mail.presentation.dto.events.event.CommonEvent

data class CreateMailCommand(
    @field:NotBlank(message = "Client email must not be blank")
    val email: String,
    @field:NotBlank(message = "Text must not be blank")
    val text: String
) : CommonEvent
