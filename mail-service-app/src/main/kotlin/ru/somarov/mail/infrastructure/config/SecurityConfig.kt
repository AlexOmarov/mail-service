package ru.somarov.mail.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import java.security.SecureRandom


@Configuration
@EnableWebFluxSecurity
private class SecurityConfig {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .authorizeExchange { it.anyExchange().authenticated() }
            .httpBasic(withDefaults())
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(ENCODER_STRENGTH, SecureRandom())
    }

    @Bean
    fun userDetailsService(encoder: PasswordEncoder): ReactiveUserDetailsService {
        val userDetails = User.builder()
            .passwordEncoder { encoder.encode(it) }
            .username("user")
            .password("user")
            .roles("USER")
            .build()
        return MapReactiveUserDetailsService(userDetails)
    }

    companion object {
        const val ENCODER_STRENGTH = 11
    }
}
