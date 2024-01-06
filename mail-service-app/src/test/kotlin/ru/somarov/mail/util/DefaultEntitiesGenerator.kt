package ru.somarov.mail.util

import ru.somarov.mail.presentation.grpc.CreateMailRequest

object DefaultEntitiesGenerator {
    fun createCreateMailRequest(email: String = "test@test.ru", text: String = "text"): CreateMailRequest {
        return CreateMailRequest.newBuilder()
            .setEmail(email)
            .setText(text)
            .build()
    }
}
