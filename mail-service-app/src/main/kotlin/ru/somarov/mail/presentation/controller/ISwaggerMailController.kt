package ru.somarov.mail.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import ru.somarov.mail.presentation.http.request.CreateMailRequest
import ru.somarov.mail.presentation.http.response.MailResponse
import ru.somarov.mail.presentation.rsocket.response.standard.StandardResponse
import java.util.UUID

interface ISwaggerMailController {

    @Operation(summary = "Get mail by id", description = "Returns 200 if successful")
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Successful Operation"
        )]
    )
    suspend fun getMail(@PathVariable id: UUID): StandardResponse<MailResponse>

    @Operation(summary = "Create new mail", description = "Returns 200 if successful")
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Successful Operation"
        )]
    )
    suspend fun createMail(@RequestBody request: CreateMailRequest): StandardResponse<MailResponse>
}
