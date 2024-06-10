package ru.somarov.mail.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable
import ru.somarov.mail.serialization.UUIDSerializer
import java.util.UUID

@Serializable
@Schema(description = "Dto which represents mail in system")
data class Mail(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val text: String
) : java.io.Serializable
