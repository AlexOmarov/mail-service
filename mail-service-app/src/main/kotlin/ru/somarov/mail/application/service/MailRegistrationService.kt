package ru.somarov.mail.application.service

import com.denumhub.appeal.infrastructure.db.entity.Appeal
import com.denumhub.appeal.infrastructure.db.entity.AppealChannel
import com.denumhub.appeal.infrastructure.db.entity.AppealStatus
import com.denumhub.appeal.infrastructure.db.repo.AppealRepo
import com.denumhub.appeal.presentation.grpc.RegisterAppealRequest
import com.denumhub.appeal.presentation.grpc.RegisterAppealResponse
import com.denumhub.response.UserProfileResponse
import com.denumhub.service.MonolithBackendAdapterApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MailRegistrationService(
    private val appealRepo: AppealRepo,
    private val monolithBackendAdapterApi: MonolithBackendAdapterApi
) {
    private val log = LoggerFactory.getLogger(MailRegistrationService::class.java)
    suspend fun registerMail(request: RegisterAppealRequest): RegisterAppealResponse {
        log.info("Gor registerAppeal request with following text: ${request.text}, and token: ${request.token}")
        val userInfo = monolithBackendAdapterApi.getUserProfile(request.token)

        val newAppeal = Appeal(
            id = UUID.randomUUID(),
            clientId = userInfo.id,
            clientEmail = userInfo.email,
            clientPhone = userInfo.phone,
            text = request.text,
            appealStatusId = AppealStatus.Companion.AppealStatusCode.NEW.id,
            isEmailSent = false,
            creationDate = OffsetDateTime.now(),
            lastUpdateDate = OffsetDateTime.now(),
            clientFio = formFio(userInfo),
            clientBirthday = userInfo.birthdate,
            appealChannelId = AppealChannel.Companion.AppealChannelCode.MOBILE.id
        )

        appealRepo.save(newAppeal)

        return RegisterAppealResponse.newBuilder()
            .setAppeal(com.denumhub.appeal.presentation.grpc.Appeal.newBuilder().setId(newAppeal.id.toString()))
            .build()
    }

    private fun formFio(userInfo: UserProfileResponse): String {
        var result = ""
        if (userInfo.surname != null) result = "${userInfo.surname}"
        if (userInfo.name != null) result = "$result ${userInfo.name}"
        if (userInfo.patronymic != null) result = "$result ${userInfo.patronymic}"
        return result
    }
}
