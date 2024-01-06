package ru.somarov.mail.presentation.http.response.standard

import io.swagger.v3.oas.annotations.media.Schema
import ru.somarov.mail.presentation.rsocket.response.standard.ResultCode

@Schema(description = "Metadata of the standard response")
data class ResponseMetadata(val code: ResultCode, val systemMessage: String)
