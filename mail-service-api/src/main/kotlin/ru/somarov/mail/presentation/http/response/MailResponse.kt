package ru.somarov.mail.presentation.http.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response to operations with mail")
data class MailResponse(val mail: MailDto)
