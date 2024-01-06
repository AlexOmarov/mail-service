package ru.somarov.mail.util

import io.grpc.Metadata
import net.devh.boot.grpc.client.inject.GrpcClient
import net.devh.boot.grpc.common.security.SecurityConstants.AUTHORIZATION_HEADER
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.presentation.grpc.CreateMailRequest
import ru.somarov.mail.presentation.grpc.MailResponse
import ru.somarov.mail.presentation.grpc.MailServiceGrpcKt
import java.util.Base64

@Service
class GrpcTestClient(private val props: ServiceProps) {
    @GrpcClient("mail-service")
    private lateinit var currentServiceClient: MailServiceGrpcKt.MailServiceCoroutineStub

    suspend fun createMail(request: CreateMailRequest): MailResponse {
        val auth = props.contour.auth.user + ":" + props.contour.auth.password
        val encodedAuth = Base64.getEncoder().encode(auth.encodeToByteArray())
        val authHeader = "Basic " + String(encodedAuth)
        val metadata = Metadata().also { it.put(AUTHORIZATION_HEADER, authHeader) }
        return currentServiceClient.createMail(request, metadata)
    }
}
