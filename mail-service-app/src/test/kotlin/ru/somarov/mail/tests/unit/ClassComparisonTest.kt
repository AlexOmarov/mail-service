package ru.somarov.mail.tests.unit

import org.junit.jupiter.api.Test
import ru.somarov.mail.presentation.dto.event.RetryMessage
import kotlin.reflect.KClass

class ClassComparisonTest {

    @Test
    @Suppress("UNCHECKED_CAST") // Check if generic class comparison works
    fun `Class comparison works for generics`() {
        assert(
            supports(
                RetryMessage::class as KClass<RetryMessage<Any>>,
                RetryMessage::class as KClass<RetryMessage<Any>>
            )
        )
    }

    private fun <R : Any, T : Any> supports(fr: KClass<R>, sec: KClass<T>): Boolean {
        return fr == sec
    }
}
