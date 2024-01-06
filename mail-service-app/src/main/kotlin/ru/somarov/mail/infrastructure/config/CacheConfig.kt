package ru.somarov.mail.infrastructure.config

import io.lettuce.core.ReadFrom.REPLICA_PREFERRED
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext

@Configuration
@EnableCaching
private class CacheConfig(private val props: ServiceProps) {

    @Bean
    fun cacheConfiguration(): RedisCacheConfiguration {
        return defaultCacheConfig()
            .entryTtl(props.contour.cache.defaultTtl)
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJackson2JsonRedisSerializer()
                )
            )
    }

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val clientConfig = LettuceClientConfiguration.builder()
            .readFrom(REPLICA_PREFERRED)
            .build()

        val serverConfig = RedisStandaloneConfiguration(props.contour.cache.host, props.contour.cache.port)

        return LettuceConnectionFactory(serverConfig, clientConfig)
    }

    @Bean
    fun redisCacheManagerBuilderCustomizer(): RedisCacheManagerBuilderCustomizer {
        return RedisCacheManagerBuilderCustomizer { builder ->
            builder
                .withCacheConfiguration("mails", defaultCacheConfig().entryTtl(props.contour.cache.defaultTtl))
        }
    }
}
