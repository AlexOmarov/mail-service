package ru.somarov.mail.presentation.rsocket.response.standard

data class ErrorResponse(val details: Map<String, String>) : java.io.Serializable
