package ru.somarov.mail.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable

@Serializable
@Schema(description = "Response to operations with mail")
data class MailResponse(
    val mail: Mail
) : java.io.Serializable
