package ru.somarov.mail.util

import ru.somarov.mail.presentation.grpc.RegisterMailRequest

object DefaultEntitiesGenerator {
    fun createRegisterMailRequest(token: String = "token", text: String = "text"): RegisterMailRequest {
        return RegisterMailRequest.newBuilder()
            .setToken(token)
            .setText(text)
            .build()
    }
}
