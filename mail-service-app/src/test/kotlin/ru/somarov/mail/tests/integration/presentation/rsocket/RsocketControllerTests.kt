package ru.somarov.mail.tests.integration.presentation.rsocket

import io.rsocket.exceptions.ApplicationErrorException
import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.messaging.rsocket.retrieveMono
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.presentation.rsocket.response.MailRsocketResponse
import ru.somarov.mail.presentation.rsocket.response.standard.StandardRsocketResponse

class RsocketControllerTests : BaseIntegrationTest() {

    @SpyBean
    lateinit var dao: Dao

    override fun beforeEach() {
        reset(dao)
    }

    @Test
    fun `When call API without authorization then 401`() {
        assertThrows<ApplicationErrorException> {
            runBlocking {
                requester
                    .route("mail")
                    .data(ru.somarov.mail.presentation.rsocket.request.CreateMailRequest("text", "email"))
                    .retrieveMono<StandardRsocketResponse<MailRsocketResponse>>()
                    .contextCapture().awaitSingle()
            }
        }
    }

    @Test
    fun `When create mail request comes then respond with mail data`() {
        val text = "text"
        val credentials = UsernamePasswordMetadata(props.contour.auth.user, props.contour.auth.password)
        val result = runBlocking {
            requester
                .route("mail")
                .metadata(credentials, RSOCKET_AUTHENTICATION_MIME_TYPE)
                .data(ru.somarov.mail.presentation.rsocket.request.CreateMailRequest("text", "email"))
                .retrieveMono<StandardRsocketResponse<MailRsocketResponse>>()
                .contextCapture().awaitSingle().response
        }

        assert(result.mail.text == text)
    }

    @Test
    fun `When create mail request comes then store it in database`() {
        val text = "text"
        val credentials = UsernamePasswordMetadata(props.contour.auth.user, props.contour.auth.password)
        val result = runBlocking {
            requester
                .route("mail")
                .metadata(credentials, RSOCKET_AUTHENTICATION_MIME_TYPE)
                .data(ru.somarov.mail.presentation.rsocket.request.CreateMailRequest("text", "email"))
                .retrieveMono<StandardRsocketResponse<MailRsocketResponse>>()
                .contextCapture().awaitSingle().response
        }
        verifyBlocking(dao, times(1)) {
            createMail(any(), eq(text))
        }
        val mail = runBlocking { dao.getMail(result.mail.id) }
        assert(mail.text == text)
    }

    @Test
    fun `When get mail request comes then return valid mail data`() {
        val mail = runBlocking { dao.createMail("email", "text") }
        val credentials = UsernamePasswordMetadata(props.contour.auth.user, props.contour.auth.password)
        val response = runBlocking {
            requester
                .route("mail.${mail.id}")
                .metadata(credentials, RSOCKET_AUTHENTICATION_MIME_TYPE)
                .retrieveMono<StandardRsocketResponse<MailRsocketResponse>>()
                .contextCapture().awaitSingle().response
        }

        assert(response.mail.id == mail.id)
        assert(response.mail.text == mail.text)

        verifyBlocking(dao, times(1)) {
            getMail(eq(mail.id))
        }
    }

    companion object {
        val RSOCKET_AUTHENTICATION_MIME_TYPE: MimeType =
            MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)
    }
}
