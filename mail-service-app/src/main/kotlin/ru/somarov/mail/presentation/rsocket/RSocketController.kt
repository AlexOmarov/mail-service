package ru.somarov.mail.presentation.rsocket

import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.presentation.rsocket.request.CreateMailRequest
import ru.somarov.mail.presentation.rsocket.response.MailDto
import ru.somarov.mail.presentation.rsocket.response.MailResponse
import ru.somarov.mail.presentation.rsocket.response.standard.ResponseMetadata
import ru.somarov.mail.presentation.rsocket.response.standard.ResultCode
import ru.somarov.mail.presentation.rsocket.response.standard.StandardResponse
import java.util.UUID

@Controller
class RSocketController(val service: MailService) {
    private val logger = LoggerFactory.getLogger(RSocketController::class.java)

    @MessageMapping("mail")
    suspend fun createMail(@Payload request: CreateMailRequest): StandardResponse<MailResponse> {
        logger.info("Got create mail rsocket request with payload $request")
        val mail = service.createMail(request.email, request.text)
        return StandardResponse(
            MailResponse(MailDto(mail.id, mail.text)),
            ResponseMetadata(ResultCode.OK, "")
        )
    }

    @MessageMapping("mail.{id}")
    suspend fun getMail(@DestinationVariable id: UUID): StandardResponse<MailResponse> {
        logger.info("Got get mail rsocket request with id $id")
        val mail = service.getMail(id)
        return StandardResponse(
            MailResponse(MailDto(mail.id, mail.text)),
            ResponseMetadata(ResultCode.OK, "")
        )
    }
}
