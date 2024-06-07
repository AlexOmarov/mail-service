package ru.somarov.mail.presentation.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Object which holds details of error which has been thrown")
data class ErrorResponse(val details: Map<String, String>) : java.io.Serializable
