package com.denumhub.error.exception.technical

import com.denumhub.exception.AuthenticationBaseException
import com.denumhub.exception.DBUnavailabilityBaseException
import com.denumhub.exception.NoSuchByIdBaseException
import com.denumhub.exception.NotFoundDataBaseException
import com.denumhub.exception.UnavailableDeleteBaseException

enum class TechnicalExceptionRegistryMember(
    val code: Int,
    val message: String,
    val description: String,
    val exceptions: List<Class<out Throwable>>
) {
    AUTHENTICATION_EXCEPTION(
        code = 1001,
        message = "AUTHENTICATION_EXCEPTION message",
        description = "This exception is for cases when authentication fails",
        exceptions = listOf(AuthenticationBaseException::class.java)
    ),
    NOT_FOUND_DATA_EXCEPTION(
        code = 1003,
        message = "NOT_FOUND_DATA_EXCEPTION message",
        description = "This exception is for cases when no data has been found for given filters",
        exceptions = listOf(NotFoundDataBaseException::class.java)
    ),
    UNAVAILABLE_DELETE_EXCEPTION(
        code = 1004,
        message = "UNAVAILABLE_DELETE_EXCEPTION message",
        description = "This exception is for cases when we cannot delete a persistent entity based on passed params",
        exceptions = listOf(UnavailableDeleteBaseException::class.java)
    ),
    NO_SUCH_BY_ID_EXCEPTION(
        code = 1005,
        message = "NO_SUCH_BY_ID_EXCEPTION message",
        description = "This exception is for cases when there are no entity with given id",
        exceptions = listOf(NoSuchByIdBaseException::class.java)
    ),
    DB_UNAVAILABILITY_EXCEPTION(
        code = 1006,
        message = "DB_UNAVAILABILITY_EXCEPTION message",
        description = "This exception is for cases when database is unavailable",
        exceptions = listOf(DBUnavailabilityBaseException::class.java)
    ),
    BASE_TECHNICAL_EXCEPTION(
        code = 10000,
        message = "BASE_TECHNICAL_EXCEPTION message",
        description = "This exception is base for all of the unmapped system errors",
        exceptions = listOf()
    ),
    SIDE_SYSTEM_EXCEPTION(
        code = 10002,
        message = "SIDE_SYSTEM_EXCEPTION message",
        description = "This exception is for cases, when side system returned error",
        exceptions = listOf(TechnicalException::class.java)
    );

    companion object {
        fun getByCode(code: Int): TechnicalExceptionRegistryMember {
            return entries.firstOrNull { it.code == code } ?: BASE_TECHNICAL_EXCEPTION
        }

        fun getByException(exceptionClass: Class<out Throwable>): TechnicalExceptionRegistryMember {
            return entries.firstOrNull { it.exceptions.contains(exceptionClass) } ?: BASE_TECHNICAL_EXCEPTION
        }
    }
}
