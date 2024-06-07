package ru.somarov.mail.presentation.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Response to operations with mail")
data class CreateMailRequest(
    @field:NotBlank(message = "Text must not be blank")
    val text: String,
    @field:NotBlank(message = "Client email must not be blank")
    val email: String
) : java.io.Serializable
