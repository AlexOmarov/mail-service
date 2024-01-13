package ru.somarov.mail.presentation.http.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID
@Schema(description = "Dto which represents mail in system")
data class MailDto(
    val id: UUID,
    val text: String
)
