package ru.somarov.mail.infrastructure.grpc.error.exception.technical

enum class TechnicalExceptionRegistryMember(
    val code: Int,
    val message: String,
    val description: String,
    val exceptions: List<Class<out Throwable>>
) {
    BASE_TECHNICAL_EXCEPTION(
        code = 10000,
        message = "BASE_TECHNICAL_EXCEPTION message",
        description = "This exception is base for all of the unmapped system errors",
        exceptions = listOf()
    ),
    SIDE_SYSTEM_EXCEPTION(
        code = 10001,
        message = "SIDE_SYSTEM_EXCEPTION message",
        description = "This exception is for cases, when side system returned error",
        exceptions = listOf(TechnicalException::class.java)
    );

    companion object {

        fun getByException(exceptionClass: Class<out Throwable>): TechnicalExceptionRegistryMember {
            return entries.firstOrNull { it.exceptions.contains(exceptionClass) } ?: BASE_TECHNICAL_EXCEPTION
        }
    }
}
