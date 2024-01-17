package ru.somarov.mail.util

import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service
import ru.somarov.mail.presentation.grpc.MailServiceGrpcKt

@Service
class TestGrpcClient {
    @GrpcClient("mail-service")
    lateinit var currentServiceClient: MailServiceGrpcKt.MailServiceCoroutineStub
}
