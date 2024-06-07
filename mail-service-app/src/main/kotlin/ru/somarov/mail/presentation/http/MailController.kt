package ru.somarov.mail.presentation.http

import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.presentation.dto.request.CreateMailRequest
import ru.somarov.mail.presentation.dto.response.Mail
import ru.somarov.mail.presentation.dto.response.MailResponse
import java.util.UUID

@RestController
@RequestMapping("/mails")
@Validated
private class MailController(private val service: MailService) : ISwaggerMailController {
    private val logger = LoggerFactory.getLogger(MailController::class.java)

    @GetMapping("{id}")
    override suspend fun getMail(@PathVariable id: UUID): MailResponse {
        logger.info("Got getMail request for id $id")
        val mail = service.getMail(id)
        return MailResponse(Mail(mail.uuid, mail.text))
    }

    @PostMapping
    override suspend fun createMail(@RequestBody request: CreateMailRequest): MailResponse {
        logger.info("Got create mail http request with body $request")
        val mail = service.createMail(request.email, request.text)
        return MailResponse(Mail(mail.uuid, mail.text))
    }
}
