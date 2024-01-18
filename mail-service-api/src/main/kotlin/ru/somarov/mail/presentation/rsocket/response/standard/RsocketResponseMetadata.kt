package ru.somarov.mail.presentation.rsocket.response.standard

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Metadata of the standard response")
data class RsocketResponseMetadata(val code: RsocketResultCode, val systemMessage: String) : java.io.Serializable
