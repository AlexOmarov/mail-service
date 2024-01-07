package ru.somarov.mail.presentation.grpc

import net.devh.boot.grpc.server.service.GrpcService
import ru.somarov.mail.application.service.MailService
import java.util.UUID

@GrpcService
private class MailServiceGrpcServer(
    private val service: MailService
) : MailServiceGrpcKt.MailServiceCoroutineImplBase() {

    // TODO: make reactive grpc authorization
    // For now DefaultAuthenticatingServerInterceptor relies on ThreadLocal, which doesn't
    // stack with an AuthorizationManagerBeforeReactiveMethodInterceptor of reactive spring security.
    // It is tricky, because interceptors are executed in separate thread pool,
    // and then pass execution to default coroutine scope for executing suspend functions (ServerCalls class).
    // There we can only pass SecurityContext to CoroutineContext, and not reactor one. Reactor context occurs when
    // PreAuthorize CGLIB proxy is invoked.
    // It can work with suspend functions, even pass given context to proxied coroutine
    // (CoroutineUtils#invokeSuspendingFunction).
    // But it doesn't fill reactive security context holder there
    // @PreAuthorize("hasAuthority('ROLE_USER')")
    override suspend fun createMail(request: CreateMailRequest): MailResponse {
        val mail = service.createMail(request.email, request.text)
        return MailResponse.newBuilder()
            .setMail(MailDto.newBuilder().setId(mail.id.toString()).setText(mail.text))
            .build()
    }

    override suspend fun getMail(request: GetMailRequest): MailResponse {
        val mail = service.getMail(UUID.fromString(request.id))
        return MailResponse.newBuilder()
            .setMail(MailDto.newBuilder().setId(mail.id.toString()).setText(mail.text))
            .build()
    }
}
