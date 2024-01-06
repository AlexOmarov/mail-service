package ru.somarov.mail.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.infrastructure.db.entity.Mail
import java.util.UUID

@Service
class MailService(private val dao: Dao) {
    private val log = LoggerFactory.getLogger(MailService::class.java)

    suspend fun createMail(email: String, text: String): Mail {
        log.info("Got register mail request with following text: $text, and mail: $email")
        return dao.createMail(email, text)
    }

    suspend fun getMail(id: UUID): Mail {
        log.info("Got get mail request with following id: $id")
        return dao.getMail(id)
    }
}
