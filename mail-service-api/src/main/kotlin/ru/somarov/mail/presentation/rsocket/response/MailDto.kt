package ru.somarov.mail.presentation.rsocket.response

import java.util.UUID

data class MailDto(
    val id: UUID,
    val text: String
)
