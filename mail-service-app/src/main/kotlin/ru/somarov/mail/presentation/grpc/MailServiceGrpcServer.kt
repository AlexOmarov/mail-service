package ru.somarov.mail.presentation.grpc

import net.devh.boot.grpc.server.service.GrpcService
import ru.somarov.mail.application.service.MailRegistrationService

@GrpcService
class MailServiceGrpcServer(
    private val service: MailRegistrationService
) : MailServiceGrpcKt.MailServiceCoroutineImplBase() {
    override suspend fun registerMail(request: RegisterMailRequest): RegisterMailResponse {
        return service.registerMail(request)
    }
}
