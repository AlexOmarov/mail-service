package ru.somarov.mail.presentation.http

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import ru.somarov.mail.presentation.dto.request.CreateMailRequest
import ru.somarov.mail.presentation.dto.response.MailResponse
import java.util.UUID

@Tag(name = "Mail Controller", description = "Mail management APIs")
@SecurityRequirement(name = "basicAuth")
interface ISwaggerMailController {
    @Operation(
        summary = "Retrieve a Mail by Id",
        description = "Get a Mail object by specifying its id. The response is MailResponse object, inside it " +
            "there is MailDto field with id and text."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Operation is successfully processed"
            ),
            ApiResponse(
                responseCode = "456",
                description = "Some error happened inside method."
            )
        ]
    )
    suspend fun getMail(
        @Parameter(description = "Mail uuid identificator") @PathVariable(required = true) id: UUID
    ): MailResponse

    @Operation(
        summary = "Retrieve a Mail by Id",
        description = "Get a Mail object by specifying its id. The response is MailResponse object, inside it " +
            "there is MailDto field with id and text."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Operation is successfully processed"
            ),
            ApiResponse(
                responseCode = "456",
                description = "Some error happened inside method."
            )
        ]
    )
    suspend fun createMail(
        @Parameter(description = "Data for mail creation") @RequestBody request: CreateMailRequest
    ): MailResponse
}
