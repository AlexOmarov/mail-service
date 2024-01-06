package ru.somarov.mail.presentation.http.response.standard

import io.swagger.v3.oas.annotations.media.Schema
import ru.somarov.mail.presentation.rsocket.response.standard.ResponseMetadata

@Schema(description = "Standard response object with response in 'response' field and metadata")
data class StandardResponse<T>(val response: T, val metadata: ResponseMetadata)
