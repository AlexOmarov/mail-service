package ru.somarov.mail.presentation.rsocket.response.standard

data class ErrorRsocketResponse(val details: Map<String, String>) : java.io.Serializable
