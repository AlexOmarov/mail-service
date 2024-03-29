package ru.somarov.mail.presentation.controller

import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.presentation.http.request.CreateMailRequest
import ru.somarov.mail.presentation.http.response.MailDto
import ru.somarov.mail.presentation.http.response.MailResponse
import ru.somarov.mail.presentation.http.response.standard.ResponseMetadata
import ru.somarov.mail.presentation.http.response.standard.ResultCode
import ru.somarov.mail.presentation.http.response.standard.StandardResponse
import java.util.UUID

@RestController
@RequestMapping("/mails")
@Validated
private class MailController(private val service: MailService) : ISwaggerMailController {
    private val logger = LoggerFactory.getLogger(MailController::class.java)

    @GetMapping("{id}")
    override suspend fun getMail(@PathVariable id: UUID): StandardResponse<MailResponse> {
        logger.info("Got getMail request for id $id")
        val mail = service.getMail(id)
        return StandardResponse(
            MailResponse(MailDto(mail.uuid, mail.text)), ResponseMetadata(ResultCode.OK, "")
        )
    }
    @PostMapping
    override suspend fun createMail(@RequestBody request: CreateMailRequest): StandardResponse<MailResponse> {
        logger.info("Got create mail http request with body $request")
        val mail = service.createMail(request.email, request.text)
        return StandardResponse(
            MailResponse(MailDto(mail.uuid, mail.text)), ResponseMetadata(ResultCode.OK, "")
        )
    }
}
