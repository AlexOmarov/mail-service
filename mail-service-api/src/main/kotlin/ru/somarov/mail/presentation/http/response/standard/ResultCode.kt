package ru.somarov.mail.presentation.http.response.standard

import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "Enum which represents end status of operation - whether it was succeeded or not")
enum class ResultCode {
    OK, FAILED
}
