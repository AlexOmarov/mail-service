package ru.somarov.mail.infrastructure.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
private class ObjectMapperConfig {
    @Bean
    fun objectMapper(): ObjectMapper {
        val mapper = JsonMapper.builder().build()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)

        return mapper
    }
}
