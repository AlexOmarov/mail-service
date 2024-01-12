package ru.somarov.mail.infrastructure.config.rsocket

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor

@Configuration
@EnableRSocketSecurity
private class RsocketSecurityConfig {

    @Bean
    fun rsocketInterceptor(rsocket: RSocketSecurity): PayloadSocketAcceptorInterceptor {
        rsocket
            .authorizePayload { authorize ->
                authorize
                    .anyRequest().authenticated()
                    .anyExchange().permitAll()
            }
            .simpleAuthentication(withDefaults())
        return rsocket.build()
    }
}
