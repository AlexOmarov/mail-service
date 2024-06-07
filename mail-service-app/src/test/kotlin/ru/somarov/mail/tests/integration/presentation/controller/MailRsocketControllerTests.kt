package ru.somarov.mail.tests.integration.presentation.controller

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import reactor.test.StepVerifier
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.presentation.dto.request.CreateMailRequest
import ru.somarov.mail.presentation.dto.response.MailResponse
import ru.somarov.mail.util.BasicAuthCreator.createBasicAuthString

class MailRsocketControllerTests : BaseIntegrationTest() {

    @SpyBean
    lateinit var dao: Dao

    override fun beforeEach() {
        reset(dao)
    }

    @Test
    fun `When call API without authorization then 401`() {
        webClient.post().uri("/mails")
            .bodyValue(mapper.writeValueAsString(CreateMailRequest("text", "email")))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchange().expectStatus().is4xxClientError
    }

    @Test
    fun `When create mail request comes then respond with mail data`() {
        val text = "text"
        val ex = webClient.post().uri("/mails")
            .bodyValue(mapper.writeValueAsString(CreateMailRequest(text, "email")))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(
                HttpHeaders.AUTHORIZATION,
                createBasicAuthString(props.contour.auth.user, props.contour.auth.password)
            )
            .exchange().expectStatus().isOk

        StepVerifier.create(ex.returnResult(getStandardResponseMailResponseClass()).responseBody)
            .expectNextMatches { standardResponse -> standardResponse.mail.text == text }
            .verifyComplete()
    }

    @Test
    fun `When create mail request comes then store it in database`() {
        val text = "text"
        val ex = webClient.post().uri("/mails")
            .bodyValue(mapper.writeValueAsString(CreateMailRequest(text, "email")))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(
                HttpHeaders.AUTHORIZATION,
                createBasicAuthString(props.contour.auth.user, props.contour.auth.password)
            )
            .exchange().expectStatus().isOk
        val response = ex.returnResult(getStandardResponseMailResponseClass()).responseBody.blockFirst()!!
        verifyBlocking(dao, times(1)) {
            createMail(any(), eq(text))
        }
        val mail = runBlocking { dao.getMail(response.mail.id) }
        assert(mail.text == text)
    }

    @Test
    fun `When get mail request comes then return valid mail data`() {
        val mail = runBlocking { dao.createMail("email", "text") }
        val ex = webClient.get().uri("/mails/${mail.id}")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(
                HttpHeaders.AUTHORIZATION,
                createBasicAuthString(props.contour.auth.user, props.contour.auth.password)
            )
            .exchange().expectStatus().isOk
        val response =
            ex.returnResult(getStandardResponseMailResponseClass()).responseBody.blockFirst()!!

        assert(response.mail.id == mail.id)
        assert(response.mail.text == mail.text)

        verifyBlocking(dao, times(1)) {
            getMail(eq(mail.id))
        }
    }

    private fun getStandardResponseMailResponseClass(): ParameterizedTypeReference<MailResponse> {
        return object : ParameterizedTypeReference<MailResponse>() {}
    }
}
