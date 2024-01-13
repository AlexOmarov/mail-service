package ru.somarov.mail.presentation.rsocket.request

import jakarta.validation.constraints.NotBlank

data class CreateMailRequest(
    @field:NotBlank(message = "Text must not be blank")
    val text: String,
    @field:NotBlank(message = "Client email must not be blank")
    val email: String
) : java.io.Serializable
