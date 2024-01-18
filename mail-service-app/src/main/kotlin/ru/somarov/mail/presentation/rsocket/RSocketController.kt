package ru.somarov.mail.presentation.rsocket

import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.presentation.rsocket.request.CreateMailRequest
import ru.somarov.mail.presentation.rsocket.response.MailRsocketDto
import ru.somarov.mail.presentation.rsocket.response.MailRsocketResponse
import ru.somarov.mail.presentation.rsocket.response.standard.RsocketResponseMetadata
import ru.somarov.mail.presentation.rsocket.response.standard.RsocketResultCode
import ru.somarov.mail.presentation.rsocket.response.standard.StandardRsocketResponse
import java.util.UUID

@Controller
class RSocketController(val service: MailService) {
    private val logger = LoggerFactory.getLogger(RSocketController::class.java)

    @MessageMapping("mail")
    suspend fun createMail(@Payload request: CreateMailRequest): StandardRsocketResponse<MailRsocketResponse> {
        logger.info("Got create mail rsocket request with payload $request")
        val mail = service.createMail(request.email, request.text)
        return StandardRsocketResponse(
            MailRsocketResponse(MailRsocketDto(mail.uuid, mail.text)),
            RsocketResponseMetadata(RsocketResultCode.OK, "")
        )
    }

    @MessageMapping("mail.{id}")
    suspend fun getMail(@DestinationVariable id: UUID): StandardRsocketResponse<MailRsocketResponse> {
        logger.info("Got get mail rsocket request with id $id")
        val mail = service.getMail(id)
        return StandardRsocketResponse(
            MailRsocketResponse(MailRsocketDto(mail.uuid, mail.text)),
            RsocketResponseMetadata(RsocketResultCode.OK, "")
        )
    }
}
