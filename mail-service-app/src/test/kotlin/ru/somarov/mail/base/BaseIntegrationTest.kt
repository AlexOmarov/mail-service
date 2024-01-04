package ru.somarov.mail.base

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.aop.AopInvocationException
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.util.ReflectionUtils
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import ru.somarov.mail.util.GrpcTestClient
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Properties

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaseIntegrationTest {

    @Autowired
    lateinit var grpcTestClient: GrpcTestClient

    @Autowired
    lateinit var dbClient: DatabaseClient

    @MockBean
    lateinit var emailSender: JavaMailSenderImpl

    @BeforeEach
    fun setup() {
        spyBeanWorkAround() // For SpyBean usage https://github.com/spring-projects/spring-framework/issues/31713

        val message = MimeMessage(Session.getDefaultInstance(Properties()))

        doReturn(message).whenever(emailSender).createMimeMessage()
        doNothing().whenever(emailSender).send(anyVararg<MimeMessage>())
    }

    @AfterEach
    fun cleanUp() {
        unmockkAll() // For SpyBean usage https://github.com/spring-projects/spring-framework/issues/31713
        cleanAfterEach()
    }

    fun cleanAfterEach() {
        dbClient.sql { "TRUNCATE mail CASCADE" }.then().block()
        reset(emailSender)
    }

    // TODO: Remove when https://github.com/spring-projects/spring-framework/issues/31713 will be fixed
    @Suppress("ThrowsCount")
    private fun spyBeanWorkAround() {
        mockkStatic(AopUtils::class)
        every { AopUtils.invokeJoinpointUsingReflection(any(), any(), any()) }.answers {
            it.invocation.args
            val method = it.invocation.args[1] as Method
            val args = it.invocation.args[2] as Array<*>
            val target = it.invocation.args[0]
            try {
                ReflectionUtils.makeAccessible(method)
                method.invoke(target, *args)
            } catch (ex: InvocationTargetException) {
                // Invoked method threw a checked exception. We must rethrow it. The client won't see the interceptor.
                throw ex.targetException
            } catch (ex: IllegalArgumentException) {
                throw AopInvocationException(
                    "AOP configuration seems to be invalid: tried calling method [" +
                        method + "] on target [" + target + "]", ex
                )
            } catch (ex: IllegalAccessException) {
                throw AopInvocationException("Could not access method [$method]", ex)
            }
        }
    }

    companion object {
        private var postgresql = PostgreSQLContainer<Nothing>("postgres:15.2").apply {
            withReuse(true)
            start()
        }

        init {
            System.setProperty("spring.flyway.url", postgresql.jdbcUrl)
            System.setProperty("spring.flyway.user", postgresql.username)
            System.setProperty("spring.flyway.password", postgresql.password)
            System.setProperty(
                "spring.r2dbc.url",
                "r2dbc:postgresql://${postgresql.host}:${postgresql.firstMappedPort}/${postgresql.databaseName}"
            )
            System.setProperty("spring.r2dbc.username", postgresql.username)
            System.setProperty("spring.r2dbc.password", postgresql.password)
        }
    }
}
