package ru.somarov.mail.util

import io.grpc.Metadata
import net.devh.boot.grpc.client.inject.GrpcClient
import net.devh.boot.grpc.common.security.SecurityConstants
import org.springframework.stereotype.Service
import ru.somarov.mail.presentation.grpc.CreateMailRequest
import ru.somarov.mail.presentation.grpc.MailResponse
import ru.somarov.mail.presentation.grpc.MailServiceGrpcKt
import ru.somarov.mail.util.BasicAuthCreator.createBasicAuthString

@Service
class TestGrpcClient {
    @GrpcClient("mail-service")
    lateinit var currentServiceClient: MailServiceGrpcKt.MailServiceCoroutineStub

    suspend fun createMail(request: CreateMailRequest, user: String, password: String): MailResponse {
        val authHeader = createBasicAuthString(user, password)
        val metadata = Metadata().also { it.put(SecurityConstants.AUTHORIZATION_HEADER, authHeader) }
        return currentServiceClient.createMail(request, metadata)
    }
}
