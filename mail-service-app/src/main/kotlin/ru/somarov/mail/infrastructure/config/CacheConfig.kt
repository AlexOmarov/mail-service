package ru.somarov.mail.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializationContext.RedisSerializationContextBuilder
import ru.somarov.mail.infrastructure.db.entity.Mail
import java.time.Duration

@Configuration
@EnableCaching
private class CacheConfig(private val props: ServiceProps) {

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(2))
            .shutdownTimeout(Duration.ZERO)
            .build()

        val serverConfig = RedisStandaloneConfiguration(props.contour.cache.host, props.contour.cache.port)
        serverConfig.password = RedisPassword.of(props.contour.cache.password)
        return LettuceConnectionFactory(serverConfig, clientConfig)
    }

    @Bean
    fun reactiveRedisTemplate(objectMapper: ObjectMapper): ReactiveRedisTemplate<String, Mail> {
        val serializer: Jackson2JsonRedisSerializer<Mail> = Jackson2JsonRedisSerializer(objectMapper, Mail::class.java)
        val builder: RedisSerializationContextBuilder<String, Mail> =
            RedisSerializationContext.newSerializationContext(
                Jackson2JsonRedisSerializer(objectMapper, String::class.java)
            )
        val context: RedisSerializationContext<String, Mail> = builder.value(serializer).build()
        val template = ReactiveRedisTemplate(redisConnectionFactory(), context)
        template.expire("mails", props.contour.cache.defaultTtl)
        return template
    }
}
