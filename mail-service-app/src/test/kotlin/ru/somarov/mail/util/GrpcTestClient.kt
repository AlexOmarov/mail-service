package ru.somarov.mail.util

import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service
import ru.somarov.mail.presentation.grpc.MailServiceGrpcKt
import ru.somarov.mail.presentation.grpc.RegisterMailRequest
import ru.somarov.mail.presentation.grpc.RegisterMailResponse

@Service
class GrpcTestClient {
    @GrpcClient("mail-service")
    private lateinit var currentServiceClient: MailServiceGrpcKt.MailServiceCoroutineStub

    suspend fun registerMail(request: RegisterMailRequest): RegisterMailResponse {
        return currentServiceClient.registerMail(request)
    }
}
