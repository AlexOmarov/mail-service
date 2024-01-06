package ru.somarov.mail.infrastructure.config

import org.springframework.boot.web.codec.CodecCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.somarov.mail.infrastructure.hessian.impl.HessianReader
import ru.somarov.mail.infrastructure.hessian.impl.HessianWriter

@Configuration
private class CodecConfig {
    @Bean
    fun hessianCodec(): CodecCustomizer {
        return CodecCustomizer {
            it.customCodecs().register(HessianWriter())
            it.customCodecs().register(HessianReader())
        }
    }
}
