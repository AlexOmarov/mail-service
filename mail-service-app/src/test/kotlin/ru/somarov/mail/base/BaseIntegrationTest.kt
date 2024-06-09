package ru.somarov.mail.base

import com.fasterxml.jackson.databind.ObjectMapper
import com.redis.testcontainers.RedisContainer
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Hooks
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.presentation.consumers.MailConsumer
import java.util.Properties

@Testcontainers
@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseIntegrationTest {

    private val log = LoggerFactory.getLogger(this.javaClass)

    init {
        // Still doesn't add header to request with this, fix is needed
        Hooks.enableAutomaticContextPropagation()
    }

    @Autowired
    lateinit var dbClient: DatabaseClient

    @Autowired
    lateinit var mapper: ObjectMapper

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var props: ServiceProps

    @MockBean
    lateinit var emailSenderImpl: JavaMailSenderImpl

    @SpyBean
    lateinit var mailConsumer: MailConsumer

    @SpyBean
    lateinit var requester: RSocketRequester

    @BeforeAll
    fun setUp() {
        // Wait for consumer to load (not to start consuming)
        log.info("Set up")
    }

    @AfterAll
    fun teardown() {
        reset(mailConsumer)
    }

    @BeforeEach
    fun setup() {
        // Schedulers can start when after cleanup is already done
        // So we need to clean table up in beforeEach method as well
        dbClient.sql { "TRUNCATE mail CASCADE" }.then().block()

        val message = MimeMessage(Session.getDefaultInstance(Properties()))

        doReturn(message).whenever(emailSenderImpl).createMimeMessage()
        doNothing().whenever(emailSenderImpl).send(any<MimeMessage>())
        doNothing().whenever(emailSenderImpl).send(any<MimeMessage>(), any<MimeMessage>())
        beforeEach()
    }

    @AfterEach
    fun cleanUp() {
        cleanAfterEach()
    }

    fun cleanAfterEach() {
        dbClient.sql { "TRUNCATE mail CASCADE" }.then().block()
        reset(emailSenderImpl)
    }

    abstract fun beforeEach()

    companion object {
        private var postgresql = PostgreSQLContainer<Nothing>("postgres:16.3").apply {
            withReuse(true)
            start()
        }
        private var redis = RedisContainer(DockerImageName.parse("redis:6.2.6")).apply {
            withReuse(true)
            start()
        }
        private var kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1")).apply {
            withReuse(true)
            start()
        }

        init {
            System.setProperty("kafka.brokers", kafka.bootstrapServers)

            System.setProperty("contour.cache.host", redis.host)
            System.setProperty("contour.cache.port", redis.firstMappedPort.toString())

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
