package ru.somarov.mail.presentation.rsocket

import jakarta.validation.Valid
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.presentation.dto.request.CreateMailRequest
import ru.somarov.mail.presentation.dto.response.Mail
import ru.somarov.mail.presentation.dto.response.MailResponse
import java.util.UUID

@Controller
@Validated
private class MailRsocketController(val service: MailService) {

    @MessageMapping("mail")
    suspend fun createMail(@Payload @Valid request: CreateMailRequest, requester: RSocketRequester): MailResponse {
        val mail = service.createMail(request.email, request.text)
        return MailResponse(Mail(mail.uuid, mail.text))
    }

    @MessageMapping("mail.{id}")
    suspend fun getMail(@DestinationVariable id: UUID): MailResponse {
        val mail = service.getMail(id)
        return MailResponse(Mail(mail.uuid, mail.text))
    }
}
