package ru.somarov.mail.infrastructure.db.repo

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import ru.somarov.mail.infrastructure.db.entity.Mail
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface MailRepo : CoroutineCrudRepository<Mail, UUID> {
    fun findAllByMailStatusIdAndCreationDateAfter(
        mailStatusId: UUID,
        creationDate: OffsetDateTime,
        page: Pageable
    ): Flow<Mail>
}
