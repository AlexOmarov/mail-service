package ru.somarov.mail.tests.unit

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.somarov.mail.application.util.OffsetDateTimeDeserializer
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class OffsetDatetimeDeserializationTests {

    @Test
    fun `When deserialize valid date then return OffsetDatetime object`() {
        val serializedDate = "2022-10-10T12:11:46.913155+05:00"
        val result = OffsetDateTimeDeserializer.parse(serializedDate)
        assert(result.isBefore(OffsetDateTime.now()))
    }

    @Test
    fun `When deserialize valid date with invalid format then throw exception`() {
        val serializedDate = "2022/10/10 00:00:00.000+0300"
        assertThrows<DateTimeParseException> {
            OffsetDateTimeDeserializer.parse(serializedDate)
        }
    }

    @Test
    fun `When deserialize invalid date then throw exception`() {
        val serializedDate = "WRONG"
        assertThrows<DateTimeParseException> {
            OffsetDateTimeDeserializer.parse(serializedDate)
        }
    }
}
