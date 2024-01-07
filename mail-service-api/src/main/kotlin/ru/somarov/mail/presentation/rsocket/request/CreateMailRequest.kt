package ru.somarov.mail.presentation.rsocket.request

data class CreateMailRequest(val text: String, val email: String) : java.io.Serializable
