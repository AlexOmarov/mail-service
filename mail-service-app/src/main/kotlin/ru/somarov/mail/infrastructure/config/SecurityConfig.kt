package ru.somarov.mail.infrastructure.config

import io.micrometer.observation.ObservationRegistry
import net.devh.boot.grpc.server.security.authentication.BasicGrpcAuthenticationReader
import net.devh.boot.grpc.server.security.authentication.CompositeGrpcAuthenticationReader
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ObservationAuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.server.SecurityWebFilterChain
import java.security.SecureRandom

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
private class SecurityConfig(private val props: ServiceProps) {

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

    @Bean
    fun authenticationReader(): GrpcAuthenticationReader {
        val readers: MutableList<GrpcAuthenticationReader> = ArrayList()
        readers.add(BasicGrpcAuthenticationReader())
        return CompositeGrpcAuthenticationReader(readers)
    }

    // Here ObservationAuthenticationManager is used from blocking io,
    // because grpc auth processing is executed using blocking operations on grpc thread pool
    // and coming to coroutines non-blocking operations on main thread pool
    // only after this extraction will have been done
    @Bean
    @Suppress("SpreadOperator") // Due to User builder spec, is executed once per launch
    fun authenticationManager(
        registry: ObservationRegistry,
        passwordEncoder: PasswordEncoder,
        authenticationReader: GrpcAuthenticationReader
    ): ObservationAuthenticationManager {
        return ObservationAuthenticationManager(
            registry,
            ProviderManager(DaoAuthenticationProvider(passwordEncoder).also {
                it.setUserDetailsService(
                    InMemoryUserDetailsManager(listOf(User.builder()
                        .passwordEncoder { pass -> passwordEncoder.encode(pass) }
                        .username(props.contour.auth.user)
                        .password(props.contour.auth.password)
                        .roles(*(props.contour.auth.roles.toTypedArray()))
                        .build()))
                )
            })
        )
    }

    companion object {
        const val ENCODER_STRENGTH = 11
    }
}
