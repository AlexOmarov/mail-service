package ru.somarov.mail.base

import com.redis.testcontainers.RedisContainer
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.aop.AopInvocationException
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.util.ReflectionUtils
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Hooks
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.presentation.kafka.consumers.CreateMailCommandConsumerWithRetrySupport
import ru.somarov.mail.util.TestGrpcClient
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.time.Duration
import java.util.Properties

@Testcontainers
@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseIntegrationTest {

    init {
        // Still doesn't add header to request with this, fix is needed
        Hooks.enableAutomaticContextPropagation()
    }

    @Autowired
    lateinit var dbClient: DatabaseClient

    @Autowired
    lateinit var props: ServiceProps

    @MockBean
    lateinit var emailSenderImpl: JavaMailSenderImpl

    @Autowired
    lateinit var grpcClient: TestGrpcClient

    @SpyBean
    lateinit var createMailConsumer: CreateMailCommandConsumerWithRetrySupport

    @BeforeAll
    fun setUp() {
        // Wait for consumer to load (not to start consuming)
        await.await().timeout(Duration.ofSeconds(360)).atMost(Duration.ofSeconds(360))
            .untilAsserted {
                Assertions.assertAll(
                    { verify(createMailConsumer, times(1)).getReceiver() }
                )
            }
    }

    @AfterAll
    fun teardown() {
        reset(createMailConsumer)
    }

    @BeforeEach
    fun setup() {
        // Schedulers can start when after cleanup is already done
        // So we need to clean table up in beforeEach method as well
        dbClient.sql { "TRUNCATE mail CASCADE" }.then().block()
        spyBeanWorkAround() // For SpyBean usage https://github.com/spring-projects/spring-framework/issues/31713

        val message = MimeMessage(Session.getDefaultInstance(Properties()))

        doReturn(message).whenever(emailSenderImpl).createMimeMessage()
        doNothing().whenever(emailSenderImpl).send(anyVararg<MimeMessage>())
        beforeEach()
    }

    @AfterEach
    fun cleanUp() {
        unmockkAll() // For SpyBean usage https://github.com/spring-projects/spring-framework/issues/31713
        cleanAfterEach()
    }

    fun cleanAfterEach() {
        dbClient.sql { "TRUNCATE mail CASCADE" }.then().block()
        reset(emailSenderImpl)
    }

    abstract fun beforeEach()

    // Remove when https://github.com/spring-projects/spring-framework/issues/31713 will be fixed
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
        private var redis = RedisContainer(DockerImageName.parse("redis:6.2.6")).apply {
            withReuse(true)
            start()
        }
        private var kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.3")).apply {
            withReuse(true)
            start()
        }

        init {
            System.setProperty("kafka.brokers", kafka.bootstrapServers)

            System.setProperty("spring.redis.host", redis.host)
            System.setProperty("spring.redis.port", redis.firstMappedPort.toString())

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
