package ru.somarov.mail.presentation.rsocket.response

import java.util.UUID

data class MailRsocketDto(
    val id: UUID,
    val text: String
) : java.io.Serializable
