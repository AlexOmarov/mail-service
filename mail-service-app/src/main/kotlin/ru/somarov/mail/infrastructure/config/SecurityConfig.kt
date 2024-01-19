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
private class SecurityConfig(private val props: ServiceProps) {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .authorizeExchange {
                it
                    .pathMatchers(*props.contour.auth.exclusions.toTypedArray()).permitAll()
                    .anyExchange().authenticated()
            }
            .csrf { it.disable() }
            .httpBasic(withDefaults())
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(ENCODER_STRENGTH, SecureRandom())
    }

    @Bean
    @Suppress("SpreadOperator") // Due to User builder spec, is executed once per launch
    fun userDetailsService(encoder: PasswordEncoder): ReactiveUserDetailsService {
        val userDetails = User.builder()
            .passwordEncoder { encoder.encode(it) }
            .username(props.contour.auth.user)
            .password(props.contour.auth.password)
            .roles(*(props.contour.auth.roles.toTypedArray()))
            .build()
        return MapReactiveUserDetailsService(userDetails)
    }

    companion object {
        const val ENCODER_STRENGTH = 11
    }
}
