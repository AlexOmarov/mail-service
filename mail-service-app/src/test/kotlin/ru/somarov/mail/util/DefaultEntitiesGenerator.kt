package ru.somarov.mail.util

import ru.somarov.mail.presentation.grpc.RegisterMailRequest

object DefaultEntitiesGenerator {
    fun createRegisterMailRequest(email: String = "test@test.ru", text: String = "text"): RegisterMailRequest {
        return RegisterMailRequest.newBuilder()
            .setEmail(email)
            .setText(text)
            .build()
    }
}
